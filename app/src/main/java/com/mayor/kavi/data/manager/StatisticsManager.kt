package com.mayor.kavi.data.manager

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mayor.kavi.data.models.*
import com.mayor.kavi.data.repository.*
import com.mayor.kavi.di.AppModule.IoDispatcher
import com.mayor.kavi.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import javax.inject.Inject
import timber.log.Timber
import javax.inject.Singleton
import kotlin.math.*
import kotlinx.serialization.encodeToString
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

@Singleton
class StatisticsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val statisticsRepository: StatisticsRepository,
    private val userRepository: UserRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    private val applicationScope = CoroutineScope(dispatcher + SupervisorJob())

    companion object {
        private fun gameStatisticsKey(userId: String) = stringPreferencesKey("GAME_STATS_$userId")

        val LocalStatisticsManager = staticCompositionLocalOf<StatisticsManager> {
            error("No StatisticsManager provided")
        }
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "statistics")
    private val dataStore = context.dataStore
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        allowSpecialFloatingPointValues = true
    }

    private var interpreter: Interpreter? = null

    private val _playerAnalysis = MutableStateFlow<PlayerAnalysis?>(null)
    val playerAnalysis: StateFlow<PlayerAnalysis?> = _playerAnalysis.asStateFlow()

    private val _modelTrainingStatus = MutableStateFlow<String?>(null)
    val modelTrainingStatus: StateFlow<String?> = _modelTrainingStatus.asStateFlow()

    private val _gameStatistics = MutableStateFlow<GameStatistics?>(null)
    val gameStatistics: StateFlow<GameStatistics?> = _gameStatistics.asStateFlow()

    private val _gameStartTime = MutableStateFlow<Long>(0)
    private val _turnStartTime = MutableStateFlow<Long>(0)
    private val _decisionTimes = MutableStateFlow<MutableList<Long>>(mutableListOf())
    private val _rollsThisTurn = MutableStateFlow(0)
    private val _bankingScores = MutableStateFlow<MutableList<Int>>(mutableListOf())

    private val statisticsMutex = Mutex()

    init {
        applicationScope.launch(dispatcher) {
            initializeTFLiteModel(context)
            loadGameStatistics()
            updatePlayerAnalysis()
        }
    }

    private fun initializeTFLiteModel(context: Context) {
        try {
            interpreter?.close()

            context.assets.open("dice_stats_model.tflite").use { inputStream ->
                val modelBytes = inputStream.readBytes()
                val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size)
                    .order(ByteOrder.nativeOrder())
                modelBuffer.put(modelBytes)
                modelBuffer.rewind()
                interpreter = Interpreter(modelBuffer)
                Timber.d("TFLite model loaded successfully")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading TFLite model: ${e.message}")
            // Make sure we don't keep a broken interpreter
            interpreter = null
        }
    }

    private fun prepareModelInput(stats: GameStatistics): FloatBuffer {
        val inputBuffer = FloatBuffer.allocate(4)

        // Normalize inputs to match training data ranges
        inputBuffer.put(minOf(stats.gamesPlayed.toFloat(), 100f) / 100f)  // games_played
        inputBuffer.put(calculateWinRate(stats))                           // win_rate
        inputBuffer.put(calculateAverageScore(stats) / 1000f)             // avg_score
        inputBuffer.put(calculateConsistency(stats))                       // consistency

        inputBuffer.rewind()
        return inputBuffer
    }

    private fun calculateAverageScore(stats: GameStatistics): Float {
        val scores =
            stats.playerAnalysis?.performanceMetrics?.averageScoreByMode?.values ?: return 0f
        return if (scores.isNotEmpty()) scores.average().toFloat() else 0f
    }

    private fun determinePlayStyleFromModel(playStyleScore: Float): PlayStyle {
        return when {
            playStyleScore > 0.66f -> PlayStyle.AGGRESSIVE
            playStyleScore < 0.33f -> PlayStyle.CAUTIOUS
            else -> PlayStyle.BALANCED
        }
    }

    private fun runModelInference(stats: GameStatistics): PlayerAnalysisPrediction {
        if (interpreter == null) return PlayerAnalysisPrediction()

        val inputBuffer = prepareModelInput(stats)
        val outputBuffer = FloatBuffer.allocate(4)

        try {
            interpreter?.run(inputBuffer, outputBuffer)
            outputBuffer.rewind()

            return PlayerAnalysisPrediction(
                predictedWinRate = outputBuffer.get(),
                predictedConsistency = outputBuffer.get(),
                improvementPotential = outputBuffer.get(),
                predictedPlayStyle = determinePlayStyleFromModel(outputBuffer.get())
            )
        } catch (e: Exception) {
            Timber.e(e, "Error running model inference: ${e.message}")
            return PlayerAnalysisPrediction()
        }
    }

    data class PlayerAnalysisPrediction(
        val predictedWinRate: Float = 0.5f,
        val predictedConsistency: Float = 0.5f,
        val improvementPotential: Float = 0.5f,
        val predictedPlayStyle: PlayStyle = PlayStyle.BALANCED
    )

    fun updateGameStatistics(
        gameMode: String,
        score: Int,
        isWin: Boolean,
        wasComeback: Boolean = false,
        wasCloseGame: Boolean = false
    ) {
        applicationScope.launch(dispatcher) {
            statisticsMutex.withLock {
        val currentStats = _gameStatistics.value ?: GameStatistics()
        val timeMetrics = updateTimeMetrics(currentStats)
        val decisionPatterns = updateDecisionPatterns()
        val performanceMetrics = updatePerformanceMetrics(
                    currentStats, isWin, gameMode, score, wasComeback, wasCloseGame
                )
                val achievements =
                    calculateAchievements(timeMetrics, performanceMetrics, decisionPatterns)
        val updatedStats = currentStats.copy(
            gamesPlayed = currentStats.gamesPlayed + 1,
            highScores = currentStats.highScores.toMutableMap().apply {
                val currentHigh = this[gameMode] ?: 0
                if (score > currentHigh) {
                    this[gameMode] = score
                }
            },
            winRates = currentStats.winRates.toMutableMap().apply {
                val currentWinRate = this[gameMode] ?: WinRate()
                this[gameMode] = WinRate(
                    wins = currentWinRate.wins + if (isWin) 1 else 0,
                    total = currentWinRate.total + 1
                )
            },
                    playerAnalysis = currentStats.playerAnalysis?.copy(
                        performanceMetrics = performanceMetrics,
                        timeMetrics = timeMetrics,
                        decisionPatterns = decisionPatterns,
                        achievementProgress = achievements
                    ) ?: PlayerAnalysis(),
                    lastSeen = System.currentTimeMillis()
        )

        // Update local state
        _gameStatistics.value = updatedStats

        // Save to both remote and local storage
            saveGameStatisticsToDataStore(updatedStats)
            when (val result = statisticsRepository.updateGameStatistics(updatedStats)) {
                is Result.Success -> {
                    Timber.d("Successfully updated game statistics")
                    updatePlayerAnalysis()
                        // Check for retraining every game
                        checkAndRetrainModel()
                }

                is Result.Error -> {
                    Timber.e(result.exception, "Failed to update game statistics")
                }

                else -> {}
                }
            }
        }
    }

    private fun mergeStatistics(
        remoteStats: GameStatistics,
        localStats: GameStatistics?
    ): GameStatistics {
        if (localStats == null) return remoteStats

        return GameStatistics(
                            gamesPlayed = maxOf(remoteStats.gamesPlayed, localStats.gamesPlayed),
                            highScores = (remoteStats.highScores.keys + localStats.highScores.keys).associateWith { gameMode ->
                                maxOf(
                                    remoteStats.highScores[gameMode] ?: 0,
                                    localStats.highScores[gameMode] ?: 0
                                )
                            },
                            winRates = (remoteStats.winRates.keys + localStats.winRates.keys).associateWith { gameMode ->
                                val remoteWinRate = remoteStats.winRates[gameMode] ?: WinRate()
                                val localWinRate = localStats.winRates[gameMode] ?: WinRate()
                if ((remoteStats.lastSeen ?: 0) > (localStats.lastSeen
                        ?: 0)
                ) remoteWinRate else localWinRate
                            },
                            lastSeen = maxOf(remoteStats.lastSeen ?: 0, localStats.lastSeen ?: 0),
                            playerAnalysis = mergePlayerAnalysis(
                                remoteStats.playerAnalysis,
                                localStats.playerAnalysis
                            )
                        )
    }

    private suspend fun loadGameStatisticsFromDataStoreSafely() {
        try {
            loadGameStatisticsFromDataStore()
        } catch (e: Exception) {
            Timber.w(e, "Failed to load statistics from DataStore")
            // Optionally, set a default value or skip
        }
    }

    private suspend fun loadGameStatistics() {
        try {
            loadGameStatisticsFromDataStoreSafely()
            when (val result = statisticsRepository.getGameStatistics()) {
                is Result.Success -> {
                    val remoteStats = result.data
                    val localStats = _gameStatistics.value
                    val mergedStats = mergeStatistics(remoteStats, localStats)

                    _gameStatistics.value = mergedStats
                    saveGameStatisticsToDataStore(mergedStats)

                    if ((mergedStats.lastSeen ?: 0) > (remoteStats.lastSeen ?: 0)) {
                    statisticsRepository.updateGameStatistics(mergedStats)
                    }
                }

                is Result.Error -> {
                    Timber.e(result.exception, "Error loading remote game statistics")
                }

                else -> {}
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in loadGameStatistics")
        }
    }

    private fun mergePlayerAnalysis(
        remote: PlayerAnalysis?,
        local: PlayerAnalysis?
    ): PlayerAnalysis {
        if (remote == null) return local ?: PlayerAnalysis()
        if (local == null) return remote

        // Take the latest values based on consistency
        return if (remote.consistency > local.consistency) {
            remote
        } else {
            local
        }
    }

    private suspend fun updatePlayerAnalysis() {
        val stats = _gameStatistics.value ?: return
        val currentUser = userRepository.getCurrentUserId() ?: return

        // Get model predictions
        val predictions = runModelInference(stats)

        // Calculate player analysis based on actual gameplay patterns
        val currentAnalysis = stats.playerAnalysis ?: PlayerAnalysis()
        val analysis = PlayerAnalysis(
            predictedWinRate = predictions.predictedWinRate,
            consistency = calculateConsistency(stats),
            playStyle = determinePlayStyle(stats),
            improvement = calculateImprovement(stats),
            decisionPatterns = currentAnalysis.decisionPatterns,  // Keep tracked patterns
            timeMetrics = currentAnalysis.timeMetrics,  // Keep tracked metrics
            performanceMetrics = currentAnalysis.performanceMetrics,  // Keep tracked performance
            achievementProgress = currentAnalysis.achievementProgress  // Keep tracked achievements
        )

        _playerAnalysis.value = analysis
        try {
            statisticsRepository.updatePlayerAnalysis(currentUser, analysis)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update player analysis")
        }
    }

    private suspend fun exportTrainingData(): String {
        return when (val result = statisticsRepository.getAllUsersGameStatistics()) {
            is Result.Success -> {
                val csvFile = File(context.filesDir, "training_data.csv")
                csvFile.bufferedWriter().use { writer ->
                    // Write CSV header
                    writer.write(
                        "games_played,win_rate,avg_score,consistency," +
                                "pred_win_rate,consistency,improvement,play_style\n"
                    )

                    // Write data for each user's statistics
                    result.data.forEach { stats ->
                        // Calculate features
                        val avgScore = stats.highScores.values.average()
                        val overallWinRate = stats.winRates.values.let { rates ->
                            val totalWins = rates.sumOf { it.wins }
                            val totalGames = rates.sumOf { it.total }
                            if (totalGames > 0) totalWins.toDouble() / totalGames else 0.0
                        }

                        // Only include entries with player analysis
                        stats.playerAnalysis?.let { analysis ->
                            writer.write(
                                "${stats.gamesPlayed},$overallWinRate," +
                                        "$avgScore,${analysis.consistency}," +
                                        "${analysis.predictedWinRate},${analysis.consistency}," +
                                        "${analysis.improvement},${analysis.playStyle.ordinal}\n"
                            )
                        }
                    }
                }
                csvFile.absolutePath
            }

            is Result.Error -> throw result.exception!!

            else -> throw Exception("Unknown result type")
        }
    }

    // This function can be called periodically or after significant data changes
    private suspend fun checkAndRetrainModel() {
        _modelTrainingStatus.value = "Checking if model retraining is needed..."
        Timber.d("Checking if model retraining is needed")

            try {
            when (val result = statisticsRepository.getAllUsersGameStatistics()) {
                is Result.Success -> {
                    val stats = result.data
                    if (stats.size >= 5) { // Lower threshold for testing
                        _modelTrainingStatus.value = "Preparing training data..."
                val trainingDataPath = exportTrainingData()

                        _modelTrainingStatus.value = "Training model with ${stats.size} samples..."
                        Timber.d("Training model with ${stats.size} samples at path: $trainingDataPath")

                        // Re-initialize the model after training
                        initializeTFLiteModel(context)

                        _modelTrainingStatus.value = "Model training completed"
                        Timber.d("Model training completed")
                    } else {
                        _modelTrainingStatus.value =
                            "Not enough data for retraining (${stats.size} samples)"
                        Timber.d("Not enough data for retraining (${stats.size} samples)")
                    }
                }

                is Result.Error -> {
                    _modelTrainingStatus.value =
                        "Error checking statistics: ${result.exception?.message}"
                    Timber.e(result.exception, "Error checking statistics")
                }

                else -> {
                    _modelTrainingStatus.value = "Unknown result when checking statistics"
                }
            }
        } catch (e: Exception) {
            _modelTrainingStatus.value = "Error during model training: ${e.message}"
            Timber.e(e, "Error during model training")
        }

        // Clear status after a delay
        delay(3000)
        _modelTrainingStatus.value = null
    }

    private suspend fun saveGameStatisticsToDataStore(stats: GameStatistics) {
        try {
            val userId = userRepository.getCurrentUserId() ?: return
            val jsonString = json.encodeToString(stats)
            dataStore.edit { prefs ->
                prefs[gameStatisticsKey(userId)] = jsonString
            }
            Timber.d("Successfully saved game statistics to DataStore")
        } catch (e: Exception) {
            Timber.e(e, "Error saving game statistics to DataStore")
        }
    }

    private suspend fun loadGameStatisticsFromDataStore() {
        try {
            val userId = userRepository.getCurrentUserId() ?: return
            dataStore.data.firstOrNull()?.let { prefs ->
                val jsonString = prefs[gameStatisticsKey(userId)]
                if (jsonString != null) {
                    val stats = json.decodeFromString<GameStatistics>(jsonString)
                    _gameStatistics.value = stats
                    Timber.d("Successfully loaded game statistics from DataStore")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading game statistics from DataStore")
            // If local load fails, initialize with empty statistics
            _gameStatistics.value = GameStatistics()
        }
    }

    // Helper functions for analysis calculations
    private fun calculateWinRate(stats: GameStatistics): Float {
        val totalWins = stats.winRates.values.sumOf { it.wins }
        val totalGames = stats.winRates.values.sumOf { it.total }
        return if (totalGames > 0) totalWins.toFloat() / totalGames else 0f
    }

    private fun calculateConsistency(stats: GameStatistics): Float {
        val winRates = stats.winRates.values.map {
            if (it.total > 0) it.wins.toFloat() / it.total else 0f
        }
        return if (winRates.isNotEmpty()) {
            val mean = winRates.average()
            val variance = winRates.map { (it - mean).pow(2) }.average()
            sqrt(variance).toFloat()
        } else 0.5f
    }

    private fun determinePlayStyle(stats: GameStatistics): PlayStyle {
        val riskLevel = calculateRiskLevel(stats)
        // Thresholds fine-tuned based on observation and experience
        return when {
            riskLevel > 0.6f -> PlayStyle.AGGRESSIVE
            riskLevel < 0.4f -> PlayStyle.CAUTIOUS
            else -> PlayStyle.BALANCED
        }
    }

    private fun calculateRiskLevel(stats: GameStatistics): Float {
        // Calculate risk based on win rate and consistency
        val winRate = calculateWinRate(stats)
        val consistency = calculateConsistency(stats)
        return (winRate + (1 - consistency)) / 2
    }

    private fun calculateImprovement(stats: GameStatistics): Float {
        val totalGames = stats.winRates.values.sumOf { it.total }
        if (totalGames < 5) return 0f // Cannot determine improvement with less than 5 games
        val gameModes = stats.winRates.keys

        // Calculate win rate for each game mode
        gameModes.associateWith { gameMode ->
            val winRate = stats.winRates[gameMode] ?: WinRate()
            if (winRate.total > 0) winRate.wins.toFloat() / winRate.total else 0f
        }
        val recentGames = stats.winRates.values.sumOf { it.total }
        val recentWins = stats.winRates.values.sumOf { it.wins }
        val recentWinRate = if (recentGames > 0) {
            recentWins.toFloat() / recentGames
        } else {
            0f
        }
        // Compare recent performance against long-term performance
        var overallWinRate = calculateWinRate(stats)

        // Give some benefit to those who have had a loss before improving
        if (recentWinRate < overallWinRate) {
            overallWinRate -= 0.1f
        }
        return (recentWinRate - overallWinRate).coerceIn(0f, 1f)
    }
//
//    fun shouldAIBank(currentTurnScore: Int, aiTotalScore: Int, playerTotalScore: Int): Boolean {
//        // If AI can win by banking, always bank
//        if (aiTotalScore + currentTurnScore >= 100) return true
//        val playerAnalysis = _playerAnalysis.value
//        val playerWinRate = playerAnalysis?.predictedWinRate ?: 0.5f
//        val playerConsistency = playerAnalysis?.consistency ?: 0.5f
//        val playerStyle = playerAnalysis?.playStyle ?: PlayStyle.BALANCED
//
//        // Base minimum score varies based on player's style and stats
//        val baseMinScore = when (playerStyle) {
//            PlayStyle.AGGRESSIVE -> if (playerWinRate > 0.6f) 25 else 22
//            PlayStyle.CAUTIOUS -> if (playerWinRate < 0.4f) 15 else 18
//            else -> when {
//                playerWinRate > 0.6f -> 22
//                playerWinRate < 0.4f -> 18
//                else -> 20
//            }
//        }
//
//        // Adjust based on game situation
//        val situationalAdjustment = when {
//            playerTotalScore >= 75 -> if (playerConsistency > 0.7f) -8 else -5
//            aiTotalScore >= 75 -> when (playerStyle) {
//                PlayStyle.AGGRESSIVE -> +7
//                PlayStyle.CAUTIOUS -> +3
//                else -> +5
//            }
//
//            playerTotalScore > aiTotalScore + 20 -> if (playerWinRate > 0.5f) -10 else -7
//            else -> 0
//        }
//
//        val finalMinScore = (baseMinScore + situationalAdjustment).coerceIn(12, 28)
//        val randomRange = if (playerConsistency > 0.7f) (-1..1) else (-2..2)
//        return currentTurnScore >= finalMinScore + randomRange.random()
//    }

    fun startGameTimer() {
        _gameStartTime.value = System.currentTimeMillis()
    }

    fun startTurnTimer() {
        _turnStartTime.value = System.currentTimeMillis()
        _rollsThisTurn.value = 0
    }

    fun recordDecision(timeSpent: Long) {
        _decisionTimes.value.add(timeSpent)
    }

    fun recordRoll() {
        _rollsThisTurn.value += 1
    }

    fun recordBanking(score: Int) {
        _bankingScores.value.add(score)
    }

    private fun updateTimeMetrics(currentStats: GameStatistics): TimeMetrics {
        val gameDuration = (System.currentTimeMillis() - _gameStartTime.value)
        val currentMetrics = currentStats.playerAnalysis?.timeMetrics ?: TimeMetrics()

        // Calculate average game duration
        val avgDuration = if (currentMetrics.averageGameDuration == 0L) {
            gameDuration
        } else {
            (currentMetrics.averageGameDuration + gameDuration) / 2L
        }

        // Calculate average turn duration
        val avgTurnDuration = if (_decisionTimes.value.isEmpty()) {
            0L
        } else {
            _decisionTimes.value.average().toLong()
        }

        // Update total play time
        val newTotalPlayTime = if (currentMetrics.totalPlayTime == 0L) {
            gameDuration
        } else {
            currentMetrics.totalPlayTime + gameDuration
        }

        return TimeMetrics(
            averageGameDuration = avgDuration,
            averageTurnDuration = avgTurnDuration,
            fastestGame = if (currentMetrics.fastestGame == 0L || gameDuration < currentMetrics.fastestGame) {
                gameDuration
            } else {
                currentMetrics.fastestGame
            },
            totalPlayTime = newTotalPlayTime
        )
    }

    private fun updateDecisionPatterns(): DecisionPatterns {
        return DecisionPatterns(
            averageRollsPerTurn = _rollsThisTurn.value.toFloat(),
            bankingThreshold = _bankingScores.value.average().toFloat(),
            riskTaking = calculateRiskTaking(),
            decisionSpeed = _decisionTimes.value.average().toFloat()
        )
    }

    private fun calculateRiskTaking(): Float {
        val avgBankingScore = _bankingScores.value.average()
        val maxPossibleScore = when {
            avgBankingScore > 800 -> 1000f // Greed game
            avgBankingScore > 50 -> 100f    // Pig game
            else -> 50f                     // Default
        }
        return (avgBankingScore / maxPossibleScore).toFloat()
    }

    private fun updatePerformanceMetrics(
        currentStats: GameStatistics,
        isWin: Boolean,
        gameMode: String,
        finalScore: Int,
        wasComeback: Boolean,
        wasCloseGame: Boolean
    ): PerformanceMetrics {
        val currentMetrics = currentStats.playerAnalysis?.performanceMetrics ?: PerformanceMetrics()

        val newStreak = if (isWin) currentMetrics.currentStreak + 1 else 0
        val personalBests = currentMetrics.personalBests.toMutableMap()
        if (finalScore > (personalBests[gameMode] ?: 0)) {
            personalBests[gameMode] = finalScore
        }

        val avgScores = currentMetrics.averageScoreByMode.toMutableMap()
        val currentAvg = avgScores[gameMode] ?: 0f
        avgScores[gameMode] = if (currentAvg == 0f)
            finalScore.toFloat()
        else
            (currentAvg + finalScore) / 2

        return PerformanceMetrics(
            currentStreak = newStreak,
            longestStreak = maxOf(newStreak, currentMetrics.longestStreak),
            comebacks = currentMetrics.comebacks + if (wasComeback) 1 else 0,
            closeGames = currentMetrics.closeGames + if (wasCloseGame) 1 else 0,
            personalBests = personalBests,
            averageScoreByMode = avgScores
        )
    }

    private fun calculateAchievements(
        timeMetrics: TimeMetrics,
        performanceMetrics: PerformanceMetrics,
        decisionPatterns: DecisionPatterns
    ): Map<String, Float> {
        val stats = gameStatistics.value ?: GameStatistics()

        // Convert total play time from milliseconds to hours for achievement calculation
        val playTimeHours = timeMetrics.totalPlayTime / (1000 * 60 * 60).toFloat()

        return mapOf(
            Achievement.STREAK_MASTER.name to (performanceMetrics.longestStreak.toFloat() / 10f).coerceAtMost(
                1f
            ),
            Achievement.COMEBACK_KING.name to (performanceMetrics.comebacks.toFloat() / 5f).coerceAtMost(
                1f
            ),
            Achievement.CONSISTENT_PLAYER.name to (1f - calculateConsistency(stats)).coerceAtMost(1f),
            Achievement.RISK_TAKER.name to decisionPatterns.riskTaking,
            Achievement.SPEED_STAR.name to (1000f / (decisionPatterns.decisionSpeed + 1f)).coerceAtMost(
                1f
            ),
            Achievement.VETERAN_PLAYER.name to (playTimeHours / 10f).coerceAtMost(1f) // 10 hours for max achievement
        )
    }

    suspend fun clearUserStatistics() {
        // Reset all local state
        _gameStatistics.value = GameStatistics()
        _playerAnalysis.value = null
        _gameStartTime.value = 0
        _turnStartTime.value = 0
        _rollsThisTurn.value = 0
        _decisionTimes.value.clear()
        _bankingScores.value.clear()
        // Clear local storage
        saveGameStatisticsToDataStore(GameStatistics())
        // Clear remote storage
        try {
            when (val result = statisticsRepository.clearUserStatistics()) {
                is Result.Success -> {
                    Timber.d("Successfully cleared user statistics")
                }

                is Result.Error -> {
                    Timber.e(result.exception, "Failed to clear remote statistics")
                }

                else -> {}
            }
        } catch (e: Exception) {
            Timber.e(e, "Error clearing user statistics")
        }
    }

    suspend fun clearAllData() {
        // Reset all local state
        _gameStatistics.value = GameStatistics()
        _playerAnalysis.value = null
        _gameStartTime.value = 0
        _turnStartTime.value = 0
        _rollsThisTurn.value = 0
        _decisionTimes.value.clear()
        _bankingScores.value.clear()

        // Clear local storage
        try {
            val userId = userRepository.getCurrentUserId()
            if (userId != null) {
                dataStore.edit { prefs ->
                    prefs.remove(gameStatisticsKey(userId))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error clearing local statistics data")
        }
    }
}

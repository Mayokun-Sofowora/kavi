package com.mayor.kavi.data.manager

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mayor.kavi.data.models.*
import com.mayor.kavi.data.repository.*
import com.mayor.kavi.util.IoDispatcher
import com.mayor.kavi.util.Result
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import javax.inject.Inject
import timber.log.Timber
import javax.inject.Singleton
import kotlin.math.*
import kotlinx.serialization.encodeToString

@Singleton
class StatisticsManager @Inject constructor(
    context: Context,
    private val statisticsRepository: StatisticsRepository,
    private val userRepository: UserRepository,
    private val applicationScope: CoroutineScope,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    companion object {
        private fun gameStatisticsKey(userId: String) = stringPreferencesKey("GAME_STATS_$userId")

        val LocalStatisticsManager = staticCompositionLocalOf<StatisticsManager> {
            error("No StatisticsManager provided")
        }
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "statistics")
    private val dataStore = context.dataStore
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    
    private val _playerAnalysis = MutableStateFlow<PlayerAnalysis?>(null)
    val playerAnalysis: StateFlow<PlayerAnalysis?> = _playerAnalysis.asStateFlow()
    
    private val _gameStatistics = MutableStateFlow<GameStatistics?>(null)
    val gameStatistics: StateFlow<GameStatistics?> = _gameStatistics.asStateFlow()

    init {
        applicationScope.launch(dispatcher) {
            loadGameStatistics()
            updatePlayerAnalysis()
        }
    }

    fun updateGameStatistics(gameMode: String, score: Int, isWin: Boolean) {
        val currentStats = _gameStatistics.value ?: GameStatistics()
        val updatedStats = currentStats.copy(
            highScores = currentStats.highScores.toMutableMap().apply {
                val currentHigh = this[gameMode] ?: 0
                if (score > currentHigh) {
                    this[gameMode] = score
                }
            },
            gamesPlayed = currentStats.gamesPlayed + 1,
            winRates = currentStats.winRates.toMutableMap().apply {
                val currentWinRate = this[gameMode] ?: WinRate()
                this[gameMode] = WinRate(
                    wins = currentWinRate.wins + if (isWin) 1 else 0,
                    total = currentWinRate.total + 1
                )
            }
        )

        // Update local state
        _gameStatistics.value = updatedStats

        // Save to both remote and local storage
        applicationScope.launch(dispatcher) {
            saveGameStatisticsToDataStore(updatedStats)
            when (val result = statisticsRepository.updateGameStatistics(updatedStats)) {
                is Result.Success -> {
                    Timber.d("Successfully updated game statistics")
                    updatePlayerAnalysis() // Update analysis after statistics change
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Failed to update game statistics")
                }
                else -> {}
            }
        }
    }

    private suspend fun loadGameStatistics() {
        try {
            // First try to load from local DataStore
            loadGameStatisticsFromDataStore()
            
            // Then fetch from Firestore
            when (val result = statisticsRepository.getGameStatistics()) {
                is Result.Success -> {
                    val remoteStats = result.data
                    val localStats = _gameStatistics.value
                    
                    // Merge remote and local stats by taking the maximum values
                    val mergedStats = if (localStats != null) {
                        GameStatistics(
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
                                // Take the maximum of wins and totals instead of adding them
                                WinRate(
                                    wins = maxOf(remoteWinRate.wins, localWinRate.wins),
                                    total = maxOf(remoteWinRate.total, localWinRate.total)
                                )
                            },
                            lastSeen = maxOf(remoteStats.lastSeen ?: 0, localStats.lastSeen ?: 0),
                            playerAnalysis = remoteStats.playerAnalysis ?: localStats.playerAnalysis
                        )
                    } else {
                        remoteStats
                    }
                    
                    // Update both local and remote with merged data
                    _gameStatistics.value = mergedStats
                    saveGameStatisticsToDataStore(mergedStats)
                    // Only update remote if local has higher values
                    if (localStats != null && shouldUpdateRemote(localStats, remoteStats)) {
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

    private fun shouldUpdateRemote(local: GameStatistics, remote: GameStatistics): Boolean {
        // Check if local has any higher values that need to be synced to remote
        if (local.gamesPlayed > remote.gamesPlayed) return true
        
        // Check high scores
        for (gameMode in local.highScores.keys) {
            if ((local.highScores[gameMode] ?: 0) > (remote.highScores[gameMode] ?: 0)) {
                return true
            }
        }
        
        // Check win rates
        for (gameMode in local.winRates.keys) {
            val localWinRate = local.winRates[gameMode] ?: WinRate()
            val remoteWinRate = remote.winRates[gameMode] ?: WinRate()
            if (localWinRate.wins > remoteWinRate.wins || localWinRate.total > remoteWinRate.total) {
                return true
            }
        }
        
        return false
    }

    private suspend fun updatePlayerAnalysis() {
        val stats = _gameStatistics.value ?: return
        val currentUser = userRepository.getCurrentUserId()

        val analysis = PlayerAnalysis(
            predictedWinRate = calculateWinRate(stats),
            consistency = calculateConsistency(stats),
            playStyle = determinePlayStyle(stats),
            improvement = calculateImprovement(stats)
        )

        _playerAnalysis.value = analysis
        statisticsRepository.updatePlayerAnalysis(currentUser!!, analysis)
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
        return when {
            riskLevel > 0.7f -> PlayStyle.AGGRESSIVE
            riskLevel < 0.3f -> PlayStyle.CAUTIOUS
            else -> PlayStyle.BALANCED
        }
    }

    private fun calculateRiskLevel(stats: GameStatistics): Float {
        val winRate = calculateWinRate(stats)
        val consistency = calculateConsistency(stats)
        return (winRate + (1 - consistency)) / 2
    }

    private fun calculateImprovement(stats: GameStatistics): Float {
        // Calculate improvement based on recent game performance
        val totalGames = stats.winRates.values.sumOf { it.total }
        val totalWins = stats.winRates.values.sumOf { it.wins }
        
        return if (totalGames >= 5) {
            val recentWinRate = totalWins.toFloat() / totalGames
            val overallWinRate = calculateWinRate(stats)
            (recentWinRate - overallWinRate).coerceIn(0f, 1f)
        } else 0f
    }

    // AI decision making for Pig game
    fun shouldAIBank(currentTurnScore: Int, aiTotalScore: Int, playerTotalScore: Int): Boolean {
        // If AI can win by banking, always bank
        if (aiTotalScore + currentTurnScore >= 100) return true

        val playerAnalysis = _playerAnalysis.value
        val playerWinRate = playerAnalysis?.predictedWinRate ?: 0.5f
        val playerConsistency = playerAnalysis?.consistency ?: 0.5f
        val playerStyle = playerAnalysis?.playStyle ?: PlayStyle.BALANCED

        // Base minimum score varies based on player's style and stats
        val baseMinScore = when (playerStyle) {
            PlayStyle.AGGRESSIVE -> if (playerWinRate > 0.6f) 25 else 22
            PlayStyle.CAUTIOUS -> if (playerWinRate < 0.4f) 15 else 18
            else -> when {
                playerWinRate > 0.6f -> 22
                playerWinRate < 0.4f -> 18
                else -> 20
            }
        }

        // Adjust based on game situation
        val situationalAdjustment = when {
            playerTotalScore >= 75 -> if (playerConsistency > 0.7f) -8 else -5
            aiTotalScore >= 75 -> when (playerStyle) {
                PlayStyle.AGGRESSIVE -> +7
                PlayStyle.CAUTIOUS -> +3
                else -> +5
            }
            playerTotalScore > aiTotalScore + 20 -> if (playerWinRate > 0.5f) -10 else -7
            else -> 0
        }

        val finalMinScore = (baseMinScore + situationalAdjustment).coerceIn(12, 28)
        val randomRange = if (playerConsistency > 0.7f) (-1..1) else (-2..2)
        
        return currentTurnScore >= finalMinScore + randomRange.random()
    }
}

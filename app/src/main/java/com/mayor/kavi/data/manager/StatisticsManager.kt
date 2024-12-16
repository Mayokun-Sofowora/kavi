package com.mayor.kavi.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mayor.kavi.data.GameRepository
import com.mayor.kavi.data.games.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import timber.log.Timber
import kotlin.math.*

data class GameStats(
    val gamesPlayed: Int,
    val highScores: Map<String, Int>,
    val winRates: Map<String, Pair<Int, Int>>,
    val shakeRates: List<Pair<Long, Int>>
)

data class PlayerAnalysis(
    val predictedWinRate: Float,
    val consistency: Float,
    val improvement: Float,
    val playStyle: PlayStyle = PlayStyle.BALANCED,
    val trends: List<TrendPoint> = emptyList()
)

data class TrendPoint(
    val turnNumber: Int,
    val score: Int,
    val decision: String
)

data class ScoreState(
    val currentTurnScore: Int = 0,
    val overallScore: Int = 0,
    val winRates: Map<String, Pair<Int, Int>> = emptyMap()
)

enum class PlayStyle {
    CAUTIOUS, BALANCED, AGGRESSIVE
}

/**
 *  Using TensorFlow Lite to analyze game and player statistics.
 */
class StatisticsManager @Inject constructor(
    private val context: Context,
    gameRepository: GameRepository
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "statistics")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dataStore = context.dataStore
    private var currentUserId: String? = null

    companion object {
        private const val MODEL_VERSION = "1.0.0"
        private val MODEL_VERSION_KEY = stringPreferencesKey("model_version")

        // Function to create keys based on user ID
        private fun gamesPlayedKey(userId: String) = intPreferencesKey("GAMES_PLAYED_$userId")
        private fun highScoresKey(userId: String) = stringPreferencesKey("HIGH_SCORES_$userId")
        private fun winRatesKey(userId: String) = stringPreferencesKey("WIN_RATES_$userId")
        private fun shakeRatesKey(userId: String) = stringPreferencesKey("shake_rates_$userId")

    }

    private var interpreter: Interpreter? = null

    // Score state flows
    private val _scoreState = MutableStateFlow(ScoreState())
    val scoreState: StateFlow<ScoreState> = _scoreState
    private val _balutScoreState = MutableStateFlow(BalutScoreState())
    val balutScoreState: StateFlow<BalutScoreState> = _balutScoreState
    private val _chicagoScoreState = MutableStateFlow(ChicagoScoreState())
    val chicagoScoreState: StateFlow<ChicagoScoreState> = _chicagoScoreState
    private val _mexicoScoreState = MutableStateFlow(MexicoScoreState())

    // Player analysis flow
    private val _playerAnalysis = MutableStateFlow<PlayerAnalysis?>(null)
    val playerAnalysis: StateFlow<PlayerAnalysis?> = _playerAnalysis
    private var _shakeCount = 0

    init {
        scope.launch {
            loadTensorFlowModel()
            checkModelVersion()
        }
    }

    private suspend fun analyzePlayerStats(gameStats: GameStats): PlayerAnalysis {
        return withContext(Dispatchers.Default) {
            val input = prepareInputData(gameStats)
            val outputArray =
                Array(1) { FloatArray(4) } // [winRate, consistency, improvement, preferredStyle]

            interpreter?.run(input, outputArray)

            PlayerAnalysis(
                predictedWinRate = outputArray[0][0],
                consistency = outputArray[0][1],
                improvement = outputArray[0][2],
                playStyle = determinePlayStyle(outputArray[0][3])
            )
        }
    }

    suspend fun clearUserStats(userId: String) {
        dataStore.edit { prefs ->
            prefs.remove(gamesPlayedKey(userId))
            prefs.remove(highScoresKey(userId))
            prefs.remove(winRatesKey(userId))
            prefs.remove(shakeRatesKey(userId))
        }
        _shakeCount = 0
    }

    private fun loadTensorFlowModel() {
        try {
            val modelFile = "dice_stats_model.tflite"
            val model = FileUtil.loadMappedFile(context, modelFile)
            val options = Interpreter.Options().apply {
                setNumThreads(2)
            }
            interpreter = Interpreter(model, options)
            if (!validateModel()) {
                Timber.w("The tensorflow model was invalid")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading TensorFlow model")
        }
    }

    private suspend fun checkModelVersion() {
        val currentVersion = dataStore.data.first()[MODEL_VERSION_KEY]
        if (currentVersion != MODEL_VERSION) {
            // Reload model if version mismatch
            loadTensorFlowModel()
            dataStore.edit { preferences ->
                preferences[MODEL_VERSION_KEY] = MODEL_VERSION
            }
        }
    }

    private fun validateModel(): Boolean {
        return try {
            val inputShape = interpreter?.getInputTensor(0)?.shape()
            val outputShape = interpreter?.getOutputTensor(0)?.shape()
            inputShape?.get(1) == 4 && outputShape?.get(1) == 4
        } catch (e: Exception) {
            Timber.tag("StatisticsManager").d(e, "Model validation failed")
            false
        }
    }

    private fun prepareInputData(gameStats: GameStats): Array<FloatArray> {
        return arrayOf(
            floatArrayOf(
                gameStats.gamesPlayed.toFloat(),
                calculateOverallWinRate(gameStats.winRates),
                calculateAverageScore(gameStats.highScores),
                calculateConsistency(gameStats.winRates)
            )
        )
    }

    private fun calculateOverallWinRate(winRates: Map<String, Pair<Int, Int>>): Float {
        val totalWins = winRates.values.sumOf { it.first }
        val totalGames = winRates.values.sumOf { it.second }
        return if (totalGames > 0) totalWins.toFloat() / totalGames else 0f
    }

    private fun calculateAverageScore(highScores: Map<String, Int>): Float {
        return if (highScores.isNotEmpty()) {
            highScores.values.average().toFloat()
        } else 0f
    }

    private fun calculateConsistency(winRates: Map<String, Pair<Int, Int>>): Float {
        // Calculate standard deviation of win rates
        val rates = winRates.values.map {
            if (it.second > 0) it.first.toFloat() / it.second else 0f
        }
        return if (rates.isNotEmpty()) {
            val mean = rates.average()
            val variance = rates.map { (it - mean).pow(2) }.average()
            sqrt(variance).toFloat()
        } else 0f
    }

    suspend fun updateGameStats(board: GameBoard, score: Int, isWin: Boolean) {
        currentUserId?.let { userId ->
            dataStore.edit { prefs ->
                if (isWin) {
                    // Only increment games played if the game is over
                    val currentGamesPlayed = prefs[gamesPlayedKey(userId)] ?: 0
                    prefs[gamesPlayedKey(userId)] = currentGamesPlayed + 1
                }
                // Update win rates
                val winRates = prefs[winRatesKey(userId)]?.let {
                    Json.decodeFromString<Map<String, Pair<Int, Int>>>(it)
                } ?: emptyMap()

                val updatedWinRates = winRates.toMutableMap().apply {
                    val current = this[board.modeName] ?: (0 to 0)
                    // Increment total games and wins if applicable
                    this[board.modeName] =
                        (current.first + if (isWin) 1 else 0) to (current.second + 1)
                }
                prefs[winRatesKey(userId)] = Json.encodeToString(updatedWinRates)

                // Update high scores
                val highScores = prefs[highScoresKey(userId)]?.let {
                    Json.decodeFromString<Map<String, Int>>(it)
                } ?: emptyMap()

                val updatedHighScores = highScores.toMutableMap().apply {
                    val currentHigh = this[board.modeName] ?: 0
                    if (score > currentHigh) {
                        this[board.modeName] = score
                    }
                }
                prefs[highScoresKey(userId)] = Json.encodeToString(updatedHighScores)
            }
        }
    }

    fun getGameStats(): Flow<GameStats> = dataStore.data.map { prefs ->
        val userId = currentUserId ?: ""
        GameStats(
            gamesPlayed = prefs[gamesPlayedKey(userId)] ?: 0,
            highScores = prefs[highScoresKey(userId)]?.let {
                Json.decodeFromString<Map<String, Int>>(it)
            } ?: emptyMap(),
            winRates = prefs[winRatesKey(userId)]?.let {
                Json.decodeFromString<Map<String, Pair<Int, Int>>>(it)
            } ?: emptyMap(),
            shakeRates = prefs[shakeRatesKey(userId)]?.let {
                Json.decodeFromString<List<Pair<Long, Int>>>(it)
            } ?: emptyList()
        )
    }

    suspend fun updateShakeRate() {
        _shakeCount++
        currentUserId?.let { userId ->
            dataStore.edit { prefs ->
                val currentRates = prefs[shakeRatesKey(userId)]?.let {
                    Json.decodeFromString<List<Pair<Long, Int>>>(it)
                } ?: emptyList()

                val newRate = Pair(System.currentTimeMillis(), _shakeCount)
                val updatedRates = (currentRates + newRate).takeLast(10)

                prefs[shakeRatesKey(userId)] = Json.encodeToString(updatedRates)
            }
        }
    }

    // New functions to update score states
    fun updateScoreState(newScore: Int) {
        _scoreState.value = _scoreState.value.copy(currentTurnScore = newScore)
    }

    fun updateBalutScoreState(newBalutState: BalutScoreState) {
        _balutScoreState.value = newBalutState
    }

    // -- Analysis logic --
    fun updateAnalysis() {
        val previousAnalysis = _playerAnalysis.value
        scope.launch(Dispatchers.Default) {
            try {
                val gameStats = getGameStats().first() // collect the latest values of GameStats
                val analysis = analyzePlayerStats(gameStats)

                _playerAnalysis.value = PlayerAnalysis(
                    predictedWinRate = analysis.predictedWinRate,
                    consistency = analysis.consistency,
                    improvement = analysis.improvement,
                    playStyle = analysis.playStyle,
                    trends = analysis.trends
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to update analysis")
                // Keep the previous analysis on error
                _playerAnalysis.value = previousAnalysis
            }
        }
    }

    private fun determinePlayStyle(riskLevel: Float): PlayStyle {
        return when {
            riskLevel > 0.7f -> PlayStyle.AGGRESSIVE
            riskLevel < 0.3f -> PlayStyle.CAUTIOUS
            else -> PlayStyle.BALANCED
        }
    }

}
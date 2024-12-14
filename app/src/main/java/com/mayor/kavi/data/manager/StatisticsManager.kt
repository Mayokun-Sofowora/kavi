package com.mayor.kavi.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mayor.kavi.data.GameRepository
import com.mayor.kavi.data.games.GameBoard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import timber.log.Timber
import kotlin.math.*


data class DetailedGameStats(
    val gamesPlayed: Int = 0,
    val highScores: Map<String, Int> = emptyMap(),
    val winRates: Map<String, Pair<Int, Int>> = emptyMap(),
    val averageScores: Map<String, Double> = emptyMap(),
    val playTime: Map<String, Long> = emptyMap(),
    val preferredBoards: List<String> = emptyList(),
    val recentScores: List<GameResult> = emptyList()
)

data class GameResult(
    val board: String,
    val score: Int,
    val timestamp: Long,
    val duration: Long,
    val isWin: Boolean
)

data class GameStats(
    val gamesPlayed: Int,
    val highScores: Map<String, Int>,
    val winRates: Map<String, Pair<Int, Int>>
)

data class PlayerAnalysis(
    val predictedWinRate: Float,
    val consistency: Float,
    val improvement: Float,
    val playStyle: PlayStyle
)

enum class PlayStyle {
    CAUTIOUS, BALANCED, AGGRESSIVE
}

/**
 *  Using TensorFlow Lite to analyze game and player statistics.
 */
class StatisticsManager @Inject constructor(
    private val context: Context,
    private val gameRepository: GameRepository
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "statistics")
    private val dataStore = context.dataStore
    private var interpreter: Interpreter? = null

    init {
        loadTensorFlowModel()
    }

    private fun loadTensorFlowModel() {
        try {
            val modelFile = "dice_stats_model.tflite"
            val model = FileUtil.loadMappedFile(context, modelFile)
            val options = Interpreter.Options()
            interpreter = Interpreter(model, options)
        } catch (e: Exception) {
            Timber.e(e, "Error loading TensorFlow model")
        }
    }

    suspend fun analyzePlayerStats(gameStats: GameStats): PlayerAnalysis {
        return withContext(Dispatchers.Default) {
            val input = prepareInputData(gameStats)
            val outputArray =
                Array(1) { FloatArray(4) } // [winRate, consistency, improvement, preferredStyle]

            interpreter?.run(input, outputArray)

            PlayerAnalysis(
                predictedWinRate = outputArray[0][0],
                consistency = outputArray[0][1],
                improvement = outputArray[0][2],
                playStyle = interpretPlayStyle(outputArray[0][3])
            )
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

    private fun interpretPlayStyle(value: Float): PlayStyle {
        return when {
            value < 0.3 -> PlayStyle.CAUTIOUS
            value < 0.6 -> PlayStyle.BALANCED
            else -> PlayStyle.AGGRESSIVE
        }
    }

    suspend fun updateGameStats(
        board: GameBoard,
        score: Int,
        isWin: Boolean,
        duration: Long = 0
    ) {
        dataStore.edit { prefs ->
            // Update games played
            val currentGamesPlayed = prefs[GAMES_PLAYED_KEY] ?: 0
            prefs[GAMES_PLAYED_KEY] = currentGamesPlayed + 1

            // Update high scores
            val highScores = prefs[HIGH_SCORES_KEY]?.let {
                Json.decodeFromString<Map<String, Int>>(it)
            } ?: emptyMap()

            val updatedHighScores = highScores.toMutableMap().apply {
                val currentHigh = this[board.modeName] ?: 0
                if (score > currentHigh) {
                    this[board.modeName] = score
                }
            }
            prefs[HIGH_SCORES_KEY] = Json.encodeToString(updatedHighScores)

            // Update win rates
            val winRates = prefs[WIN_RATES_KEY]?.let {
                Json.decodeFromString<Map<String, Pair<Int, Int>>>(it)
            } ?: emptyMap()

            val updatedWinRates = winRates.toMutableMap().apply {
                val current = this[board.modeName] ?: (0 to 0)
                this[board.modeName] = (current.first + if (isWin) 1 else 0) to (current.second + 1)
            }
            prefs[WIN_RATES_KEY] = Json.encodeToString(updatedWinRates)
        }
    }

    private fun calculateNewAverages(
        currentAverages: Map<String, Double>,
        board: String,
        newScore: Int
    ): Map<String, Double> {
        val updatedAverages = currentAverages.toMutableMap()
        val currentAverage = currentAverages[board] ?: 0.0
        val gamesPlayed = (currentAverages[board]?.toInt() ?: 0) + 1
        updatedAverages[board] = (currentAverage * (gamesPlayed - 1) + newScore) / gamesPlayed
        return updatedAverages
    }

    fun getGameStats(): Flow<GameStats> = dataStore.data.map { prefs ->
        GameStats(
            gamesPlayed = prefs[GAMES_PLAYED_KEY] ?: 0,
            highScores = prefs[HIGH_SCORES_KEY]?.let {
                Json.decodeFromString<Map<String, Int>>(it)
            } ?: emptyMap(),
            winRates = prefs[WIN_RATES_KEY]?.let {
                Json.decodeFromString<Map<String, Pair<Int, Int>>>(it)
            } ?: emptyMap()
        )
    }

    companion object {
        private val GAMES_PLAYED_KEY = intPreferencesKey("GAMES_PLAYED")
        private val HIGH_SCORES_KEY = stringPreferencesKey("HIGH_SCORES")
        private val WIN_RATES_KEY = stringPreferencesKey("WIN_RATES")
    }
}

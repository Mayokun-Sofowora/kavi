package com.mayor.kavi.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mayor.kavi.data.games.GameBoard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StatisticsManager(context: Context) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "statistics")
    private val dataStore = context.dataStore

    companion object {
        private val STATS_PREFIX = stringPreferencesKey("STATS_")
        private val GAMES_PLAYED_KEY = intPreferencesKey("GAMES_PLAYED")
        private val HIGH_SCORES_KEY = stringPreferencesKey("HIGH_SCORES")
        private val WIN_RATES_KEY = stringPreferencesKey("WIN_RATES")
    }

    suspend fun updateGameStats(board: GameBoard, score: Int, isWin: Boolean) {
        dataStore.edit { prefs ->
            // Update games played
            val gamesPlayed = prefs[GAMES_PLAYED_KEY] ?: 0
            prefs[GAMES_PLAYED_KEY] = gamesPlayed + 1

            // Update high scores
            val highScores = prefs[HIGH_SCORES_KEY]?.let {
                Json.decodeFromString<Map<String, Int>>(it)
            }?.toMutableMap() ?: mutableMapOf()

            if (score > (highScores[board.modeName] ?: 0)) {
                highScores[board.modeName] = score
                prefs[HIGH_SCORES_KEY] = Json.encodeToString<Map<String, Int>>(highScores)
            }

            // Update recent scores

            // Update win rates with explicit typing
            val winRates: MutableMap<String, Pair<Int, Int>> = prefs[WIN_RATES_KEY]?.let {
                Json.decodeFromString<Map<String, Pair<Int, Int>>>(it)
            }?.toMutableMap() ?: mutableMapOf()

            val currentStats = winRates[board.modeName] ?: Pair(0, 0)
            val newStats = Pair(
                currentStats.first + if (isWin) 1 else 0,
                currentStats.second + 1
            )
            winRates[board.modeName] = newStats

            prefs[WIN_RATES_KEY] = Json.encodeToString<Map<String, Pair<Int, Int>>>(winRates)
        }
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
}
data class GameStats(
    val gamesPlayed: Int,
    val highScores: Map<String, Int>,
    val winRates: Map<String, Pair<Int, Int>>
)
package com.mayor.kavi.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mayor.kavi.ui.viewmodel.GameBoard
import kotlinx.coroutines.flow.*
import java.io.IOException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class SettingsDataStoreManager(context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private val dataStore = context.dataStore

    companion object {
        val SELECTED_BOARD_KEY = stringPreferencesKey("SELECTED_BOARD_KEY")
        val SHAKE_ENABLED_KEY = booleanPreferencesKey("SHAKE_ENABLED_KEY")
        val SOUND_ENABLED_KEY = booleanPreferencesKey("SOUND_ENABLED_KEY")
        private val STATS_PREFIX = stringPreferencesKey("STATS_")
        private val GAMES_PLAYED_KEY = intPreferencesKey("GAMES_PLAYED")
        private val HIGH_SCORES_KEY = stringPreferencesKey("HIGH_SCORES")
        private val WIN_RATES_KEY = stringPreferencesKey("WIN_RATES")
    }

    suspend fun setSelectedBoard(board: String) {
        dataStore.edit { pref ->
            pref[SELECTED_BOARD_KEY] = board
        }
    }

    fun getSelectedBoard(): Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { pref ->
            pref[SELECTED_BOARD_KEY] ?: GameBoard.PIG.modeName
        }

    suspend fun setShakeEnabled(enabled: Boolean) {
        dataStore.edit { pref ->
            pref[SHAKE_ENABLED_KEY] = enabled
        }
    }

    fun getShakeEnabled(): Flow<Boolean> = dataStore.data
        .map { pref ->
            pref[SHAKE_ENABLED_KEY] == true
        }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { pref ->
            pref[SOUND_ENABLED_KEY] = enabled
        }
    }

    fun getSoundEnabled(): Flow<Boolean> = dataStore.data
        .map { pref ->
            pref[SOUND_ENABLED_KEY] != false
        }

    suspend fun updateGameStats(
        board: GameBoard,
        score: Int,
        isWin: Boolean
    ) {
        dataStore.edit { prefs ->
            // Update games played
            val gamesPlayed = prefs[GAMES_PLAYED_KEY] ?: 0
            prefs[GAMES_PLAYED_KEY] = gamesPlayed + 1

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
package com.mayor.kavi.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mayor.kavi.data.games.GameBoard
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

class SettingsManager @Inject constructor(context: Context) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private val dataStore = context.dataStore

    companion object {
        private val SHAKE_ENABLED_KEY = booleanPreferencesKey("SHAKE_ENABLED_KEY")
        private val SOUND_ENABLED_KEY = booleanPreferencesKey("SOUND_ENABLED_KEY")
        private val VIBRATION_ENABLED_KEY = booleanPreferencesKey("VIBRATION_ENABLED_KEY")
        private val BOARD_COLOR_KEY = stringPreferencesKey("BOARD_COLOR_KEY")

        private val STATS_PREFIX = stringPreferencesKey("STATS_")
        private val GAMES_PLAYED_KEY = intPreferencesKey("GAMES_PLAYED")
        private val HIGH_SCORES_KEY = stringPreferencesKey("HIGH_SCORES")
        private val WIN_RATES_KEY = stringPreferencesKey("WIN_RATES")

        // Define available board colors
        val BOARD_COLORS = listOf(
            "default",
            "blue",
            "green", 
            "purple",
            "orange"
        )
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

    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { pref ->
            pref[VIBRATION_ENABLED_KEY] = enabled
        }
    }

    fun getVibrationEnabled(): Flow<Boolean> = dataStore.data
        .map { pref ->
            pref[VIBRATION_ENABLED_KEY] == true
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

    fun getBoardColor(): Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[BOARD_COLOR_KEY] ?: "default"
        }

    suspend fun setBoardColor(color: String) {
        if (color !in BOARD_COLORS) return
        dataStore.edit { preferences ->
            preferences[BOARD_COLOR_KEY] = color
        }
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
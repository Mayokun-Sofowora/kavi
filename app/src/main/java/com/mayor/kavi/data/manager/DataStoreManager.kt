package com.mayor.kavi.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mayor.kavi.data.models.GameScoreState
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import java.io.IOException

class DataStoreManager(
    context: Context,
    private val dataStore: DataStore<Preferences> = context.dataStore
) {
    companion object {
        // Game Interface Mode
        private val INTERFACE_MODE_KEY = stringPreferencesKey("interface_mode")
        private const val DEFAULT_INTERFACE_MODE = "classic" // vs "ar_mode"

        // Settings
        private val BOARD_COLOR_KEY = stringPreferencesKey("board_color_key")
        private val SHAKE_ENABLED_KEY = booleanPreferencesKey("shake_enabled")
        private val VIBRATION_ENABLED_KEY = booleanPreferencesKey("vibration_enabled")
        private val CUSTOM_GAMES_KEY = stringPreferencesKey("custom_games")

        private const val DEFAULT_BOARD_COLOR_KEY = "default"
        private const val DEFAULT_SHAKE_ENABLED = false

        // Instance handling
        @Volatile
        private var INSTANCE: DataStoreManager? = null

        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

        fun getInstance(context: Context): DataStoreManager {
            return INSTANCE ?: synchronized(this) {
                DataStoreManager(context).also { INSTANCE = it }
            }
        }
    }

    private val json = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = true
    }

    // Interface Mode (Classic vs AR)
    suspend fun setInterfaceMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[INTERFACE_MODE_KEY] = mode
        }
    }

    fun getInterfaceMode(): Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[INTERFACE_MODE_KEY] ?: DEFAULT_INTERFACE_MODE
        }

    // Shake-to-Roll Setting
    suspend fun setShakeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHAKE_ENABLED_KEY] = enabled
        }
    }

    fun getShakeEnabled(): Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[SHAKE_ENABLED_KEY] ?: DEFAULT_SHAKE_ENABLED
        }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[VIBRATION_ENABLED_KEY] = enabled
        }
    }

    fun getVibrationEnabled(): Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[VIBRATION_ENABLED_KEY] != false  // Default to true
        }

    suspend fun setBoardColor(color: String) {
        dataStore.edit { preferences ->
            preferences[BOARD_COLOR_KEY] = color
        }
    }

    fun getBoardColor(): Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[BOARD_COLOR_KEY] ?: DEFAULT_BOARD_COLOR_KEY
        }

    fun loadCustomGames(): Flow<List<GameScoreState.CustomScoreState>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            try {
                val gamesJson = preferences[CUSTOM_GAMES_KEY] ?: "[]"
                json.decodeFromString<List<GameScoreState.CustomScoreState>>(gamesJson)
            } catch (e: Exception) {
                emptyList()
            }
        }
}

package com.mayor.kavi.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import java.io.IOException

class DataStoreManager(context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")
    private val dataStore = context.dataStore

    companion object {
        // Game Board Selection
        private val SELECTED_BOARD_KEY = stringPreferencesKey("selected_board")
        private const val DEFAULT_BOARD = "pig" // Default board game

        // Game Interface Mode
        private val INTERFACE_MODE_KEY = stringPreferencesKey("interface_mode")
        private const val DEFAULT_INTERFACE_MODE = "classic" // vs "ar_mode"

        // Play Mode
        private val PLAY_MODE_KEY = stringPreferencesKey("play_mode")
        private const val DEFAULT_PLAY_MODE = "vs_ai" // vs "vs_player"

        // Settings
        private val THEME_KEY = stringPreferencesKey("theme")
        private val SHAKE_ENABLED_KEY = booleanPreferencesKey("shake_enabled")
        
        private const val DEFAULT_THEME = "light"
        private const val DEFAULT_SHAKE_ENABLED = false

        // Instance handling
        @Volatile
        private var INSTANCE: DataStoreManager? = null

        fun getInstance(context: Context): DataStoreManager {
            return INSTANCE ?: synchronized(this) {
                DataStoreManager(context).also { INSTANCE = it }
            }
        }
    }

    // Board Selection (Pig, Chicago, etc.)
    suspend fun setSelectedBoard(board: String) {
        dataStore.edit { preferences ->
            preferences[SELECTED_BOARD_KEY] = board
        }
    }

    fun getSelectedBoard(): Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[SELECTED_BOARD_KEY] ?: DEFAULT_BOARD
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

    // Play Mode (VS AI or VS Player)
    suspend fun setPlayMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[PLAY_MODE_KEY] = mode
        }
    }

    fun getPlayMode(): Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PLAY_MODE_KEY] ?: DEFAULT_PLAY_MODE
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

    // Theme Setting
    suspend fun setThemeMode(themeMode: String) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = themeMode
        }
    }

    fun getThemeMode(): Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[THEME_KEY] ?: DEFAULT_THEME
        }
}

package com.mayor.kavi.data.manager

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
        // Game Interface Mode
        private val INTERFACE_MODE_KEY = stringPreferencesKey("interface_mode")
        private const val DEFAULT_INTERFACE_MODE = "classic" // vs "ar_mode"

        // Settings
        private val BOARD_COLOR_KEY = stringPreferencesKey("board_color_key")
        private val SHAKE_ENABLED_KEY = booleanPreferencesKey("shake_enabled")
        private val VIBRATION_ENABLED_KEY = booleanPreferencesKey("vibration_enabled")
        
        private const val DEFAULT_BOARD_COLOR_KEY = "default"
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
}

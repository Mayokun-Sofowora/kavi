package com.mayor.kavi.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mayor.kavi.data.games.*
import kotlinx.coroutines.flow.*
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

        // Define available board colors
        val BOARD_COLORS = BoardColors.getAvailableColors()
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

}
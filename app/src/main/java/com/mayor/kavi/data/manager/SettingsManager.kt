package com.mayor.kavi.data.manager

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mayor.kavi.util.BoardColors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(@ApplicationContext context: Context) {
    companion object {
        private val SHAKE_ENABLED_KEY = booleanPreferencesKey("SHAKE_ENABLED_KEY")
        private val VIBRATION_ENABLED_KEY = booleanPreferencesKey("VIBRATION_ENABLED_KEY")
        private val BOARD_COLOR_KEY = stringPreferencesKey("BOARD_COLOR_KEY")

        const val DEFAULT_BOARD_COLOR = "default"

        val BOARD_COLORS = BoardColors.getAvailableColors()

        val LocalSettingsManager = staticCompositionLocalOf<SettingsManager> {
            error("No SettingsManager provided!")
        }
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private val dataStore = context.dataStore

    suspend fun setShakeEnabled(enabled: Boolean) {
        dataStore.edit { pref ->
            pref[SHAKE_ENABLED_KEY] = enabled
        }
    }

    fun getShakeEnabled(): Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { pref ->
            pref[SHAKE_ENABLED_KEY] == true
        }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { pref ->
            pref[VIBRATION_ENABLED_KEY] = enabled
        }
    }

    fun getVibrationEnabled(): Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { pref ->
            pref[VIBRATION_ENABLED_KEY] == true
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
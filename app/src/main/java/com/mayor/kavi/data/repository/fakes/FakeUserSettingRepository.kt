package com.mayor.kavi.data.repository.fakes

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject

// Interface for SettingsRepository
interface SettingsRepository {
    fun isSoundEnabled(): Boolean
    fun setSoundEnabled(enabled: Boolean)
    fun isNotificationsEnabled(): Boolean
    fun setNotificationsEnabled(enabled: Boolean)
    fun getSelectedTheme(): String
    fun setSelectedTheme(theme: String)
}

// Implementation of SettingsRepository
class SettingsRepositoryImpl @Inject constructor(context: Context) : SettingsRepository {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // Check if sound is enabled
    override fun isSoundEnabled(): Boolean {
        return sharedPreferences.getBoolean("sound_enabled", true)
    }

    // Set sound enabled or disabled
    override fun setSoundEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("sound_enabled", enabled).apply()
    }

    // Check if notifications are enabled
    override fun isNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean("notifications_enabled", true)
    }

    // Set notifications enabled or disabled
    override fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    // Get the selected theme (e.g., "Light" or "Dark")
    override fun getSelectedTheme(): String {
        return sharedPreferences.getString("selected_theme", "Light") ?: "Light"
    }

    // Set the selected theme (e.g., "Light" or "Dark")
    override fun setSelectedTheme(theme: String) {
        sharedPreferences.edit().putString("selected_theme", theme).apply()
    }
}

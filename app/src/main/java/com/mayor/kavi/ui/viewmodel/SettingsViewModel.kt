package com.mayor.kavi.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import com.mayor.kavi.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/***
 * Purpose: Manage app settings.
 * Responsibilities:
 * Store settings like sound, notifications, and theme preferences.
 * Update settings when changed by the user.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isSoundEnabled = MutableStateFlow(settingsRepository.isSoundEnabled())
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled

    private val _isNotificationsEnabled = MutableStateFlow(settingsRepository.isNotificationsEnabled())
    val isNotificationsEnabled: StateFlow<Boolean> = _isNotificationsEnabled

    private val _selectedTheme = MutableStateFlow(settingsRepository.getSelectedTheme())
    val selectedTheme: StateFlow<String> = _selectedTheme

    // Toggle sound setting
    fun toggleSound() {
        val newSoundSetting = !_isSoundEnabled.value
        _isSoundEnabled.value = newSoundSetting
        settingsRepository.setSoundEnabled(newSoundSetting)
    }

    // Toggle notifications setting
    fun toggleNotifications() {
        val newNotificationsSetting = !_isNotificationsEnabled.value
        _isNotificationsEnabled.value = newNotificationsSetting
        settingsRepository.setNotificationsEnabled(newNotificationsSetting)
    }

    // Change the theme
    fun changeTheme(theme: String) {
        _selectedTheme.value = theme
        settingsRepository.setSelectedTheme(theme)
    }
}


package com.mayor.kavi.data.repository.fakes

import com.mayor.kavi.data.models.UserSettingsEntity
import com.mayor.kavi.data.repository.UserSettingsRepository
import java.time.LocalDateTime

class FakeUserSettingsRepository : UserSettingsRepository {

    // In-memory storage for user settings
    private val userSettingsMap = mutableMapOf<Long, MutableList<UserSettingsEntity>>()

    // General CRUD operations
    override suspend fun saveOrUpdateSetting(userId: Long, key: String, value: String) {
        val existingSetting = getSettingByKey(key)
        if (existingSetting != null && existingSetting.userId == userId) {
            // Update existing setting
            val updatedSetting = existingSetting.copy(
                settingsValue = value,
                updatedAt = LocalDateTime.now()
            )
            deleteSetting(existingSetting)
            insertSetting(updatedSetting)
        } else {
            // Insert new setting
            val newSetting = UserSettingsEntity(
                userId = userId,
                settingsKey = key,
                settingsValue = value,
                updatedAt = LocalDateTime.now()
            )
            insertSetting(newSetting)
        }
    }

    override suspend fun getSettingsByUserId(userId: Long): List<UserSettingsEntity> {
        return userSettingsMap[userId]?.toList() ?: emptyList()
    }

    override suspend fun getSettingByKey(key: String): UserSettingsEntity? {
        return userSettingsMap.values.flatten().find { it.settingsKey == key }
    }

    override suspend fun deleteSetting(settings: UserSettingsEntity) {
        userSettingsMap[settings.userId]?.remove(settings)
    }

    override suspend fun deleteSettingsByUserId(userId: Long) {
        userSettingsMap.remove(userId)
    }

    // Game Use Case Functions
    override suspend fun getPreferredGameMode(userId: Long): String? {
        return getSettingByKey("preferred_game_mode")?.settingsValue
    }

    override suspend fun setPreferredGameMode(userId: Long, gameMode: String) {
        saveOrUpdateSetting(userId, "preferred_game_mode", gameMode)
    }

    override suspend fun getThemePreference(userId: Long): String? {
        return getSettingByKey("theme")?.settingsValue
    }

    override suspend fun setThemePreference(userId: Long, theme: String) {
        saveOrUpdateSetting(userId, "theme", theme)
    }

    override suspend fun isARToggleEnabled(userId: Long): Boolean {
        return getSettingByKey("ar_toggle")?.settingsValue?.toBoolean() == true
    }

    override suspend fun setARToggle(userId: Long, isEnabled: Boolean) {
        saveOrUpdateSetting(userId, "ar_toggle", isEnabled.toString())
    }

    override suspend fun getSoundSettings(userId: Long): String? {
        return getSettingByKey("sound_settings")?.settingsValue
    }

    override suspend fun setSoundSettings(userId: Long, soundLevel: String) {
        saveOrUpdateSetting(userId, "sound_settings", soundLevel)
    }

    override suspend fun getDiceStylePreference(userId: Long): String? {
        return getSettingByKey("dice_style")?.settingsValue
    }

    override suspend fun setDiceStylePreference(userId: Long, diceStyle: String) {
        saveOrUpdateSetting(userId, "dice_style", diceStyle)
    }

    override suspend fun getAIDifficulty(userId: Long): String? {
        return getSettingByKey("ai_difficulty")?.settingsValue
    }

    override suspend fun setAIDifficulty(userId: Long, difficulty: String) {
        saveOrUpdateSetting(userId, "ai_difficulty", difficulty)
    }

    override suspend fun isPerformanceAnalyticsEnabled(userId: Long): Boolean {
        return getSettingByKey("performance_analytics")?.settingsValue?.toBoolean() == true
    }

    override suspend fun setPerformanceAnalytics(userId: Long, isEnabled: Boolean) {
        saveOrUpdateSetting(userId, "performance_analytics", isEnabled.toString())
    }

    // Helper function to insert a setting for a user
    private suspend fun insertSetting(setting: UserSettingsEntity) {
        val userSettings = userSettingsMap.getOrPut(setting.userId) { mutableListOf() }
        userSettings.add(setting)
    }
}

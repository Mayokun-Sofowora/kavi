package com.mayor.kavi.data.repository

import com.mayor.kavi.data.models.UserSettingsEntity
import com.mayor.kavi.data.dao.UserSettingsDao
import java.time.LocalDateTime
import javax.inject.Inject

interface UserSettingsRepository {

    // General CRUD operations
    suspend fun saveOrUpdateSetting(userId: Long, key: String, value: String)
    suspend fun getSettingsByUserId(userId: Long): List<UserSettingsEntity>
    suspend fun getSettingByKey(key: String): UserSettingsEntity?
    suspend fun deleteSetting(settings: UserSettingsEntity)
    suspend fun deleteSettingsByUserId(userId: Long)

    // Game Use Case Functions
    suspend fun getPreferredGameMode(userId: Long): String?
    suspend fun setPreferredGameMode(userId: Long, gameMode: String)

    suspend fun getThemePreference(userId: Long): String?
    suspend fun setThemePreference(userId: Long, theme: String)

    suspend fun isARToggleEnabled(userId: Long): Boolean
    suspend fun setARToggle(userId: Long, isEnabled: Boolean)

    suspend fun getSoundSettings(userId: Long): String?
    suspend fun setSoundSettings(userId: Long, soundLevel: String)

    suspend fun getDiceStylePreference(userId: Long): String?
    suspend fun setDiceStylePreference(userId: Long, diceStyle: String)

    suspend fun getAIDifficulty(userId: Long): String?
    suspend fun setAIDifficulty(userId: Long, difficulty: String)

    suspend fun isPerformanceAnalyticsEnabled(userId: Long): Boolean
    suspend fun setPerformanceAnalytics(userId: Long, isEnabled: Boolean)
}

class UserSettingsRepositoryImpl @Inject constructor(
    private val userSettingsDao: UserSettingsDao
) : UserSettingsRepository {

    // General CRUD operations
    override suspend fun saveOrUpdateSetting(userId: Long, key: String, value: String) {
        val existingSetting = userSettingsDao.getSettingsByKey(key)
        if (existingSetting != null && existingSetting.userId == userId) {
            val updatedSetting = existingSetting.copy(
                settingsValue = value,
                updatedAt = LocalDateTime.now()
            )
            userSettingsDao.updateSettings(updatedSetting)
        } else {
            val newSetting = UserSettingsEntity(
                userId = userId,
                settingsKey = key,
                settingsValue = value,
                updatedAt = LocalDateTime.now()
            )
            userSettingsDao.insertSettings(newSetting)
        }
    }

    override suspend fun getSettingsByUserId(userId: Long): List<UserSettingsEntity> {
        return userSettingsDao.getSettingsByUserId(userId)
    }

    override suspend fun getSettingByKey(key: String): UserSettingsEntity? {
        return userSettingsDao.getSettingsByKey(key)
    }

    override suspend fun deleteSetting(settings: UserSettingsEntity) {
        userSettingsDao.deleteSettings(settings)
    }

    override suspend fun deleteSettingsByUserId(userId: Long) {
        val userSettings = userSettingsDao.getSettingsByUserId(userId)
        userSettings.forEach { userSettingsDao.deleteSettings(it) }
    }

    // Game Use Case Functions
    override suspend fun getPreferredGameMode(userId: Long): String? {
        return userSettingsDao.getSettingsByKey("preferred_game_mode")?.settingsValue
    }

    override suspend fun setPreferredGameMode(userId: Long, gameMode: String) {
        saveOrUpdateSetting(userId, "preferred_game_mode", gameMode)
    }

    override suspend fun getThemePreference(userId: Long): String? {
        return userSettingsDao.getSettingsByKey("theme")?.settingsValue
    }

    override suspend fun setThemePreference(userId: Long, theme: String) {
        saveOrUpdateSetting(userId, "theme", theme)
    }

    override suspend fun isARToggleEnabled(userId: Long): Boolean {
        return userSettingsDao.getSettingsByKey("ar_toggle")?.settingsValue?.toBoolean() == true
    }

    override suspend fun setARToggle(userId: Long, isEnabled: Boolean) {
        saveOrUpdateSetting(userId, "ar_toggle", isEnabled.toString())
    }

    override suspend fun getSoundSettings(userId: Long): String? {
        return userSettingsDao.getSettingsByKey("sound_settings")?.settingsValue
    }

    override suspend fun setSoundSettings(userId: Long, soundLevel: String) {
        saveOrUpdateSetting(userId, "sound_settings", soundLevel)
    }

    override suspend fun getDiceStylePreference(userId: Long): String? {
        return userSettingsDao.getSettingsByKey("dice_style")?.settingsValue
    }

    override suspend fun setDiceStylePreference(userId: Long, diceStyle: String) {
        saveOrUpdateSetting(userId, "dice_style", diceStyle)
    }

    override suspend fun getAIDifficulty(userId: Long): String? {
        return userSettingsDao.getSettingsByKey("ai_difficulty")?.settingsValue
    }

    override suspend fun setAIDifficulty(userId: Long, difficulty: String) {
        saveOrUpdateSetting(userId, "ai_difficulty", difficulty)
    }

    override suspend fun isPerformanceAnalyticsEnabled(userId: Long): Boolean {
        return userSettingsDao.getSettingsByKey("performance_analytics")?.settingsValue?.toBoolean() == true
    }

    override suspend fun setPerformanceAnalytics(userId: Long, isEnabled: Boolean) {
        saveOrUpdateSetting(userId, "performance_analytics", isEnabled.toString())
    }

}

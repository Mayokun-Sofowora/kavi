package com.mayor.kavi.data.dao

import androidx.room.*
import com.mayor.kavi.data.models.UserSettingsEntity

@Dao
interface UserSettingsDao {
    @Insert
    suspend fun insertSettings(settings: UserSettingsEntity)

    @Update
    suspend fun updateSettings(settings: UserSettingsEntity)

    @Delete
    suspend fun deleteSettings(settings: UserSettingsEntity)

    @Query("SELECT * FROM user_settings WHERE user_id = :userId")
    suspend fun getSettingsByUserId(userId: Long): List<UserSettingsEntity>

    @Query("SELECT * FROM user_settings WHERE settings_key = :settingsKey")
    suspend fun getSettingsByKey(settingsKey: String): UserSettingsEntity?
}

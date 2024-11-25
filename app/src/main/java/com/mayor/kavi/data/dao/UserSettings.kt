package com.mayor.kavi.data.dao

import androidx.room.*
import java.time.LocalDateTime

@Entity(
    tableName = "user_settings",
    foreignKeys = [
        ForeignKey(
            entity = Users::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserSettings(
    @PrimaryKey(autoGenerate = true) val settingsId: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "settings_key") val settingsKey: String,
    @ColumnInfo(name = "settings_value") val settingsValue: String,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime
)

@Dao
interface UserSettingsDao {
    @Insert
    suspend fun insertSettings(settings: UserSettings)

    @Update
    suspend fun updateSettings(settings: UserSettings)

    @Delete
    suspend fun deleteSettings(settings: UserSettings)

    @Query("SELECT * FROM user_settings WHERE user_id = :userId")
    suspend fun getSettingsByUserId(userId: Long): List<UserSettings>

    @Query("SELECT * FROM user_settings WHERE settings_key = :settingsKey")
    suspend fun getSettingsByKey(settingsKey: String): UserSettings?
}

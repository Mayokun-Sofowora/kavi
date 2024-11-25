package com.mayor.kavi.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "achievements",
    foreignKeys = [
        ForeignKey(entity = Users::class, parentColumns = ["userId"], childColumns = ["userId"])
    ]
)
data class Achievements(
    @PrimaryKey val achievementId: Long,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "unlocked") val unlocked: Boolean,
    @ColumnInfo(name = "icon_resource") val iconResource: Int
)

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievements)

    @Query("SELECT * FROM achievements WHERE achievementId = :achievementId")
    suspend fun getAchievementById(achievementId: Long): Achievements?

    @Query("SELECT * FROM achievements WHERE user_id = :userId")
    fun getAchievementsForUser(userId: Long): Flow<List<Achievements>>  // Get all achievements for a specific user

    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<Achievements>>

    @Delete
    suspend fun deleteAchievement(achievement: Achievements)
}

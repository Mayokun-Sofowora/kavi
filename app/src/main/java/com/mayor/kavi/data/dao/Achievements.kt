package com.mayor.kavi.data.dao

import androidx.room.*
import com.mayor.kavi.data.models.AchievementsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementsEntity)

    @Query("SELECT * FROM achievements WHERE achievementId = :achievementId")
    suspend fun getAchievementById(achievementId: Long): AchievementsEntity?

    @Query("SELECT * FROM achievements WHERE user_id = :userId")
    fun getAchievementsForUser(userId: Long): Flow<List<AchievementsEntity>>  // Get all achievements for a specific user

    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<AchievementsEntity>>

    @Delete
    suspend fun deleteAchievement(achievement: AchievementsEntity)
}

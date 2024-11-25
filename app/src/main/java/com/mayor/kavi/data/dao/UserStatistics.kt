package com.mayor.kavi.data.dao

import androidx.room.*
import com.mayor.kavi.data.models.UserStatisticsEntity

@Dao
interface UserStatisticsDao {
    @Insert
    suspend fun insertStatistics(statistics: UserStatisticsEntity)

    @Update
    suspend fun updateStatistics(statistics: UserStatisticsEntity)

    @Delete
    suspend fun deleteStatistics(statistics: UserStatisticsEntity)

    @Query("SELECT * FROM user_statistics WHERE user_id = :userId")
    suspend fun getStatisticsByUserId(userId: Long): UserStatisticsEntity?

    @Query("SELECT * FROM user_statistics")
    suspend fun getAllUserStatistics(): List<UserStatisticsEntity>
}

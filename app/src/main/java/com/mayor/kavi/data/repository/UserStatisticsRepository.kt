package com.mayor.kavi.data.repository

import com.mayor.kavi.data.dao.UserStatisticsDao
import com.mayor.kavi.data.models.AchievementsEntity
import com.mayor.kavi.data.models.GameModes
import com.mayor.kavi.data.models.UserStatisticsEntity
import java.time.LocalDateTime
import javax.inject.Inject

interface UserStatisticsRepository {

    // Basic CRUD operations
    suspend fun insertStatistics(statistics: UserStatisticsEntity)
    suspend fun updateStatistics(statistics: UserStatisticsEntity)
    suspend fun deleteStatistics(statistics: UserStatisticsEntity)
    suspend fun getStatisticsByUserId(userId: Long): UserStatisticsEntity?
    suspend fun getAllUserStatistics(): List<UserStatisticsEntity>

    // Additional Analytics and Game Use Cases
    suspend fun incrementGamesPlayed(userId: Long, isWin: Boolean)
    suspend fun updateAchievements(userId: Long, achievements: Set<AchievementsEntity>)
    suspend fun updateFavoriteGameMode(userId: Long, gameMode: GameModes)
    suspend fun updatePlayTime(userId: Long, playTime: Long)
    suspend fun updateScores(userId: Long, newScore: Int)
}

class UserStatisticsRepositoryImpl @Inject constructor(
    private val userStatisticsDao: UserStatisticsDao
) : UserStatisticsRepository {

    // Basic CRUD operations
    override suspend fun insertStatistics(statistics: UserStatisticsEntity) {
        userStatisticsDao.insertStatistics(statistics)
    }

    override suspend fun updateStatistics(statistics: UserStatisticsEntity) {
        userStatisticsDao.updateStatistics(statistics)
    }

    override suspend fun deleteStatistics(statistics: UserStatisticsEntity) {
        userStatisticsDao.deleteStatistics(statistics)
    }

    override suspend fun getStatisticsByUserId(userId: Long): UserStatisticsEntity? {
        return userStatisticsDao.getStatisticsByUserId(userId)
    }

    override suspend fun getAllUserStatistics(): List<UserStatisticsEntity> {
        return userStatisticsDao.getAllUserStatistics()
    }

    // Increment games played, won, or lost
    override suspend fun incrementGamesPlayed(userId: Long, isWin: Boolean) {
        val stats = userStatisticsDao.getStatisticsByUserId(userId)
        stats?.let {
            it.totalGamesPlayed += 1
            if (isWin) {
                it.totalGamesWon += 1
            } else {
                it.totalGamesLost += 1
            }
            it.updatedAt = LocalDateTime.now() // Update timestamp
            userStatisticsDao.updateStatistics(it)
        } ?: run {
            // Handle case when statistics are not found, could insert a new record or log an error
        }
    }

    // Update achievements
    override suspend fun updateAchievements(
        userId: Long,
        newAchievements: Set<AchievementsEntity>
    ) {
        val stats = userStatisticsDao.getStatisticsByUserId(userId)
        stats?.let {
            val updatedAchievements = it.achievements.toMutableSet()
            updatedAchievements.addAll(newAchievements)
            it.achievements = updatedAchievements
            it.updatedAt = LocalDateTime.now() // Update timestamp
            userStatisticsDao.updateStatistics(it)
        } ?: run {
            // Handle case when statistics are not found
        }
    }

    // Update favorite game mode
    override suspend fun updateFavoriteGameMode(userId: Long, gameMode: GameModes) {
        val stats = userStatisticsDao.getStatisticsByUserId(userId)
        stats?.let {
            it.favoriteGameMode = gameMode
            it.updatedAt = LocalDateTime.now() // Update timestamp
            userStatisticsDao.updateStatistics(it)
        } ?: run {
            // Handle case when statistics are not found
        }
    }

    // Update total play time
    override suspend fun updatePlayTime(userId: Long, playTime: Long) {
        val stats = userStatisticsDao.getStatisticsByUserId(userId)
        stats?.let {
            it.totalPlayTime += playTime
            it.updatedAt = LocalDateTime.now() // Update timestamp
            userStatisticsDao.updateStatistics(it)
        } ?: run {
            // Handle case when statistics are not found
        }
    }

    // Update scores (highest and average)
    override suspend fun updateScores(userId: Long, newScore: Int) {
        val stats = userStatisticsDao.getStatisticsByUserId(userId)
        stats?.let {
            it.highestScore = maxOf(it.highestScore, newScore)
            it.averageScore =
                ((it.averageScore * it.totalGamesPlayed) + newScore) / (it.totalGamesPlayed + 1)
            it.updatedAt = LocalDateTime.now() // Update timestamp
            userStatisticsDao.updateStatistics(it)
        } ?: run {
            // Handle case when statistics are not found
        }
    }
}
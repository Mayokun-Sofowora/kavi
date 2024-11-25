package com.mayor.kavi.data.repository

import com.mayor.kavi.data.dao.UserStatisticsDao
import com.mayor.kavi.data.dao.UserStatistics
import com.mayor.kavi.data.models.GameModes
import java.time.LocalDateTime
import javax.inject.Inject

interface UserStatisticsRepository {

    // Basic CRUD operations
    suspend fun insertStatistics(statistics: UserStatistics)
    suspend fun updateStatistics(statistics: UserStatistics)
    suspend fun deleteStatistics(statistics: UserStatistics)
    suspend fun getStatisticsByUserId(userId: Long): UserStatistics?
    suspend fun getAllUserStatistics(): List<UserStatistics>

    // Additional Analytics and Game Use Cases
    suspend fun incrementGamesPlayed(userId: Long, isWin: Boolean)
    suspend fun updateAchievements(userId: Long, newAchievements: String)
    suspend fun updateFavoriteGameMode(userId: Long, gameMode: GameModes)
    suspend fun updatePlayTime(userId: Long, playTime: Long)
    suspend fun updateScores(userId: Long, newScore: Int)
}

class UserStatisticsRepositoryImpl @Inject constructor(
    private val userStatisticsDao: UserStatisticsDao
) : UserStatisticsRepository {

    // Basic CRUD operations
    override suspend fun insertStatistics(statistics: UserStatistics) {
        userStatisticsDao.insertStatistics(statistics)
    }

    override suspend fun updateStatistics(statistics: UserStatistics) {
        userStatisticsDao.updateStatistics(statistics)
    }

    override suspend fun deleteStatistics(statistics: UserStatistics) {
        userStatisticsDao.deleteStatistics(statistics)
    }

    override suspend fun getStatisticsByUserId(userId: Long): UserStatistics? {
        return userStatisticsDao.getStatisticsByUserId(userId)
    }

    override suspend fun getAllUserStatistics(): List<UserStatistics> {
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
            userStatisticsDao.updateStatistics(it)
        }
    }

    // Update achievements
    override suspend fun updateAchievements(userId: Long, newAchievements: String) {
        val stats = userStatisticsDao.getStatisticsByUserId(userId)
        stats?.let {
            it.achievements = "${it.achievements},$newAchievements".trim(',')
            it.updatedAt = LocalDateTime.now()
            userStatisticsDao.updateStatistics(it)
        }
    }

    // Update favorite game mode
    override suspend fun updateFavoriteGameMode(userId: Long, gameMode: GameModes) {
        val stats = userStatisticsDao.getStatisticsByUserId(userId)
        stats?.let {
            it.favoriteGameMode = gameMode
            it.updatedAt = LocalDateTime.now()
            userStatisticsDao.updateStatistics(it)
        }
    }

    // Update total play time
    override suspend fun updatePlayTime(userId: Long, playTime: Long) {
        val stats = userStatisticsDao.getStatisticsByUserId(userId)
        stats?.let {
            it.totalPlayTime += playTime
            it.updatedAt = LocalDateTime.now()
            userStatisticsDao.updateStatistics(it)
        }
    }

    // Update scores (highest and average)
    override suspend fun updateScores(userId: Long, newScore: Int) {
        val stats = userStatisticsDao.getStatisticsByUserId(userId)
        stats?.let {
            it.highestScore = maxOf(it.highestScore, newScore)
            it.averageScore =
                ((it.averageScore * it.totalGamesPlayed) + newScore) / (it.totalGamesPlayed + 1)
            it.updatedAt = LocalDateTime.now()
            userStatisticsDao.updateStatistics(it)
        }
    }

}

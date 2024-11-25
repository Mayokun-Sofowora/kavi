package com.mayor.kavi.data.repository.fakes

import com.mayor.kavi.data.models.AchievementsEntity
import com.mayor.kavi.data.models.GameModes
import com.mayor.kavi.data.models.UserStatisticsEntity
import com.mayor.kavi.data.repository.UserStatisticsRepository
import java.time.LocalDateTime

class FakeUserStatisticsRepository : UserStatisticsRepository {

    // In-memory storage for user statistics
    private val userStatisticsMap = mutableMapOf<Long, UserStatisticsEntity>()

    // Basic CRUD operations
    override suspend fun insertStatistics(statistics: UserStatisticsEntity) {
        userStatisticsMap[statistics.userId] = statistics
    }

    override suspend fun updateStatistics(statistics: UserStatisticsEntity) {
        userStatisticsMap[statistics.userId] = statistics
    }

    override suspend fun deleteStatistics(statistics: UserStatisticsEntity) {
        userStatisticsMap.remove(statistics.userId)
    }

    override suspend fun getStatisticsByUserId(userId: Long): UserStatisticsEntity? {
        return userStatisticsMap[userId]
    }

    override suspend fun getAllUserStatistics(): List<UserStatisticsEntity> {
        return userStatisticsMap.values.toList()
    }

    // Increment games played, won, or lost
    override suspend fun incrementGamesPlayed(userId: Long, isWin: Boolean) {
        val stats = userStatisticsMap[userId]
        stats?.let {
            it.totalGamesPlayed += 1
            if (isWin) {
                it.totalGamesWon += 1
            } else {
                it.totalGamesLost += 1
            }
            it.updatedAt = LocalDateTime.now() // Update timestamp
            userStatisticsMap[userId] = it
        } ?: run {
            // Handle case when statistics are not found
        }
    }

    // Update achievements
    override suspend fun updateAchievements(
        userId: Long,
        newAchievements: Set<AchievementsEntity>
    ) {
        val stats = userStatisticsMap[userId]
        stats?.let {
            val updatedAchievements = it.achievements.toMutableSet()
            updatedAchievements.addAll(newAchievements)
            it.achievements = updatedAchievements
            it.updatedAt = LocalDateTime.now() // Update timestamp
            userStatisticsMap[userId] = it
        } ?: run {
            // Handle case when statistics are not found
        }
    }

    // Update favorite game mode
    override suspend fun updateFavoriteGameMode(userId: Long, gameMode: GameModes) {
        val stats = userStatisticsMap[userId]
        stats?.let {
            it.favoriteGameMode = gameMode
            it.updatedAt = LocalDateTime.now() // Update timestamp
            userStatisticsMap[userId] = it
        } ?: run {
            // Handle case when statistics are not found
        }
    }

    // Update total play time
    override suspend fun updatePlayTime(userId: Long, playTime: Long) {
        val stats = userStatisticsMap[userId]
        stats?.let {
            it.totalPlayTime += playTime
            it.updatedAt = LocalDateTime.now() // Update timestamp
            userStatisticsMap[userId] = it
        } ?: run {
            // Handle case when statistics are not found
        }
    }

    // Update scores (highest and average)
    override suspend fun updateScores(userId: Long, newScore: Int) {
        val stats = userStatisticsMap[userId]
        stats?.let {
            it.highestScore = maxOf(it.highestScore, newScore)
            it.averageScore =
                ((it.averageScore * it.totalGamesPlayed) + newScore) / (it.totalGamesPlayed + 1)
            it.updatedAt = LocalDateTime.now() // Update timestamp
            userStatisticsMap[userId] = it
        } ?: run {
            // Handle case when statistics are not found
        }
    }
}

package com.mayor.kavi.data.repository.fakes

import com.mayor.kavi.data.dao.PlayerStatsEntity
import com.mayor.kavi.data.repository.StatisticsRepository
import com.mayor.kavi.data.models.*
import com.mayor.kavi.utils.deserializeAchievements
import com.mayor.kavi.utils.serializeAchievements
import com.mayor.kavi.utils.toDomain
import com.mayor.kavi.utils.toEntity
import kotlinx.coroutines.flow.*

class FakeUserStatisticsRepository : StatisticsRepository {
    // Simulate a fake in-memory storage
    private val playerStatsMap = mutableMapOf<String, PlayerStatsEntity>()

    override fun getPlayerStats(playerId: String): Flow<PlayerStats> = flow {
        emit(playerStatsMap[playerId]?.toDomain() ?: defaultPlayerStats(playerId))
    }

    override suspend fun updatePlayerStats(playerId: String, gameWon: Boolean) {
        val currentStats = playerStatsMap[playerId] ?: defaultPlayerStats(playerId).toEntity()

        val updatedGamesPlayed = currentStats.totalGamesPlayed + 1
        val updatedGamesWon =
            if (gameWon) currentStats.totalGamesWon + 1 else currentStats.totalGamesWon
        val level = calculatePlayerLevel(updatedGamesWon)

        // Check for new achievements
        val updatedAchievements = updateAchievements(currentStats, gameWon)

        val updatedStats = currentStats.copy(
            totalGamesPlayed = updatedGamesPlayed,
            totalGamesWon = updatedGamesWon,
            level = level,
            achievements = serializeAchievements(updatedAchievements)
        )
        insertPlayerStats(updatedStats)
    }

    override suspend fun unlockAchievement(playerId: String, achievement: Achievement) {
        val currentStats = playerStatsMap[playerId] ?: return
        val existingAchievements = deserializeAchievements(currentStats.achievements)
        val updatedAchievements = existingAchievements + achievement

        insertPlayerStats(
            currentStats.copy(achievements = serializeAchievements(updatedAchievements))
        )
    }

    override fun getLeaderboard(): Flow<List<PlayerStats>> = flow {
        emit(playerStatsMap.values
            .map { it.toDomain() }
            .sortedWith(compareByDescending<PlayerStats> { it.totalGamesWon }
                .thenByDescending { it.totalGamesPlayed })
        )
    }

    override suspend fun resetPlayerStats(playerId: String) {
        val defaultStats = defaultPlayerStats(playerId).toEntity()
        insertPlayerStats(defaultStats)
    }

    private suspend fun insertPlayerStats(stats: PlayerStatsEntity) {
        playerStatsMap[stats.playerId] = stats
    }

    private fun defaultPlayerStats(playerId: String) = PlayerStats(
        playerId = playerId,
        level = "Beginner",
        totalGamesPlayed = 0,
        totalGamesWon = 0,
        achievements = emptySet(),
        highestScore = 0,
        averageScore = 0.0,
        favoriteGameMode = GameMode.CLASSIC,
        totalPlayTime = 0
    )

    private fun calculatePlayerLevel(totalWins: Int): String {
        return when {
            totalWins >= 50 -> "Expert"
            totalWins >= 20 -> "Intermediate"
            else -> "Beginner"
        }
    }

    private fun updateAchievements(stats: PlayerStatsEntity, gameWon: Boolean): Set<Achievement> {
        val achievements = deserializeAchievements(stats.achievements).toMutableSet()

        achievements.addIfAbsent(Achievement(1L, "First Game Played", "Play your first game", true))
        if (stats.totalGamesWon >= 10) {
            achievements.addIfAbsent(Achievement(2L, "10 Games Won", "Win 10 games", true))
        }
        if (stats.totalGamesWon >= 50) {
            achievements.addIfAbsent(Achievement(3L, "50 Games Won", "Win 50 games", true))
        }
        if (gameWon && stats.totalGamesWon % 3 == 0) {
            achievements.addIfAbsent(
                Achievement(
                    4L,
                    "Winning Streak",
                    "Win 3 games in a row",
                    true
                )
            )
        }

        return achievements
    }

    private fun MutableSet<Achievement>.addIfAbsent(achievement: Achievement) {
        if (!this.contains(achievement)) {
            this.add(achievement)
        }
    }
}
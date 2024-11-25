package com.mayor.kavi.data.repository.fakes

import com.mayor.kavi.data.repository.LeaderboardRepository
import com.mayor.kavi.data.models.LeaderboardEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeLeaderboardRepository : LeaderboardRepository {

    private val leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())

    override fun getLeaderboard(): Flow<List<LeaderboardEntry>> = leaderboard

    override suspend fun addPlayerToLeaderboard(
        playerId: String,
        playerName: String,
        wins: Int,
        gamesPlayed: Int
    ) {
        val newEntry = LeaderboardEntry(
            rank = leaderboard.value.size + 1,
            playerName = playerName,
            totalWins = wins,
            totalGames = gamesPlayed,
            level = determinePlayerLevel(wins)
        )

        // Add new entry to the leaderboard and re-sort
        leaderboard.value = (leaderboard.value + newEntry).sortedByDescending { it.totalWins }
    }

    override suspend fun getPlayerRank(playerId: String): Int {
        return leaderboard.value.indexOfFirst { it.playerName == playerId } + 1
    }

    override suspend fun updateLeaderboard(playerId: String, wins: Int, gamesPlayed: Int) {
        leaderboard.value = leaderboard.value.map {
            if (it.playerName == playerId) {
                it.copy(
                    totalWins = wins,
                    totalGames = gamesPlayed,
                    level = determinePlayerLevel(wins)
                )
            } else it
        }.sortedByDescending { it.totalWins }
    }

    override suspend fun deleteLeaderboardEntry(playerId: String) {
        leaderboard.value = leaderboard.value.filterNot { it.playerName == playerId }
            .mapIndexed { index, entry -> entry.copy(rank = index + 1) } // Recalculate ranks
    }

    private fun determinePlayerLevel(totalWins: Int): String {
        return when {
            totalWins >= 50 -> "Expert"
            totalWins >= 20 -> "Intermediate"
            else -> "Beginner"
        }
    }
}

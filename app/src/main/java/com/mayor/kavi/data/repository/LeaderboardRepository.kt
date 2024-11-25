package com.mayor.kavi.data.repository

import com.mayor.kavi.data.dao.LeaderboardDao
import com.mayor.kavi.data.dao.LeaderboardEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface LeaderboardRepository {

    /**
     * Get the top players in the leaderboard.
     */
    fun getLeaderboard(): Flow<List<LeaderboardEntry>>

    /**
     * Add a new player's score to the leaderboard.
     */
    suspend fun addPlayerToLeaderboard(
        playerId: String,
        playerName: String,
        wins: Int,
        gamesPlayed: Int
    )

    /**
     * Get the player's current rank.
     */
    suspend fun getPlayerRank(playerId: String): Int

    /**
     * Update a player's score.
     */
    suspend fun updateLeaderboard(playerId: String, wins: Int, gamesPlayed: Int)

    /**
     * Delete a player's entry from the leaderboard.
     */
    suspend fun deleteLeaderboardEntry(playerId: String)
}

class LeaderboardRepositoryImpl @Inject constructor(
    private val leaderboardDao: LeaderboardDao
) : LeaderboardRepository {

    override fun getLeaderboard(): Flow<List<LeaderboardEntry>> {
        return leaderboardDao.getAllLeaderboardEntries().map { entries ->
            entries.map { entity ->
                LeaderboardEntry(
                    rank = entity.rank,
                    playerName = entity.playerName,
                    totalWins = entity.totalWins,
                    totalGames = entity.totalGames,
                    level = entity.level
                )
            }
        }
    }

    override suspend fun addPlayerToLeaderboard(
        playerId: String,
        playerName: String,
        wins: Int,
        gamesPlayed: Int
    ) {
        val leaderboardSize = leaderboardDao.getAllLeaderboardEntries().first().size
        val rank = leaderboardSize + 1 // Calculate rank dynamically
        val entry = LeaderboardEntry(
            rank = rank,
            playerName = playerName,
            totalWins = wins,
            totalGames = gamesPlayed,
            level = determinePlayerLevel(wins)
        )
        leaderboardDao.insertLeaderboardEntry(entry)
    }

    override suspend fun getPlayerRank(playerId: String): Int {
        val leaderboardEntries = leaderboardDao.getAllLeaderboardEntries().first()
        return leaderboardEntries.indexOfFirst { it.playerName == playerId } + 1
    }

    override suspend fun updateLeaderboard(playerId: String, wins: Int, gamesPlayed: Int) {
        val leaderboardEntries = leaderboardDao.getAllLeaderboardEntries().first()
        leaderboardEntries.forEach {
            if (it.playerName == playerId) {
                val updatedEntry = it.copy(
                    totalWins = wins,
                    totalGames = gamesPlayed,
                    level = determinePlayerLevel(wins)
                )
                leaderboardDao.insertLeaderboardEntry(updatedEntry) // Update the entry
            }
        }
    }

    private fun determinePlayerLevel(totalWins: Int): String {
        return when {
            totalWins >= 50 -> "Expert"
            totalWins >= 20 -> "Intermediate"
            else -> "Beginner"
        }
    }

    override suspend fun deleteLeaderboardEntry(playerId: String) {
        val leaderboardEntries =
            leaderboardDao.getAllLeaderboardEntries().firstOrNull() // Collect the Flow into a list
        val entryToDelete =
            leaderboardEntries?.firstOrNull { it.playerName == playerId } // Find the matching entry
        entryToDelete?.let {
            leaderboardDao.deleteLeaderboardEntry(it) // Delete the entry
        }
    }

}
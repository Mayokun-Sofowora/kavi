package com.mayor.kavi.data.repository.fakes

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import kotlin.test.assertEquals
import org.junit.Test

class FakeLeaderboardRepositoryTest {

    private lateinit var leaderboardRepository: FakeLeaderboardRepository

    @Before
    fun setUp() {
        leaderboardRepository = FakeLeaderboardRepository()
    }

    @Test
    fun `test addPlayerToLeaderboard`() = runBlocking {
        leaderboardRepository.addPlayerToLeaderboard("player1", "Player One", 10, 15)

        val leaderboard = leaderboardRepository.getLeaderboard().first()
        assertEquals(1, leaderboard.size) // Check if player was added
        assertEquals("Player One", leaderboard[0].playerName)
        assertEquals(10, leaderboard[0].totalWins)
        assertEquals(15, leaderboard[0].totalGames)
    }

    @Test
    fun `test getPlayerRank`() = runBlocking {
        leaderboardRepository.addPlayerToLeaderboard("player1", "Player One", 10, 15)
        leaderboardRepository.addPlayerToLeaderboard("player2", "Player Two", 20, 25)

        val rank = leaderboardRepository.getPlayerRank("player1")
        assertEquals(2, rank) // Player One should be ranked 2nd
    }

    @Test
    fun `test updateLeaderboard`() = runBlocking {
        leaderboardRepository.addPlayerToLeaderboard("player1", "Player One", 10, 15)

        // Update player's stats
        leaderboardRepository.updateLeaderboard("player1", 20, 30)

        val leaderboard = leaderboardRepository.getLeaderboard().first()
        assertEquals(1, leaderboard.size)
        assertEquals(20, leaderboard[0].totalWins) // Check if wins are updated
        assertEquals(30, leaderboard[0].totalGames) // Check if games played are updated
    }

    @Test
    fun `test deleteLeaderboardEntry should remove the correct entry and update ranks`() = runTest {
        // Arrange: Add some entries
        leaderboardRepository.addPlayerToLeaderboard("1", "Player1", 10, 15)
        leaderboardRepository.addPlayerToLeaderboard("2", "Player2", 20, 25)
        leaderboardRepository.addPlayerToLeaderboard("3", "Player3", 30, 35)

        // Act: Delete "Player2"
        leaderboardRepository.deleteLeaderboardEntry("2")

        // Assert: Remaining entries and ranks
        val leaderboard = leaderboardRepository.getLeaderboard().first()
        assertEquals(2, leaderboard.size) // One entry removed
        assertEquals("Player3", leaderboard[0].playerName) // Player3 is now rank 1
        assertEquals(1, leaderboard[0].rank)
        assertEquals("Player1", leaderboard[1].playerName) // Player1 is now rank 2
        assertEquals(2, leaderboard[1].rank)
    }
}

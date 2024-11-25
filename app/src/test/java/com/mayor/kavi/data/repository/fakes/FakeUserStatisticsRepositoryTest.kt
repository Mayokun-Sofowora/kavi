package com.mayor.kavi.data.repository.fakes

import com.mayor.kavi.data.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.*

class FakeUserStatisticsRepositoryTest {

    private lateinit var fakeStatisticsRepository: FakeUserStatisticsRepository

    @Before
    fun setUp() {
        fakeStatisticsRepository = FakeUserStatisticsRepository()
    }

    @Test
    fun `test getPlayerStats returns default stats for new player`() = runTest {
        val playerId = "player1"
        val statsFlow = fakeStatisticsRepository.getPlayerStats(playerId)

        // Collect the emitted value (first value in flow)
        val stats = statsFlow.toList().first()

        assertEquals(playerId, stats.playerId)
        assertEquals("Beginner", stats.level)
        assertEquals(0, stats.totalGamesPlayed)
        assertEquals(0, stats.totalGamesWon)
        assertTrue(stats.achievements.isEmpty())
    }

    @Test
    fun `test updatePlayerStats updates stats correctly`() = runTest {
        val playerId = "player1"
        fakeStatisticsRepository.updatePlayerStats(playerId, gameWon = true)

        // Verify stats have been updated correctly
        val stats = fakeStatisticsRepository.getPlayerStats(playerId).toList().first()

        assertEquals(1, stats.totalGamesPlayed)
        assertEquals(1, stats.totalGamesWon)
        assertEquals("Beginner", stats.level) // Level should be "Beginner" since total wins are 1
    }

    @Test
    fun `test UpdatePlayerStats`() = runBlocking {
        val fakeRepo = FakeGameRepository()

        val game = Game(
            id = 1,
            players = listOf("Alice", "Bob"),
            score = mapOf("Alice" to 30, "Bob" to 20),
            gameMode = GameMode.CLASSIC,
            isPlayerWinner = { player -> player == "Alice" },
            timestamp = System.currentTimeMillis(),
            currentPlayerIndex = 0,
            gameType = GameType.LOCAL_MULTIPLAYER,
            diceValues = listOf(1, 2, 3, 4, 5),
            lockedDice = listOf(true, false, false, false, false),
            remainingRolls = 2,
            imageRecognitionMode = false,
            recognizedDiceValues = emptyList(),
            isPlayersTurn = { player -> player == "Alice" }
        )

        fakeRepo.updatePlayerStats(game)

        val aliceStats = fakeRepo.getPlayerStats("Alice")
        val bobStats = fakeRepo.getPlayerStats("Bob")

        assertNotNull(aliceStats)
        assertEquals(1, aliceStats?.totalGamesPlayed)
        assertEquals(1, aliceStats?.totalGamesWon)
        assertEquals(30, aliceStats?.highestScore)
        assertEquals(30.0, aliceStats?.averageScore)

        assertNotNull(bobStats)
        assertEquals(1, bobStats?.totalGamesPlayed)
        assertEquals(0, bobStats?.totalGamesWon)
        assertEquals(20, bobStats?.highestScore)
        assertEquals(20.0, bobStats?.averageScore)
    }


    @Test
    fun `test unlockAchievement adds achievement correctly`() = runTest {
        val playerId = "player1"
        val achievement = Achievement(1L, "First Game Played", "Play your first game", true)

        fakeStatisticsRepository.updatePlayerStats(
            playerId,
            gameWon = true
        ) // This should unlock "First Game Played"
        fakeStatisticsRepository.unlockAchievement(playerId, achievement)

        // Verify the achievement has been unlocked
        val stats = fakeStatisticsRepository.getPlayerStats(playerId).toList().first()
        assertTrue(stats.achievements.contains(achievement))
    }

    @Test
    fun `test leaderboard is updated correctly`() = runTest {
        val player1Id = "player1"
        val player2Id = "player2"

        // Update player1's stats
        fakeStatisticsRepository.updatePlayerStats(player1Id, gameWon = true)
        // Update player2's stats
        fakeStatisticsRepository.updatePlayerStats(player2Id, gameWon = false)

        // Verify the leaderboard
        val leaderboard = fakeStatisticsRepository.getLeaderboard().toList().first()

        // player1 should be at the top of the leaderboard
        assertEquals(player1Id, leaderboard.first().playerId)
        assertEquals(player2Id, leaderboard.last().playerId)
    }

    @Test
    fun `resetPlayerStats should reset stats to default`() = runTest {
        // Arrange: Add some initial stats
        val playerId = "player1"
        val initialStats = PlayerStats(
            playerId = playerId,
            level = "Expert",
            totalGamesPlayed = 100,
            totalGamesWon = 75,
            achievements = setOf(Achievement(1, "Test Achievement", "Description", true)),
            highestScore = 250,
            averageScore = 180.5,
            favoriteGameMode = GameMode.VIRTUAL,
            totalPlayTime = 10000
        )

        // Act: Reset player stats
        fakeStatisticsRepository.resetPlayerStats(playerId)

        // Assert: Verify stats are reset to default values
        val resetStats = fakeStatisticsRepository.getPlayerStats(playerId).first()
        assertEquals("Beginner", resetStats.level)
        assertEquals(0, resetStats.totalGamesPlayed)
        assertEquals(0, resetStats.totalGamesWon)
        assertEquals(emptySet<Achievement>(), resetStats.achievements)
        assertEquals(0, resetStats.highestScore)
        assertEquals(0.0, resetStats.averageScore)
        assertEquals(GameMode.CLASSIC, resetStats.favoriteGameMode)
        assertEquals(0, resetStats.totalPlayTime)
    }
}

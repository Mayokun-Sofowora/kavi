package com.mayor.kavi.data.repository.fakes

import com.mayor.kavi.data.models.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FakeAchievementRepositoryTest {

    private val fakeRepository = FakeAchievementRepository()

    @Test
    fun `test getAllAchievements`() = runBlocking {
        val achievements = fakeRepository.getAllAchievements().toList()
        assertEquals(3, achievements[0].size) // Check that there are 3 achievements
    }

    @Test
    fun `test checkNewAchievements with Perfect Score`() = runBlocking {
        val game = Game(
            gameMode = GameMode.CLASSIC,
            gameType = GameType.LOCAL_MULTIPLAYER,
            players = listOf("Player1", "Player2"),
            score = mapOf("Player1" to 50, "Player2" to 30),
            remainingRolls = 0,
            lockedDice = listOf(),
            timestamp = System.currentTimeMillis(),
            imageRecognitionMode = false,
            diceValues = listOf(1, 2, 3, 4, 5),
            recognizedDiceValues = null,
            currentPlayerIndex = 0,
            isPlayerWinner = {playerName -> true},
            isPlayersTurn = {playerName -> false},
            id = 0
        )

        val unlockedAchievements = fakeRepository.checkNewAchievements(game)
        assertTrue(unlockedAchievements.any { it.name == "Perfect Score" })
    }

    @Test
    fun `test unlockAchievement`() = runBlocking {
        val achievementId = 1L
        fakeRepository.unlockAchievement(achievementId)

        val achievement = fakeRepository.getAchievementById(achievementId)
        assertNotNull(achievement)
        assertTrue(achievement?.isUnlocked == true)
    }

    @Test
    fun `test deleteAchievement`() = runBlocking {
        val achievementId = 1L
        fakeRepository.deleteAchievement(achievementId)

        val achievement = fakeRepository.getAchievementById(achievementId)
        assertNull(achievement) // Check that the achievement is deleted
    }
}

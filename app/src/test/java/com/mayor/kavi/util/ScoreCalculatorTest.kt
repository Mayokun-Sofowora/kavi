package com.mayor.kavi.util

import com.mayor.kavi.ui.viewmodel.GameViewModel.Companion.AI_PLAYER_ID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ScoreCalculatorTest {
    @Test
    fun `test calculateGreedScore with single scoring dice`() = runTest {
        // Test for single 1
        var (score, diceIndices) = ScoreCalculator.calculateGreedScore(listOf(1, 2, 3, 4, 6, 2))
        assertEquals(100, score)
        assertEquals(setOf(0), diceIndices)

        // Test for single 5
        var (score2, diceIndices2) = ScoreCalculator.calculateGreedScore(listOf(2, 3, 4, 5, 6, 2))
        assertEquals(50, score2)
        assertEquals(setOf(3), diceIndices2)
    }

    @Test
    fun `test calculateGreedScore with triple`() = runTest {
        // Test for three 1's
        var (score, diceIndices) = ScoreCalculator.calculateGreedScore(listOf(1, 1, 1, 4, 6, 2))
        assertEquals(1000, score)
        assertEquals(setOf(0, 1, 2), diceIndices)

        // Test for three 2's
        var (score2, diceIndices2) = ScoreCalculator.calculateGreedScore(listOf(2, 2, 2, 4, 6, 3))
        assertEquals(200, score2)
        assertEquals(setOf(0, 1, 2), diceIndices2)
    }

    @Test
    fun `test calculateGreedScore with a straight`() = runTest {
        val (score, diceIndices) = ScoreCalculator.calculateGreedScore(listOf(1, 2, 3, 4, 5, 6))
        assertEquals(1500, score)
        assertEquals(setOf(0, 1, 2, 3, 4, 5), diceIndices)
    }

    @Test
    fun `test calculateGreedScore with six of a kind`() = runTest {
        val (score, diceIndices) = ScoreCalculator.calculateGreedScore(listOf(1, 1, 1, 1, 1, 1))
        assertEquals(3000, score)
        assertEquals(setOf(0, 1, 2, 3, 4, 5), diceIndices)
    }

    @Test
    fun `test calculateGreedScore with five of a kind`() = runTest {
        val (score, diceIndices) = ScoreCalculator.calculateGreedScore(listOf(5, 5, 5, 5, 5, 1))
        assertEquals(2000, score)
        assertEquals(setOf(0, 1, 2, 3, 4, 5), diceIndices)
    }

    @Test
    fun `test calculateGreedScore with three pairs`() = runTest {
        val (score, diceIndices) = ScoreCalculator.calculateGreedScore(listOf(1, 1, 2, 2, 3, 3))
        assertEquals(1500, score)
        assertEquals(setOf(0, 1, 2, 3, 4, 5), diceIndices)
    }

    @Test
    fun `test calculateGreedScore with combination score and singles`() = runTest {
        val (score, diceIndices) = ScoreCalculator.calculateGreedScore(listOf(1, 1, 1, 5, 5, 6))
        assertEquals(1100, score)
        assertEquals(setOf(0, 1, 2, 3, 4), diceIndices)

        val (score2, diceIndices2) = ScoreCalculator.calculateGreedScore(listOf(2, 2, 2, 1, 5, 6))
        assertEquals(350, score2)
        assertEquals(setOf(0, 1, 2, 3, 4), diceIndices2)
    }

    @Test
    fun `test calculateBalutScore with five of a kind`() = runTest {
        val (score, diceIndices) = ScoreCalculator.calculateBalutScore(listOf(4, 4, 4, 4, 4))
        assertEquals(50, score)
        assertEquals(setOf(0, 1, 2, 3, 4), diceIndices)
    }

    @Test
    fun `test calculateBalutScore with four of a kind`() = runTest {
        val (score, diceIndices) = ScoreCalculator.calculateBalutScore(listOf(2, 2, 2, 2, 5))
        assertEquals(40, score)
        assertEquals(setOf(0, 1, 2, 3), diceIndices)
    }


    @Test
    fun `test calculateBalutScore with full house`() = runTest {
        val (score, diceIndices) = ScoreCalculator.calculateBalutScore(listOf(2, 2, 2, 5, 5))
        assertEquals(35, score)
        assertEquals(setOf(0, 1, 2, 3, 4), diceIndices)
    }

    @Test
    fun `test calculateBalutScore with small straight`() = runTest {
        var (score, diceIndices) = ScoreCalculator.calculateBalutScore(listOf(1, 2, 3, 4, 6))
        assertEquals(30, score)
        assertEquals(setOf(0, 1, 2, 3, 4), diceIndices)

        var (score2, diceIndices2) = ScoreCalculator.calculateBalutScore(listOf(2, 3, 4, 5, 2))
        assertEquals(30, score2)
        assertEquals(setOf(0, 1, 2, 3, 4), diceIndices2)

        var (score3, diceIndices3) = ScoreCalculator.calculateBalutScore(listOf(3, 4, 5, 6, 1))
        assertEquals(30, score3)
        assertEquals(setOf(0, 1, 2, 3, 4), diceIndices3)
    }

    @Test
    fun `test calculateBalutScore with large straight`() = runTest {
        var (score, diceIndices) = ScoreCalculator.calculateBalutScore(listOf(1, 2, 3, 4, 5))
        assertEquals(40, score)
        assertEquals(setOf(0, 1, 2, 3, 4), diceIndices)
        var (score2, diceIndices2) = ScoreCalculator.calculateBalutScore(listOf(2, 3, 4, 5, 6))
        assertEquals(40, score2)
        assertEquals(setOf(0, 1, 2, 3, 4), diceIndices2)
    }

    @Test
    fun `test calculateBalutScore with no combination`() = runTest {
        val (score, diceIndices) = ScoreCalculator.calculateBalutScore(listOf(1, 3, 3, 4, 6))
        assertEquals(0, score)
        assertTrue(diceIndices.isEmpty())
    }

    @Test
    fun `test calculateCategoryScore with number categories`() = runTest {
        assertEquals(3, ScoreCalculator.calculateCategoryScore(listOf(1, 1, 3, 4, 1), "Ones"))
        assertEquals(6, ScoreCalculator.calculateCategoryScore(listOf(2, 2, 2, 4, 5), "Twos"))
        assertEquals(9, ScoreCalculator.calculateCategoryScore(listOf(3, 3, 3, 5, 6), "Threes"))
        assertEquals(8, ScoreCalculator.calculateCategoryScore(listOf(4, 4, 2, 3, 6), "Fours"))
        assertEquals(10, ScoreCalculator.calculateCategoryScore(listOf(5, 5, 1, 3, 2), "Fives"))
        assertEquals(12, ScoreCalculator.calculateCategoryScore(listOf(6, 6, 3, 4, 1), "Sixes"))
    }

    @Test
    fun `test calculateCategoryScore with special categories`() = runTest {
        assertEquals(
            0,
            ScoreCalculator.calculateCategoryScore(listOf(1, 2, 3, 4, 5), "Four of a Kind")
        )
        assertEquals(
            40,
            ScoreCalculator.calculateCategoryScore(listOf(1, 1, 1, 1, 5), "Four of a Kind")
        )
        assertEquals(0, ScoreCalculator.calculateCategoryScore(listOf(1, 2, 3, 4, 5), "Full House"))
        assertEquals(
            35,
            ScoreCalculator.calculateCategoryScore(listOf(2, 2, 2, 3, 3), "Full House")
        )
        assertEquals(
            0,
            ScoreCalculator.calculateCategoryScore(listOf(1, 2, 3, 5, 6), "Small Straight")
        )
        assertEquals(
            30,
            ScoreCalculator.calculateCategoryScore(listOf(1, 2, 3, 4, 5), "Small Straight")
        )
        assertEquals(
            30,
            ScoreCalculator.calculateCategoryScore(listOf(2, 3, 4, 5, 1), "Small Straight")
        )
        assertEquals(
            40,
            ScoreCalculator.calculateCategoryScore(listOf(1, 2, 3, 4, 5), "Large Straight")
        )
        assertEquals(
            0,
            ScoreCalculator.calculateCategoryScore(listOf(1, 2, 3, 4, 6), "Large Straight")
        )

        assertEquals(
            0,
            ScoreCalculator.calculateCategoryScore(listOf(1, 2, 3, 4, 6), "Five of a Kind")
        )
        assertEquals(
            50,
            ScoreCalculator.calculateCategoryScore(listOf(1, 1, 1, 1, 1), "Five of a Kind")
        )
        assertEquals(15, ScoreCalculator.calculateCategoryScore(listOf(1, 2, 3, 4, 5), "Choice"))
        assertEquals(18, ScoreCalculator.calculateCategoryScore(listOf(6, 6, 1, 2, 3), "Choice"))
    }
}

class GameMessagesTest {

    @Test
    fun `test buildPigScoreMessage for game over - AI wins`() {
        val message = GameMessages.buildPigScoreMessage(
            die = 0,
            score = 100,
            playerIndex = AI_PLAYER_ID.hashCode(),
            isGameOver = true
        )
        assertEquals("AI wins with 100 points! Better luck next time!", message)
    }

    @Test
    fun `test buildPigScoreMessage for game over - Player wins`() {
        val message = GameMessages.buildPigScoreMessage(
            die = 0,
            score = 100,
            playerIndex = 0,
            isGameOver = true
        )
        assertEquals("You win with 100 points!", message)
    }

    @Test
    fun `test buildGreedScoreMessage for game over - AI wins`() {
        val message = GameMessages.buildGreedScoreMessage(
            dice = emptyList(),
            score = 10000,
            turnScore = 0,
            playerIndex = AI_PLAYER_ID.hashCode(),
            isGameOver = true
        )
        assertEquals("AI wins with 10000 points! Better luck next time!", message)
    }

    @Test
    fun `test buildGreedScoreMessage for game over - Player wins`() {
        val message = GameMessages.buildGreedScoreMessage(
            dice = emptyList(),
            score = 10000,
            turnScore = 0,
            playerIndex = 0,
            isGameOver = true
        )
        assertEquals("You win with 10000 points!", message)
    }

    @Test
    fun `test buildGreedScoreMessage for normal turn with roll`() {
        val message = GameMessages.buildGreedScoreMessage(
            dice = listOf(1, 2, 3, 4, 5),
            score = 500,
            turnScore = 150,
            playerIndex = 0,
            isGameOver = false
        )
        assertEquals("You rolled: 1, 2, 3, 4, 5\nTurn Score: 150", message)
    }

    @Test
    fun `test buildGreedScoreMessage for new player with no score`() {
        val message = GameMessages.buildGreedScoreMessage(
            dice = emptyList(),
            score = 0,
            turnScore = 0,
            playerIndex = 0,
            isGameOver = false
        )
        assertEquals("Your turn\nNeed 800 points to get on board", message)
    }

    @Test
    fun `test buildBalutCategoryMessage for game over - AI wins`() {
        val message = GameMessages.buildBalutCategoryMessage(
            category = "",
            playerIndex = AI_PLAYER_ID.hashCode(),
            isGameOver = true,
            totalScore = 300
        )
        assertEquals("AI wins with 300 points! Better luck next time!", message)
    }

    @Test
    fun `test buildBalutCategoryMessage for normal turn with category`() {
        val message = GameMessages.buildBalutCategoryMessage(
            category = "Full House",
            playerIndex = 0,
            isGameOver = false,
            totalScore = 35
        )
        assertEquals("You - Category: Full House, , Total Score: 35", message)
    }
} 
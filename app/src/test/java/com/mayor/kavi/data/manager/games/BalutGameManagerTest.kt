package com.mayor.kavi.data.manager.games

import com.mayor.kavi.data.manager.StatisticsManager
import com.mayor.kavi.ui.viewmodel.GameViewModel.Companion.AI_PLAYER_ID
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import junit.framework.TestCase.*

class BalutGameManagerTest {

    @Mock
    private lateinit var statisticsManager: StatisticsManager

    private lateinit var balutGameManager: BalutGameManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        balutGameManager = BalutGameManager(statisticsManager)
    }

    @Test
    fun `test game initialization`() {
        val gameState = balutGameManager.initializeGame()

        // Verify initial state
        assertEquals(2, gameState.playerScores.size)
        assertTrue(gameState.playerScores[0]?.isEmpty() == true)
        assertTrue(gameState.playerScores[AI_PLAYER_ID.hashCode()]?.isEmpty() == true)
        assertEquals(1, gameState.currentRound)
        assertEquals(BalutGameManager.MAX_ROLLS, gameState.rollsLeft)
        assertTrue(gameState.heldDice.isEmpty())
        assertFalse(gameState.isGameOver)
    }

    @Test
    fun `test player turn handling`() {
        val initialState = balutGameManager.initializeGame().copy(
            currentPlayerIndex = 0,
            rollsLeft = 3
        )
        val diceResults = listOf(1, 2, 3, 4, 5)
        val heldDice = setOf(0, 1) // Hold first two dice

        val updatedState = balutGameManager.handleTurn(diceResults, initialState, heldDice)

        // Verify turn state
        assertEquals(2, updatedState.rollsLeft)
        assertEquals(heldDice, updatedState.heldDice)
    }

    @Test
    fun `test scoring number category - Ones`() {
        val initialState = balutGameManager.initializeGame().copy(currentPlayerIndex = 0)
        val diceResults = listOf(1, 1, 2, 3, 4) // Two ones

        val updatedState = balutGameManager.scoreCategory(initialState, diceResults, "Ones")

        // Verify scoring
        assertEquals(2, updatedState.playerScores[0]?.get("Ones"))
        assertEquals(AI_PLAYER_ID.hashCode(), updatedState.currentPlayerIndex)
        assertEquals(BalutGameManager.MAX_ROLLS, updatedState.rollsLeft)
    }

    @Test
    fun `test scoring number category - Sixes`() {
        val initialState = balutGameManager.initializeGame().copy(currentPlayerIndex = 0)
        val diceResults = listOf(6, 6, 6, 2, 1) // Three sixes

        val updatedState = balutGameManager.scoreCategory(initialState, diceResults, "Sixes")

        // Verify scoring
        assertEquals(18, updatedState.playerScores[0]?.get("Sixes"))
    }

    @Test
    fun `test scoring five of a kind`() {
        val initialState = balutGameManager.initializeGame().copy(currentPlayerIndex = 0)
        val diceResults = listOf(3, 3, 3, 3, 3) // Five threes

        val updatedState = balutGameManager.scoreCategory(initialState, diceResults, "Five of a Kind")

        // Verify scoring
        assertEquals(50, updatedState.playerScores[0]?.get("Five of a Kind"))
    }

    @Test
    fun `test scoring four of a kind`() {
        val initialState = balutGameManager.initializeGame().copy(currentPlayerIndex = 0)
        val diceResults = listOf(4, 4, 4, 4, 2) // Four fours

        val updatedState = balutGameManager.scoreCategory(initialState, diceResults, "Four of a Kind")

        // Verify scoring
        assertEquals(40, updatedState.playerScores[0]?.get("Four of a Kind"))
    }

    @Test
    fun `test scoring full house`() {
        val initialState = balutGameManager.initializeGame().copy(currentPlayerIndex = 0)
        val diceResults = listOf(2, 2, 2, 3, 3) // Three twos and two threes

        val updatedState = balutGameManager.scoreCategory(initialState, diceResults, "Full House")

        // Verify scoring
        assertEquals(35, updatedState.playerScores[0]?.get("Full House"))
    }

    @Test
    fun `test scoring straight`() {
        val initialState = balutGameManager.initializeGame().copy(currentPlayerIndex = 0)
        val diceResults = listOf(1, 2, 3, 4, 5) // 1-5 straight

        val updatedState = balutGameManager.scoreCategory(initialState, diceResults, "Straight")

        // Verify scoring
        assertEquals(30, updatedState.playerScores[0]?.get("Straight"))
    }

    @Test
    fun `test scoring choice category`() {
        val initialState = balutGameManager.initializeGame().copy(currentPlayerIndex = 0)
        val diceResults = listOf(6, 6, 5, 4, 3) // Sum = 24

        val updatedState = balutGameManager.scoreCategory(initialState, diceResults, "Choice")

        // Verify scoring
        assertEquals(24, updatedState.playerScores[0]?.get("Choice"))
    }

    @Test
    fun `test game completion`() {
        // Create a state where all categories are filled except one
        val allCategories = BalutGameManager.CATEGORIES.associateWith { 10 }.toMutableMap()
        allCategories.remove("Choice") // Leave one category empty
        
        val initialState = balutGameManager.initializeGame().copy(
            currentPlayerIndex = 0,
            playerScores = mapOf(
                0 to allCategories,
                AI_PLAYER_ID.hashCode() to emptyMap()
            )
        )
        val diceResults = listOf(1, 2, 3, 4, 5)

        // Score the last category
        val updatedState = balutGameManager.scoreCategory(initialState, diceResults, "Choice")

        // Verify game completion
        assertTrue(updatedState.isGameOver)
        assertEquals(15, updatedState.playerScores[0]?.get("Choice")) // Sum of 1+2+3+4+5
    }

    @Test
    fun `test no rolls left`() {
        val initialState = balutGameManager.initializeGame().copy(
            currentPlayerIndex = 0,
            rollsLeft = 0
        )
        val diceResults = listOf(1, 2, 3, 4, 5)
        val heldDice = setOf(0, 1)

        val updatedState = balutGameManager.handleTurn(diceResults, initialState, heldDice)

        // Verify state remains unchanged when no rolls left
        assertEquals(initialState, updatedState)
    }

    @Test
    fun `test round advancement`() {
        val initialState = balutGameManager.initializeGame().copy(
            currentPlayerIndex = 0,
            currentRound = 1
        )
        val diceResults = listOf(1, 1, 1, 1, 1)

        // Score a category for both players to advance the round
        val firstPlayerTurn = balutGameManager.scoreCategory(initialState, diceResults, "Ones")
        val secondPlayerTurn = balutGameManager.scoreCategory(firstPlayerTurn, diceResults, "Ones")

        // Verify round advanced
        assertEquals(2, secondPlayerTurn.currentRound)
        assertEquals(0, secondPlayerTurn.currentPlayerIndex)
    }
}
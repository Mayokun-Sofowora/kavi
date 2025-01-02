package com.mayor.kavi.data.manager.games

import com.mayor.kavi.data.manager.StatisticsManager
import com.mayor.kavi.data.models.GameScoreState
import com.mayor.kavi.ui.viewmodel.GameViewModel.Companion.AI_PLAYER_ID
import javax.inject.Inject
import kotlin.math.abs

/**
 * Manager class for the Pig dice game variant.
 *
 * The Pig game is a simple dice game where players take turns rolling a die.
 * On their turn, a player can:
 * - Roll the die and accumulate points
 * - Bank their accumulated points
 * - Lose all accumulated points if they roll a 1
 *
 * The game ends when a player reaches or exceeds [WINNING_SCORE] points.
 *
 * @property statisticsManager Manages game statistics and AI behavior analysis
 */
class PigGameManager @Inject constructor(
    private val statisticsManager: StatisticsManager
) {
    companion object {
        /** Score required to win the game */
        const val WINNING_SCORE = 100

        /** Die value that causes a player to lose their turn and accumulated points */
        const val BUST_VALUE = 1
    }

    private var _turnStartTime: Long = 0

    /**
     * Initializes a new Pig game.
     *
     * Creates a new game state with:
     * - Random starting player selection
     * - Zero scores for both players
     * - Game timer initialization
     *
     * @return A new [GameScoreState.PigScoreState] with initial game settings
     */
    fun initializeGame(): GameScoreState.PigScoreState {
        // Start tracking game time when a new game starts
        statisticsManager.startGameTimer()

        val startingPlayer = if (Math.random() < 0.5) 0 else AI_PLAYER_ID.hashCode()
        return GameScoreState.PigScoreState(
            playerScores = mutableMapOf(
                0 to 0,
                AI_PLAYER_ID.hashCode() to 0
            ),
            currentPlayerIndex = startingPlayer,
            isGameOver = false,
            currentTurnScore = 0
        )
    }

    /**
     * Handles a player's turn in the game.
     *
     * Processes the dice roll result and updates the game state accordingly:
     * - For player turns: Updates score or switches turn on bust
     * - For AI turns: Makes strategic decisions about continuing or banking
     *
     * @param currentState Current state of the game
     * @param diceResult Result of the die roll (null if no roll has occurred)
     * @return Updated game state after processing the turn
     */
    fun handleTurn(
        currentState: GameScoreState.PigScoreState,
        diceResult: Int? = null
    ): GameScoreState.PigScoreState {
        // Start tracking turn time
        statisticsManager.startTurnTimer()

        return when (currentState.currentPlayerIndex) {
            AI_PLAYER_ID.hashCode() -> handleAITurn(currentState, diceResult)
            else -> handlePlayerTurn(currentState, diceResult)
        }
    }

    private fun handlePlayerTurn(
        currentState: GameScoreState.PigScoreState,
        diceResult: Int?
    ): GameScoreState.PigScoreState {
        if (diceResult != null) {
            // Record the roll for analytics
            statisticsManager.recordRoll()

            if (diceResult == BUST_VALUE) {
                // Record decision time and banking (with 0 score due to bust)
                statisticsManager.recordDecision(System.currentTimeMillis() - _turnStartTime)
                statisticsManager.recordBanking(0)
                return switchTurn(currentState)
            }
            // Continue turn with accumulated score
            return currentState.copy(
                currentTurnScore = currentState.currentTurnScore + diceResult
            )
        }
        return currentState
    }

    private fun handleAITurn(
        currentState: GameScoreState.PigScoreState,
        diceResult: Int?
    ): GameScoreState.PigScoreState {
        if (diceResult == null) return currentState
        return if (diceResult == BUST_VALUE) {
            // AI rolled a 1, lose turn and points
            currentState.copy(
                currentTurnScore = 0,
                currentPlayerIndex = 0
            )
        } else {
            // Add to current turn score
            val newTurnScore = currentState.currentTurnScore + diceResult
            val updatedState = currentState.copy(
                currentTurnScore = newTurnScore
            )
            // AI decides whether to bank or continue
            if (shouldAIBank(
                    currentTurnScore = newTurnScore,
                    aiTotalScore = updatedState.playerScores[AI_PLAYER_ID.hashCode()] ?: 0,
                    playerTotalScore = updatedState.playerScores[0] ?: 0
                )
            ) {
                bankScore(updatedState)
            } else {
                updatedState
            }
        }
    }

    /**
     * Banks the current turn's score for the active player.
     *
     * This method:
     * 1. Adds accumulated turn score to player's total
     * 2. Records banking statistics
     * 3. Checks for game completion
     * 4. Updates game state and switches turns if game continues
     *
     * @param currentState Current state of the game
     * @return Updated game state after banking the score
     */
    fun bankScore(currentState: GameScoreState.PigScoreState): GameScoreState.PigScoreState {
        // Record banking decision and score
        statisticsManager.recordDecision(System.currentTimeMillis() - _turnStartTime)
        statisticsManager.recordBanking(currentState.currentTurnScore)

        val currentPlayerScore = currentState.playerScores[currentState.currentPlayerIndex] ?: 0
        val newScore = currentPlayerScore + currentState.currentTurnScore

        val updatedState = currentState.copy(
            playerScores = currentState.playerScores.toMutableMap().apply {
                put(currentState.currentPlayerIndex, newScore)
            },
            currentTurnScore = 0
        )

        // Check if game is over and update statistics
        if (newScore >= WINNING_SCORE) {
            val isPlayerWin = currentState.currentPlayerIndex != AI_PLAYER_ID.hashCode()
            val wasComeback = checkIfComeback(currentState)
            val wasCloseGame = checkIfCloseGame(updatedState)

            statisticsManager.updateGameStatistics(
                gameMode = "PIG",
                score = newScore,
                isWin = isPlayerWin,
                wasComeback = wasComeback,
                wasCloseGame = wasCloseGame
            )

            return updatedState.copy(isGameOver = true)
        }

        return switchTurn(updatedState)
    }

    private fun checkIfComeback(state: GameScoreState.PigScoreState): Boolean {
        val playerScore = state.playerScores[0] ?: 0
        val aiScore = state.playerScores[AI_PLAYER_ID.hashCode()] ?: 0
        // Consider it a comeback if player was behind by 30+ points
        return state.currentPlayerIndex == 0 && (aiScore - playerScore) >= 30
    }

    private fun checkIfCloseGame(state: GameScoreState.PigScoreState): Boolean {
        val scores = state.playerScores.values.toList()
        if (scores.size != 2) return false
        // Consider it a close game if the difference is 10 points or less
        return abs(scores[0] - scores[1]) <= 10
    }

    private fun switchTurn(state: GameScoreState.PigScoreState): GameScoreState.PigScoreState {
        _turnStartTime = System.currentTimeMillis() // Reset turn timer
        return state.copy(
            currentPlayerIndex = if (state.currentPlayerIndex == 0)
                AI_PLAYER_ID.hashCode()
            else
                0,
            currentTurnScore = 0
        )
    }

    private fun shouldAIBank(
        currentTurnScore: Int,
        aiTotalScore: Int,
        playerTotalScore: Int
    ): Boolean {
        // Use the AI logic from StatisticsManager
        return statisticsManager.shouldAIBank(currentTurnScore, aiTotalScore, playerTotalScore)
    }
}

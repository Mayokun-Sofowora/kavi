package com.mayor.kavi.data.manager.games

import com.mayor.kavi.data.models.*
import com.mayor.kavi.data.manager.StatisticsManager
import com.mayor.kavi.data.models.GameScoreState.PigScoreState
import com.mayor.kavi.ui.viewmodel.GameViewModel.Companion.AI_PLAYER_ID
import com.mayor.kavi.util.GameMessages
import javax.inject.Inject

class PigGameManager @Inject constructor(
    private val statisticsManager: StatisticsManager
) {
    companion object {
        const val WINNING_SCORE = 100
        const val BUST_VALUE = 1
    }

    fun initializeGame(): PigScoreState {
        val startingPlayer = if (Math.random() < 0.5) 0 else AI_PLAYER_ID.hashCode()
        return PigScoreState(
            playerScores = mutableMapOf(
                0 to 0,
                AI_PLAYER_ID.hashCode() to 0
            ),
            currentPlayerIndex = startingPlayer,
            message = if (startingPlayer == 0) "You go first!" else "AI goes first!"
        )
    }

    fun handleTurn(
        currentState: PigScoreState,
        diceResult: Int? = null
    ): PigScoreState {
        return when (currentState.currentPlayerIndex) {
            0 -> handlePlayerTurn(currentState, diceResult)
            AI_PLAYER_ID.hashCode() -> handleAITurn(currentState, diceResult)
            else -> currentState
        }
    }

    private fun handlePlayerTurn(currentState: PigScoreState, diceResult: Int?): PigScoreState {
        if (diceResult == null) return currentState

        return if (diceResult == BUST_VALUE) {
            // Player rolled a 1, lose turn and all accumulated points
            currentState.copy(
                currentTurnScore = 0,
                currentPlayerIndex = AI_PLAYER_ID.hashCode(),
                message = GameMessages.buildPigScoreMessage(
                    diceResult,
                    0,
                    currentState.currentPlayerIndex
                )
            )
        } else {
            // Add to current turn score
            val newTurnScore = currentState.currentTurnScore + diceResult
            currentState.copy(
                currentTurnScore = newTurnScore,
                message = GameMessages.buildPigScoreMessage(
                    diceResult,
                    newTurnScore,
                    currentState.currentPlayerIndex
                )
            )
        }
    }

    fun handleAITurn(currentState: PigScoreState, diceResult: Int?): PigScoreState {
        if (diceResult == null) return currentState
        return if (diceResult == BUST_VALUE) {
            // AI rolled a 1, lose turn and points
            currentState.copy(
                currentTurnScore = 0,
                currentPlayerIndex = 0,
                message = GameMessages.buildPigScoreMessage(diceResult, 0, AI_PLAYER_ID.hashCode())
            )
        } else {
            // Add to current turn score
            val newTurnScore = currentState.currentTurnScore + diceResult
            val updatedState = currentState.copy(
                currentTurnScore = newTurnScore,
                message = GameMessages.buildPigScoreMessage(
                    diceResult,
                    newTurnScore,
                    AI_PLAYER_ID.hashCode()
                )
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

    private fun shouldAIBank(
        currentTurnScore: Int,
        aiTotalScore: Int,
        playerTotalScore: Int
    ): Boolean {
        // If AI can win by banking, always bank
        if (aiTotalScore + currentTurnScore >= WINNING_SCORE) return true

        // Get player's stats and style
        val playerAnalysis = statisticsManager.playerAnalysis.value
        val playerWinRate = playerAnalysis?.predictedWinRate ?: 0.5f
        val playerConsistency = playerAnalysis?.consistency ?: 0.5f
        val playerStyle = playerAnalysis?.playStyle ?: PlayStyle.BALANCED

        // Base minimum score varies based on player's style and stats
        val baseMinScore = when (playerStyle) {
            PlayStyle.AGGRESSIVE -> if (playerWinRate > 0.6f) 25 else 22 // Against aggressive players, be more conservative
            PlayStyle.CAUTIOUS -> if (playerWinRate < 0.4f) 15 else 18 // Against cautious players, be more aggressive
            else ->
                // Against balanced players, adapt based on their win rate
                when {
                    playerWinRate > 0.6f -> 22  // They're good, be careful
                    playerWinRate < 0.4f -> 18  // They're struggling, be aggressive
                    else -> 20                  // Standard play
                }
        }
        // Adjust based on game situation and player consistency
        val situationalAdjustment = when {
            playerTotalScore >= 75 -> if (playerConsistency > 0.7f) -8 else -5  // More aggressive if they're consistent
            aiTotalScore >= 75 -> when (playerStyle) {
                PlayStyle.AGGRESSIVE -> +7  // Players might take big risks
                PlayStyle.CAUTIOUS -> +3    // Players might play it safe
                else -> +5
            }

            playerTotalScore > aiTotalScore + 20 -> if (playerWinRate > 0.5f) -10 else -7 // Based on player's win rate
            else -> 0
        }
        val finalMinScore = (baseMinScore + situationalAdjustment).coerceIn(12, 28)
        // Add randomness based on player consistency
        val randomRange = if (playerConsistency > 0.7f) (-1..1) else (-2..2)
        return currentTurnScore >= finalMinScore + randomRange.random()
    }

    fun bankScore(currentState: PigScoreState): PigScoreState {
        val newTotalScore = (currentState.playerScores[currentState.currentPlayerIndex] ?: 0) +
                currentState.currentTurnScore
        val updatedPlayerScores = currentState.playerScores.toMutableMap().apply {
            this[currentState.currentPlayerIndex] = newTotalScore
        }

        return currentState.copy(
            playerScores = updatedPlayerScores,
            currentTurnScore = 0,
            isGameOver = newTotalScore >= WINNING_SCORE,
            currentPlayerIndex = if (currentState.currentPlayerIndex == 0) AI_PLAYER_ID.hashCode() else 0,
            message = if (newTotalScore >= WINNING_SCORE)
                if (currentState.currentPlayerIndex == 0) "You win with $newTotalScore points!"
                else "AI wins with $newTotalScore points!"
            else
                if (currentState.currentPlayerIndex == 0) "Banked ${currentState.currentTurnScore} points. AI's turn!"
                else "AI banks ${currentState.currentTurnScore} points. Your turn!"
        )
    }
}

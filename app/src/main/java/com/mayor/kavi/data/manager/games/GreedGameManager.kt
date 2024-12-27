package com.mayor.kavi.data.manager.games

import com.mayor.kavi.data.manager.StatisticsManager
import com.mayor.kavi.data.models.GameScoreState.GreedScoreState
import com.mayor.kavi.data.models.PlayStyle
import com.mayor.kavi.ui.viewmodel.GameViewModel.Companion.AI_PLAYER_ID
import com.mayor.kavi.util.GameMessages
import com.mayor.kavi.util.ScoreCalculator
import javax.inject.Inject

class GreedGameManager @Inject constructor(
    private val statisticsManager: StatisticsManager
) {
    companion object {
        const val WINNING_SCORE = 10000
        const val MINIMUM_STARTING_SCORE = 800
    }

    fun initializeGame(): GreedScoreState {
        val startingPlayer = if (Math.random() < 0.5) 0 else AI_PLAYER_ID.hashCode()
        return GreedScoreState(
            playerScores = mutableMapOf(
                0 to 0,
                AI_PLAYER_ID.hashCode() to 0
            ),
            currentPlayerIndex = startingPlayer,
            message = if (startingPlayer == 0) "You go first!" else "AI goes first!",
            roundHistory = mutableMapOf(
                0 to emptyList(),
                AI_PLAYER_ID.hashCode() to emptyList()
            )
        )
    }

    private fun isPlayerOnBoard(roundHistory: Map<Int, List<Int>>, playerIndex: Int): Boolean {
        return (roundHistory[playerIndex] ?: emptyList()).any { it >= MINIMUM_STARTING_SCORE }
    }

    fun handleTurn(
        diceResult: List<Int>,
        currentState: GreedScoreState
    ): GreedScoreState {
        return when (currentState.currentPlayerIndex) {
            0 -> handlePlayerTurn(diceResult, currentState)
            AI_PLAYER_ID.hashCode() -> handleAITurn(currentState, diceResult)
            else -> currentState
        }
    }

    private fun handlePlayerTurn(
        results: List<Int>,
        currentState: GreedScoreState
    ): GreedScoreState {
        // Keep currently held dice values
        val currentHeldDice = currentState.scoringDice
        val (score, newScoringDice) = ScoreCalculator.calculateGreedScore(results)

        if (score == 0) {
            // Player busts, lose turn and ALL accumulated points for this turn
            return currentState.copy(
                turnScore = 0,
                canReroll = false,
                scoringDice = emptySet(),
                currentPlayerIndex = AI_PLAYER_ID.hashCode(),
                message = GameMessages.buildGreedScoreMessage(
                    results,
                    score,
                    0,
                    currentState.currentPlayerIndex,
                    currentState.roundHistory
                )
            )
        }

        // If all dice are scoring dice, player must roll all six again
        val mustReroll = newScoringDice.size == results.size

        // Calculate new turn score
        val newTurnScore = currentState.turnScore + score

        // Handle remaining dice logic
        val remainingDiceCount = if (mustReroll) {
            6  // All dice can be rerolled
        } else {
            6 - (currentHeldDice.size + newScoringDice.size)  // Only non-scoring dice can be rerolled
        }

        // Determine if player can reroll
        val canReroll = when {
            mustReroll -> true  // Must reroll all dice
            remainingDiceCount == 0 -> false  // No dice left to roll
            newScoringDice.isEmpty() -> false  // No scoring dice in this roll
            else -> true  // Has remaining dice and scored this roll
        }

        // Combine scoring dice unless must reroll all
        val combinedScoringDice = if (mustReroll) {
            emptySet()
        } else {
            (currentHeldDice + newScoringDice)
        }

        return currentState.copy(
            turnScore = newTurnScore,
            scoringDice = combinedScoringDice,
            message = GameMessages.buildGreedScoreMessage(
                results,
                score,
                newTurnScore,
                currentState.currentPlayerIndex,
                currentState.roundHistory
            ),
            canReroll = canReroll
        )
    }

    private fun handleAITurn(
        currentState: GreedScoreState,
        diceResult: List<Int>?
    ): GreedScoreState {
        if (diceResult == null) return currentState
        val currentHeldDice = currentState.scoringDice
        val (score, newScoringDice) = ScoreCalculator.calculateGreedScore(diceResult)

        if (score == 0) {
            // AI busts
            return currentState.copy(
                turnScore = 0,
                canReroll = false,
                scoringDice = emptySet(),
                currentPlayerIndex = 0,
                message = GameMessages.buildGreedScoreMessage(
                    diceResult,
                    score,
                    0,
                    currentState.currentPlayerIndex,
                    currentState.roundHistory
                )
            )
        }

        // If all dice are scoring dice, AI must roll all six again
        val mustReroll = newScoringDice.size == 6

        // Update current turn score
        val newTurnScore = currentState.turnScore + score

        // AI strategic dice holding
        val diceToHold = if (!mustReroll) {
            val potentialNewHolds = newScoringDice.toMutableSet()

            // Keep currently held dice and add new scoring dice
            val combinedHolds = (currentHeldDice + potentialNewHolds).filter { index ->
                val dieValue = diceResult[index]
                when (// Always hold 1s and 5s as they're guaranteed points
                    dieValue) {
                    1 -> true
                    5 -> true
                    // For other values, check if they're part of a set
                    else -> {
                        val valueCount = diceResult.count { it == dieValue }
                        valueCount >= 3 // Hold if part of three of a kind or better
                    }
                }
            }.toSet()
            // If no scoring dice are found, must hold at least one
            if (combinedHolds.isEmpty() && potentialNewHolds.isNotEmpty()) {
                setOf(potentialNewHolds.first())
            } else {
                combinedHolds
            }
        } else emptySet()

        val newState = currentState.copy(
            turnScore = newTurnScore,
            scoringDice = diceToHold,
            message = GameMessages.buildGreedScoreMessage(
                diceResult,
                score,
                newTurnScore,
                currentState.currentPlayerIndex,
                currentState.roundHistory
            ),
            canReroll = mustReroll || diceToHold.isNotEmpty()
        )

        // AI decision making for banking
        if (!mustReroll && shouldAIBank(
                currentTurnScore = newTurnScore,
                aiTotalScore = newState.playerScores[AI_PLAYER_ID.hashCode()] ?: 0,
                playerTotalScore = newState.playerScores[0] ?: 0,
                scoringDiceCount = diceToHold.size,
                isOnBoard = isPlayerOnBoard(currentState.roundHistory, AI_PLAYER_ID.hashCode())
            )
        ) {
            // Bank points and end turn
            return bankScore(newState)
        }

        return newState
    }

    fun bankScore(currentState: GreedScoreState): GreedScoreState {
        // Can't bank if not on board and score is less than minimum
        if (!isPlayerOnBoard(currentState.roundHistory, currentState.currentPlayerIndex) &&
            currentState.turnScore < MINIMUM_STARTING_SCORE
        ) {
            return currentState.copy(
                message = "Need at least $MINIMUM_STARTING_SCORE points to get on board!"
            )
        }

        val newTotalScore = (currentState.playerScores[currentState.currentPlayerIndex] ?: 0) +
                currentState.turnScore

        val updatedPlayerScores = currentState.playerScores.toMutableMap().apply {
            this[currentState.currentPlayerIndex] = newTotalScore
        }

        val updatedHistory = currentState.roundHistory.toMutableMap().apply {
            this[currentState.currentPlayerIndex] =
                (this[currentState.currentPlayerIndex] ?: emptyList()) +
                        listOf(currentState.turnScore)
        }

        // Check if game is over
        val isGameOver = if (newTotalScore >= WINNING_SCORE) {
            // If current player reaches winning score, check if they have the highest score
            // and if other players have had their final turn
            val currentPlayerRounds = updatedHistory[currentState.currentPlayerIndex]?.size ?: 0
            val otherPlayersHadFinalTurn = currentState.playerScores.keys.all { playerId ->
                if (playerId == currentState.currentPlayerIndex) true
                else (updatedHistory[playerId]?.size ?: 0) >= currentPlayerRounds
            }
            val hasHighestScore = updatedPlayerScores.all { (playerId, score) ->
                playerId == currentState.currentPlayerIndex || score <= newTotalScore
            }
            otherPlayersHadFinalTurn && hasHighestScore
        } else false

        // Determine next player
        val nextPlayer = if (currentState.currentPlayerIndex == 0) AI_PLAYER_ID.hashCode() else 0

        return currentState.copy(
            playerScores = updatedPlayerScores,
            turnScore = 0,
            scoringDice = emptySet(),
            canReroll = true,
            isGameOver = isGameOver,
            currentPlayerIndex = if (isGameOver) currentState.currentPlayerIndex else nextPlayer,
            roundHistory = updatedHistory,
            message = buildString {
                if (isGameOver) {
                    // Find the winner based on highest score
                    append(
                        if (currentState.currentPlayerIndex == 0)
                            "Congratulations! You win with $newTotalScore points!"
                        else
                            "AI wins with $newTotalScore points!"
                    )
                } else {
                    val currentPlayerName =
                        if (currentState.currentPlayerIndex == 0) "You" else "AI"
                    append("$currentPlayerName banked ${currentState.turnScore} points. ")

                    // Check if this bank got them on the board
                    if (currentState.turnScore >= MINIMUM_STARTING_SCORE &&
                        !isPlayerOnBoard(currentState.roundHistory, currentState.currentPlayerIndex)
                    ) {
                        append("$currentPlayerName got on the board! ")
                    }

                    // If someone reached winning score but game isn't over, announce final round
                    if (newTotalScore >= WINNING_SCORE) {
                        append("Final round! ")
                    }

                    // Announce next turn
                    val nextPlayerName = if (nextPlayer == 0) "Your" else "AI's"
                    append("$nextPlayerName turn!")
                }
            }
        )
    }

    private fun shouldAIBank(
        currentTurnScore: Int,
        aiTotalScore: Int,
        playerTotalScore: Int,
        scoringDiceCount: Int,
        isOnBoard: Boolean
    ): Boolean {
        // If not on board, must get minimum score
        if (!isOnBoard && currentTurnScore < MINIMUM_STARTING_SCORE) return false

        // If AI can win by banking, always bank
        if (aiTotalScore + currentTurnScore >= WINNING_SCORE) return true

        // If already on board and few dice left with a decent score, consider banking
        if (isOnBoard && scoringDiceCount <= 2) {
            // More likely to bank with fewer dice and higher scores
            val threshold = when (scoringDiceCount) {
                1 -> 300  // Bank more readily with just one scoring die
                2 -> 400  // A bit more aggressive with two scoring dice
                else -> 500
            }
            if (currentTurnScore >= threshold) return true
        }

        // Get player's stats and style
        val playerAnalysis = statisticsManager.playerAnalysis.value
        val playerWinRate = playerAnalysis?.predictedWinRate ?: 0.5f
        val playerConsistency = playerAnalysis?.consistency ?: 0.5f
        val playerStyle = playerAnalysis?.playStyle ?: PlayStyle.BALANCED

        // Base minimum score varies based on player's style and stats
        val baseMinScore = when {
            !isOnBoard -> MINIMUM_STARTING_SCORE + 100 // Be a bit more aggressive when getting on board
            playerStyle == PlayStyle.AGGRESSIVE -> if (playerWinRate > 0.6f) 1000 else 800
            playerStyle == PlayStyle.CAUTIOUS -> if (playerWinRate < 0.4f) 600 else 700
            else -> when {
                playerWinRate > 0.6f -> 900  // They're good, be careful
                playerWinRate < 0.4f -> 700  // They're struggling, be aggressive
                else -> 800                  // Standard play
            }
        }

        // Adjust based on game situation and player consistency
        val situationalAdjustment = when {
            playerTotalScore >= 8000 -> if (playerConsistency > 0.7f) -200 else -150
            aiTotalScore >= 8000 -> when (playerStyle) {
                PlayStyle.AGGRESSIVE -> +200
                PlayStyle.CAUTIOUS -> +100
                else -> +150
            }

            scoringDiceCount <= 2 -> +150    // Bank earlier with few scoring dice
            scoringDiceCount >= 5 -> -150    // More aggressive with many scoring dice
            playerTotalScore > aiTotalScore + 2000 -> if (playerWinRate > 0.5f) -300 else -200
            else -> 0
        }

        val finalMinScore = if (isOnBoard) {
            (baseMinScore + situationalAdjustment).coerceIn(400, 1200)
        } else {
            MINIMUM_STARTING_SCORE
        }

        // Add randomness based on player consistency
        val randomRange = if (playerConsistency > 0.7f) (-50..50) else (-100..100)
        return currentTurnScore >= finalMinScore + randomRange.random()
    }
}
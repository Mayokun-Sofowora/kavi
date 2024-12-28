package com.mayor.kavi.data.manager.games

import com.mayor.kavi.data.manager.StatisticsManager
import com.mayor.kavi.data.models.GameScoreState.GreedScoreState
import com.mayor.kavi.data.models.PlayStyle
import com.mayor.kavi.ui.viewmodel.GameViewModel.Companion.AI_PLAYER_ID
import com.mayor.kavi.util.GameMessages.buildGreedScoreMessage
import com.mayor.kavi.util.ScoreCalculator
import javax.inject.Inject
import kotlin.random.Random

class GreedGameManager @Inject constructor(
    private val statisticsManager: StatisticsManager
) {
    companion object {
        const val WINNING_SCORE = 10000
        const val MINIMUM_STARTING_SCORE = 800
    }

    fun initializeGame(): GreedScoreState {
        val startingPlayer = if (Random.nextBoolean()) 0 else AI_PLAYER_ID.hashCode()
        return GreedScoreState(
            playerScores = mapOf(
                0 to 0,
                AI_PLAYER_ID.hashCode() to 0
            ),
            currentPlayerIndex = startingPlayer,
            message = if (startingPlayer == 0) "You go first!" else "AI goes first!",
            isGameOver = false,
            turnScore = 0,
            canReroll = true,
            lastRoll = emptyList()
        )
    }

    fun handleTurn(
        diceResults: List<Int>, currentState: GreedScoreState, heldDice: Set<Int>
    ): GreedScoreState {
        return when (currentState.currentPlayerIndex) {
            0 -> handlePlayerTurn(currentState, diceResults, heldDice)
            AI_PLAYER_ID.hashCode() -> handleAITurn(diceResults, currentState)
            else -> currentState
        }
    }

    private fun handlePlayerTurn(
        currentState: GreedScoreState, diceResults: List<Int>, heldDice: Set<Int>
    ): GreedScoreState {
        if (!currentState.canReroll) {
            return currentState.copy(message = "You can't reroll after banking your score.")
        }

        // If all dice are held, player must bank or risk losing points
        if (heldDice.size == diceResults.size) {
            return currentState.copy(
                message = "All dice are held. You must bank your score or risk losing it!",
                canReroll = false,
                heldDice = heldDice  // Preserve held dice so UI can show them
            )
        }

        // If previous roll was hot dice and player didn't reroll all dice, they bust
        if (currentState.scoringDice.isEmpty() && currentState.heldDice.isNotEmpty()) {
            return currentState.copy(
                turnScore = 0,
                message = "Reroll all dice when you get hot dice! You lose all points for this turn.",
                heldDice = emptySet(),
                scoringDice = emptySet(),
                lastRoll = diceResults,
                canReroll = false
            )
        }

        // Calculate score only for non-held dice
        val availableDice = diceResults.indices.toSet() - currentState.heldDice - currentState.scoringDice
        if (availableDice.isEmpty()) {
            return currentState.copy(
                message = "No dice available to roll. Bank your score or risk losing it!",
                canReroll = false,
                heldDice = heldDice  // Preserve held dice so UI can show them
            )
        }

        val newDiceResults = availableDice.map { diceResults[it] }
        val (score, scoringDice) = ScoreCalculator.calculateGreedScore(newDiceResults)

        // If no scoring dice in new roll, lose all accumulated points
        if (scoringDice.isEmpty()) {
            return currentState.copy(
                turnScore = 0,
                message = "Bust! You lost all accumulated points for this turn.",
                heldDice = emptySet(),
                scoringDice = emptySet(),
                lastRoll = diceResults,
                canReroll = false
            )
        }

        val newTurnScore = currentState.turnScore + score
        val message = buildGreedScoreMessage(
            dice = diceResults,
            score = score,
            turnScore = newTurnScore,
            playerIndex = currentState.currentPlayerIndex,
            roundHistory = currentState.roundHistory
        )

        // Map scoring dice to original indices
        val newScoringDiceIndices = scoringDice.map { availableDice.elementAt(it) }.toSet()

        // If all dice are now scoring, force player to reroll all dice (hot dice)
        val allDiceScoring = (currentState.scoringDice + newScoringDiceIndices).size == diceResults.size
        val finalHeldDice = if (allDiceScoring) emptySet() else heldDice
        val finalScoringDice = if (allDiceScoring) emptySet() else (currentState.scoringDice + newScoringDiceIndices)

        // Check if all dice will be held after this roll
        val allDiceWillBeHeld = finalHeldDice.size == diceResults.size

        return currentState.copy(
            heldDice = finalHeldDice,
            message = when {
                allDiceScoring -> "$message\nHot dice! Reroll all dice!"
                allDiceWillBeHeld -> "$message\nAll dice held. Bank your score or risk losing it!"
                else -> message
            },
            turnScore = newTurnScore,
            lastRoll = diceResults,
            scoringDice = finalScoringDice,
            canReroll = !allDiceWillBeHeld  // Disable reroll if all dice will be held
        )
    }

    private fun handleAITurn(
        diceResults: List<Int>, currentState: GreedScoreState
    ): GreedScoreState {
        if (!currentState.canReroll) {
            return bankScore(currentState)
        }

        val availableDice = diceResults.indices.toSet() - currentState.heldDice - currentState.scoringDice
        if (availableDice.isEmpty()) {
            return bankScore(currentState)
        }

        val newDiceResults = availableDice.map { diceResults[it] }
        val (score, scoringDice) = ScoreCalculator.calculateGreedScore(newDiceResults)

        // If no scoring dice in new roll, lose all accumulated points
        if (scoringDice.isEmpty()) {
            return currentState.copy(
                turnScore = 0,
                message = "AI busts! Lost all accumulated points for this turn. Your turn!",
                heldDice = emptySet(),
                scoringDice = emptySet(),
                lastRoll = diceResults,
                canReroll = true,
                currentPlayerIndex = 0
            )
        }

        val newTurnScore = currentState.turnScore + score
        val newScoringDiceIndices = scoringDice.map { availableDice.elementAt(it) }.toSet()
        
        // Check for hot dice (all dice scoring)
        val allDiceScoring = (currentState.scoringDice + newScoringDiceIndices).size == diceResults.size
        val finalHeldDice = if (allDiceScoring) emptySet() else decideAIDiceHolds(newScoringDiceIndices)
        val finalScoringDice = if (allDiceScoring) emptySet() else (currentState.scoringDice + newScoringDiceIndices)

        // AI decision to bank or continue
        val shouldBank = shouldAIBank(newTurnScore, currentState.playerScores[AI_PLAYER_ID.hashCode()] ?: 0)
        if (shouldBank && !allDiceScoring) {
            return bankScore(currentState.copy(turnScore = newTurnScore))
        }

        val message = buildGreedScoreMessage(
            dice = diceResults,
            score = score,
            turnScore = newTurnScore,
            playerIndex = currentState.currentPlayerIndex,
            roundHistory = currentState.roundHistory
        ) + if (allDiceScoring) "\nAI got hot dice! Re-rolling all dice!" else ""

        return currentState.copy(
            heldDice = finalHeldDice,
            message = message,
            turnScore = newTurnScore,
            lastRoll = diceResults,
            scoringDice = finalScoringDice,
            canReroll = true
        )
    }

    private fun shouldAIBank(currentTurnScore: Int, aiTotalScore: Int): Boolean {
        // If AI can win by banking, always bank
        if (aiTotalScore + currentTurnScore >= WINNING_SCORE) return true

        if (aiTotalScore == 0 && currentTurnScore < MINIMUM_STARTING_SCORE) {
            return false // Keep rolling until we get at least minimum score
        }

        val playerAnalysis = statisticsManager.playerAnalysis.value
        val baseRiskThreshold = when (playerAnalysis?.playStyle) {
            PlayStyle.AGGRESSIVE -> 0.7  // More likely to roll
            PlayStyle.CAUTIOUS -> 0.4   // More likely to bank
            else -> 0.55
        }

        // Increase risk threshold based on turn score and whether we've reached minimum
        val scoreMultiplier = if (aiTotalScore == 0) {
            (currentTurnScore.toFloat() / MINIMUM_STARTING_SCORE).coerceAtMost(1.5f)
        } else {
            (currentTurnScore.toFloat() / MINIMUM_STARTING_SCORE).coerceAtMost(2f)
        }
        val adjustedThreshold = (baseRiskThreshold * scoreMultiplier).coerceAtMost(0.9)

        return Random.nextDouble() > adjustedThreshold
    }

    fun bankScore(currentState: GreedScoreState): GreedScoreState {
        val currentScore = currentState.playerScores[currentState.currentPlayerIndex] ?: 0
        val canBank = currentState.turnScore >= MINIMUM_STARTING_SCORE || currentScore > 0

        val updatedScores = currentState.playerScores.toMutableMap()
        if (canBank) {
            updatedScores[currentState.currentPlayerIndex] = currentScore + currentState.turnScore
        }

        val nextPlayer = if (currentState.currentPlayerIndex == 0) AI_PLAYER_ID.hashCode() else 0
        val isGameOver = checkIfGameOver(updatedScores)

        val message = if (isGameOver) {
            if (currentState.currentPlayerIndex == 0) "You win with ${updatedScores[currentState.currentPlayerIndex]} points!"
            else "AI wins with ${updatedScores[currentState.currentPlayerIndex]} points!"
        } else if (!canBank) {
            "Need at least $MINIMUM_STARTING_SCORE points to start banking."
        } else {
            if (currentState.currentPlayerIndex == 0) "Banked ${currentState.turnScore} points. AI's turn!"
            else "AI banks ${currentState.turnScore} points. Your turn!"
        }

        return currentState.copy(
            playerScores = updatedScores,
            currentPlayerIndex = nextPlayer,
            message = message,
            isGameOver = isGameOver,
            turnScore = 0,
            heldDice = emptySet(),
            scoringDice = emptySet(),
            canReroll = true
        )
    }

    private fun checkIfGameOver(scores: Map<Int, Int>): Boolean {
        return scores.values.any { it >= WINNING_SCORE }
    }

    private fun decideAIDiceHolds(scoringDice: Set<Int>): Set<Int> {
        val playerAnalysis = statisticsManager.playerAnalysis.value
        val aiAggressiveness = when (playerAnalysis?.playStyle) {
            PlayStyle.AGGRESSIVE -> 0.8
            PlayStyle.CAUTIOUS -> 0.5
            else -> 0.6
        }

        return if (Random.nextDouble() < aiAggressiveness) {
            scoringDice
        } else {
            emptySet()
        }
    }
}

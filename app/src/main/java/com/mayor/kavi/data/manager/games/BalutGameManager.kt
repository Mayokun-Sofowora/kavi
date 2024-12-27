package com.mayor.kavi.data.manager.games

import com.mayor.kavi.data.manager.StatisticsManager
import com.mayor.kavi.data.models.GameScoreState.BalutScoreState
import com.mayor.kavi.data.models.PlayStyle
import com.mayor.kavi.ui.viewmodel.GameViewModel.Companion.AI_PLAYER_ID
import com.mayor.kavi.util.GameMessages
import com.mayor.kavi.util.ScoreCalculator
import javax.inject.Inject
import kotlin.random.Random

class BalutGameManager @Inject constructor(
    private val statisticsManager: StatisticsManager
) {
    companion object {
        const val MAX_ROLLS = 3
        val CATEGORIES = setOf(
            "Ones", "Twos", "Threes", "Fours", "Fives", "Sixes",
            "Full House", "Four of a Kind", "Straight", "Five of a Kind", "Choice"
        )
    }

    fun initializeGame(): BalutScoreState {
        val startingPlayer = if (Random.nextInt(0, 2) == 0) 0 else AI_PLAYER_ID.hashCode()
        return BalutScoreState(
            playerScores = mapOf(
                0 to emptyMap(),  // Human player
                AI_PLAYER_ID.hashCode() to emptyMap()  // AI player
            ),
            currentPlayerIndex = startingPlayer,
            currentRound = 1,
            maxRounds = CATEGORIES.size,
            rollsLeft = MAX_ROLLS,
            heldDice = emptySet(),
            message = if (startingPlayer == 0)
                "You go first! Round 1 of ${CATEGORIES.size}."
            else
                "AI goes first! Round 1 of ${CATEGORIES.size}.",
            isGameOver = false
        )
    }

    fun handleTurn(
        diceResults: List<Int>,
        currentState: BalutScoreState,
        heldDice: Set<Int>
    ): BalutScoreState {
        return if (currentState.currentPlayerIndex == AI_PLAYER_ID.hashCode()) {
            handleAITurn(diceResults, currentState)
        } else {
            handlePlayerTurn(diceResults, currentState, heldDice)
        }
    }

    private fun handlePlayerTurn(
        diceResults: List<Int>,
        currentState: BalutScoreState,
        heldDice: Set<Int>
    ): BalutScoreState {
        if (currentState.rollsLeft <= 0) {
            return currentState.copy(
                message = "No rolls left. Please select a category."
            )
        }

        val message = buildPlayerTurnMessage(diceResults, currentState.rollsLeft)
        return currentState.copy(
            rollsLeft = currentState.rollsLeft - 1,
            message = message,
            heldDice = heldDice
        )
    }

    private fun buildPlayerTurnMessage(diceResults: List<Int>, rollsLeft: Int): String {
        return buildString {
            append("Rolled: ${diceResults.joinToString()}")
            if (rollsLeft > 1 && rollsLeft < 4) append("\n${rollsLeft-1} rolls left") else append("\nLast roll!")
            append("\nHold dice by clicking them.")
        }
    }

    private fun handleAITurn(
        diceResults: List<Int>,
        currentState: BalutScoreState
    ): BalutScoreState {
        if (currentState.rollsLeft <= 0) {
            // AI chooses a category
            val category = chooseAICategory(diceResults, currentState)
            return scoreCategory(currentState, diceResults, category)
        }

        // AI decides which dice to hold
        val diceToHold = decideAIDiceHolds(diceResults, currentState)

        val message = buildAITurnMessage(diceResults, diceToHold, currentState.rollsLeft)
        return currentState.copy(
            rollsLeft = currentState.rollsLeft - 1,
            message = message,
            heldDice = diceToHold
        )
    }

    private fun buildAITurnMessage(
        diceResults: List<Int>,
        heldDice: Set<Int>,
        rollsLeft: Int
    ): String {
        return buildString {
            append("AI rolled: ${diceResults.joinToString()}")
            append("\nAI holds: ${heldDice.joinToString { diceResults[it].toString() }}")
            if (rollsLeft > 1) append("\n$rollsLeft rolls left.") else append("\nAI's last roll!")
        }
    }

    fun scoreCategory(
        currentState: BalutScoreState,
        dice: List<Int>,
        category: String
    ): BalutScoreState {
        val currentPlayer = currentState.currentPlayerIndex

        // Calculate score for the selected category
        val score = calculateCategoryScore(dice, category)

        // Update player scores
        val updatedScores = currentState.playerScores.toMutableMap()
        updatedScores[currentPlayer] =
            updatedScores[currentPlayer].orEmpty() + (category to score)

        // Determine next round and player
        val nextPlayer = if (currentPlayer == 0) AI_PLAYER_ID.hashCode() else 0
        val nextRound =
            if (nextPlayer == 0) currentState.currentRound + 1 else currentState.currentRound
        val isGameOver = checkIfGameOver(updatedScores)

        // Calculate total score for the current player
        val totalScore = updatedScores[currentPlayer]?.values?.sum() ?: 0

        return currentState.copy(
            playerScores = updatedScores,
            currentPlayerIndex = nextPlayer,
            currentRound = nextRound,
            rollsLeft = MAX_ROLLS,
            heldDice = emptySet(),
            message = GameMessages.buildBalutCategoryMessage(
                dice = dice,
                category = category,
                score = score,
                playerIndex = currentPlayer,
                isGameOver = isGameOver,
                totalScore = totalScore
            ),
            isGameOver = isGameOver
        )
    }

    private fun calculateCategoryScore(dice: List<Int>, category: String): Int {
        return when (category) {
            // Number categories (Ones through Sixes)
            "Ones" -> dice.count { it == 1 } * 1
            "Twos" -> dice.count { it == 2 } * 2
            "Threes" -> dice.count { it == 3 } * 3
            "Fours" -> dice.count { it == 4 } * 4
            "Fives" -> dice.count { it == 5 } * 5
            "Sixes" -> dice.count { it == 6 } * 6

            // Special combinations
            "Five of a Kind" -> {
                if (dice.groupBy { it }.any { it.value.size == 5 }) 50 else 0
            }

            "Four of a Kind" -> {
                if (dice.groupBy { it }.any { it.value.size >= 4 }) 40 else 0
            }

            "Full House" -> {
                val groups = dice.groupBy { it }
                if (groups.size == 2 && groups.values.any { it.size == 3 }) 35 else 0
            }

            "Straight" -> {
                val sorted = dice.sorted()
                if (sorted.windowed(2).all { (a, b) -> b - a == 1 }) 30 else 0
            }

            "Choice" -> dice.sum()
            else -> 0
        }
    }

    private fun checkIfGameOver(
        updatedScores: Map<Int, Map<String, Int>>
    ): Boolean {
        return updatedScores.values.any { scores ->
            CATEGORIES.all { it in scores }
        }
    }

    private fun decideAIDiceHolds(
        diceResults: List<Int>,
        currentState: BalutScoreState
    ): Set<Int> {
        val playerAnalysis = statisticsManager.playerAnalysis.value
        val aiStyle = when (playerAnalysis?.playStyle) {
            PlayStyle.AGGRESSIVE -> 0.7  // More likely to hold promising dice
            PlayStyle.CAUTIOUS -> 0.5   // More balanced approach
            else -> 0.6                 // Default strategy
        }

        // Get the scoring pattern from calculateBalutScore
        val (_, scoringDice) = ScoreCalculator.calculateBalutScore(diceResults)

        // If we have a scoring pattern, hold those dice
        if (scoringDice.isNotEmpty()) {
            return scoringDice
        }

        // If no scoring pattern, use probability-based holding for number categories
        return diceResults.mapIndexedNotNull { index, value ->
            when {
                // Hold high-value numbers more often
                value == 6 || value == 5 -> if (Random.nextDouble() < aiStyle) index else null
                // Hold other numbers based on AI style and their count
                else -> {
                    val numberCount = diceResults.count { it == value }
                    val holdProbability = when {
                        numberCount >= 2 -> aiStyle  // More likely to hold pairs
                        else -> aiStyle - 0.3        // Less likely to hold single dice
                    }
                    if (Random.nextDouble() < holdProbability) index else null
                }
            }
        }.toSet()
    }

    fun chooseAICategory(
        diceResults: List<Int>,
        currentState: BalutScoreState
    ): String {
        // Get available categories
        val usedCategories = currentState.playerScores[AI_PLAYER_ID.hashCode()] ?: emptyMap()
        val availableCategories = CATEGORIES - usedCategories.keys

        // Calculate scores for each available category
        val categoryScores = availableCategories.map { category ->
            category to calculateCategoryScore(diceResults, category)
        }

        // Choose the highest scoring available category
        return categoryScores.maxByOrNull { it.second }?.first ?: availableCategories.first()
    }
}
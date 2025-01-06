package com.mayor.kavi.game

import com.mayor.kavi.data.manager.StatisticsManager
import com.mayor.kavi.data.models.GameScoreState.BalutScoreState
import com.mayor.kavi.data.models.PlayStyle
import com.mayor.kavi.ui.viewmodel.GameViewModel.Companion.AI_PLAYER_ID
import com.mayor.kavi.util.ScoreCalculator
import javax.inject.Inject
import kotlin.random.Random

/**
 * Manager class for the Balut dice game variant.
 *
 * Balut is a strategic dice game similar to Yahtzee, where players:
 * - Roll five dice up to three times per turn
 * - Choose which dice to keep between rolls
 * - Score in various categories based on dice combinations
 *
 * The game proceeds through multiple rounds, with each player
 * scoring once in each category. The game ends when all categories
 * are filled, and the player with the highest total score wins.
 *
 * @property statisticsManager Manages game statistics and AI behavior analysis
 */
class BalutGameManager @Inject constructor(
    private val statisticsManager: StatisticsManager
) {
    companion object {
        /** Maximum number of rolls allowed per turn */
        const val MAX_ROLLS = 3

        /** Available scoring categories in the game */
        val CATEGORIES = setOf(
            "Ones", "Twos", "Threes", "Fours", "Fives", "Sixes",
            "Full House", "Four of a Kind", "Small Straight", "Large Straight", "Five of a Kind",
            "Choice"
        )
    }

    /**
     * Initializes a new Balut game.
     *
     * Creates a new game state with:
     * - Random starting player selection
     * - Empty score maps for all players
     * - Initial game parameters (rounds, rolls, etc.)
     *
     * @return A new [BalutScoreState] with initial game settings
     */
    fun initializeGame(): BalutScoreState {
        val startingPlayer = if (Random.nextInt(0, 2) == 0) 0 else AI_PLAYER_ID.hashCode()
        return BalutScoreState(
            playerScores = mapOf(
                0 to emptyMap(),
                AI_PLAYER_ID.hashCode() to emptyMap()
            ),
            currentPlayerIndex = startingPlayer,
            currentRound = 1,
            maxRounds = CATEGORIES.size,
            rollsLeft = MAX_ROLLS,
            heldDice = emptySet(),
            isGameOver = false
        )
    }

    /**
     * Handles a turn in the game, processing dice results and updating game state.
     *
     * Different handling for:
     * - Player turns: Manages dice holding and roll counting
     * - AI turns: Makes strategic decisions about dice selection
     *
     * @param diceResults Results of the current dice roll
     * @param currentState Current state of the game
     * @param heldDice Set of dice indices that are currently held
     * @return Updated game state after processing the turn
     */
    fun handleTurn(
        diceResults: List<Int>, currentState: BalutScoreState, heldDice: Set<Int>
    ): BalutScoreState {
        return if (currentState.currentPlayerIndex == AI_PLAYER_ID.hashCode()) {
            handleAITurn(diceResults, currentState)
        } else {
            handlePlayerTurn(currentState, heldDice)
        }
    }

    private fun handlePlayerTurn(
        currentState: BalutScoreState, heldDice: Set<Int>
    ): BalutScoreState {
        if (currentState.rollsLeft <= 0) {
            return currentState
        }

        return currentState.copy(
            rollsLeft = currentState.rollsLeft - 1,
            heldDice = heldDice
        )
    }

    private fun handleAITurn(
        diceResults: List<Int>, currentState: BalutScoreState
    ): BalutScoreState {
        if (currentState.rollsLeft <= 0) {
            // AI chooses a category
            val category = chooseAICategory(diceResults, currentState)
            return scoreCategory(currentState, diceResults, category)
        }

        // AI decides which dice to hold
        val diceToHold = decideAIDiceHolds(diceResults)

        return currentState.copy(
            rollsLeft = currentState.rollsLeft - 1,
            heldDice = diceToHold
        )
    }

    /**
     * Scores a category for the current player.
     *
     * This method:
     * 1. Calculates score for the selected category
     * 2. Updates player's score map
     * 3. Advances to next player/round
     * 4. Checks for game completion
     *
     * @param currentState Current state of the game
     * @param dice Current dice results to score
     * @param category Category to score in
     * @return Updated game state after scoring
     */
    fun scoreCategory(
        currentState: BalutScoreState, dice: List<Int>, category: String
    ): BalutScoreState {
        val currentPlayer = currentState.currentPlayerIndex
        // Calculate score for the selected category
        val score = ScoreCalculator.calculateCategoryScore(dice, category)

        // Update player scores
        val updatedScores = currentState.playerScores.toMutableMap()
        updatedScores[currentPlayer] =
            updatedScores[currentPlayer].orEmpty() + (category to score)
        // Determine next round and player
        val nextPlayer = if (currentPlayer == 0) AI_PLAYER_ID.hashCode() else 0
        val nextRound =
            if (nextPlayer == 0) currentState.currentRound + 1 else currentState.currentRound
        val isGameOver = checkIfGameOver(updatedScores)

        return currentState.copy(
            playerScores = updatedScores,
            currentPlayerIndex = nextPlayer,
            currentRound = nextRound,
            rollsLeft = MAX_ROLLS,
            heldDice = emptySet(),
            isGameOver = isGameOver
        )
    }

    private fun checkIfGameOver(updatedScores: Map<Int, Map<String, Int>>): Boolean {
        return updatedScores.values.any { scores ->
            CATEGORIES.all { it in scores }
        }
    }

    private fun decideAIDiceHolds(diceResults: List<Int>): Set<Int> {
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
                        else -> aiStyle - 0.3        // Less likely to hold singles
                    }
                    if (Random.nextDouble() < holdProbability) index else null
                }
            }
        }.toSet()
    }

    /**
     * Determines the optimal category choice for the AI player.
     *
     * Analyzes current dice results and available categories to make
     * the most strategic scoring decision.
     *
     * @param diceResults Current dice results to analyze
     * @param currentState Current game state
     * @return The chosen category name
     */
    fun chooseAICategory(diceResults: List<Int>, currentState: BalutScoreState): String {
        val aiScores = currentState.playerScores[AI_PLAYER_ID.hashCode()] ?: emptyMap()
        val availableCategories = CATEGORIES.filter { it !in aiScores }
        if (availableCategories.isEmpty()) return "Choice"

        // Calculate scores for each available category
        val categoryScores = availableCategories.associateWith { category ->
            val baseScore = ScoreCalculator.calculateCategoryScore(diceResults, category)
            when (category) {
                // Prioritize high-value categories
                "Five of a Kind" -> if (baseScore > 0) baseScore * 2.0 else 0.0
                "Large Straight" -> if (baseScore > 0) baseScore * 1.8 else 0.0
                "Small Straight" -> if (baseScore > 0) baseScore * 1.6 else 0.0
                "Four of a Kind" -> if (baseScore > 0) baseScore * 1.5 else 0.0
                "Full House" -> if (baseScore > 0) baseScore * 1.4 else 0.0

                // Number categories - prioritize based on count
                "Ones" -> {
                    val count = diceResults.count { it == 1 }
                    weightNumberCategory(baseScore, count)
                }
                "Twos" -> {
                    val count = diceResults.count { it == 2 }
                    weightNumberCategory(baseScore, count)
                }
                "Threes" -> {
                    val count = diceResults.count { it == 3 }
                    weightNumberCategory(baseScore, count)
                }
                "Fours" -> {
                    val count = diceResults.count { it == 4 }
                    weightNumberCategory(baseScore, count)
                }
                "Fives" -> {
                    val count = diceResults.count { it == 5 }
                    weightNumberCategory(baseScore, count)
                }
                "Sixes" -> {
                    val count = diceResults.count { it == 6 }
                    weightNumberCategory(baseScore, count)
                }

                // Choice is last resort, but weight based on total
                "Choice" -> {
                    val total = diceResults.sum()
                    when {
                        total >= 20 -> baseScore * 1.2  // Good choice score
                        total >= 15 -> baseScore * 1.0  // Average choice score
                        else -> baseScore * 0.8         // Poor choice score
                    }
                }
                else -> baseScore.toDouble()
            }
        }

        // Consider game state for final decision
        val roundsLeft = CATEGORIES.size - currentState.currentRound
        val bestCategory = categoryScores.maxByOrNull { it.value }?.key ?: availableCategories.first()

        // If near end game and haven't used Choice, prefer it for good rolls
        if (roundsLeft <= 2 && "Choice" in availableCategories && diceResults.sum() >= 20) {
            return "Choice"
        }

        return bestCategory
    }

    private fun weightNumberCategory(baseScore: Int, count: Int): Double {
        return when (count) {
            5 -> baseScore * 1.5  // Perfect number category
            4 -> baseScore * 1.3  // Very good
            3 -> baseScore * 1.2  // Good
            else -> baseScore.toDouble()
        }
    }
}
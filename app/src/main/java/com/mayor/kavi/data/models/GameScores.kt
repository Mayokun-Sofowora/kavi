// GameScores.kt
package com.mayor.kavi.data.models

/**
 * Where the states are defined for the boards.
 */
sealed class GameScoreState {
    abstract val isGameOver: Boolean
    abstract val message: String
    abstract val currentPlayerIndex: Int
    abstract val playerCount: Int

    data class PigScoreState(
        val playerScores: Map<Int, Int> = emptyMap(),
        val currentTurnScore: Int = 0,
        override val currentPlayerIndex: Int = 0,
        override val playerCount: Int = 2,
        override val isGameOver: Boolean = false,
        override val message: String = ""
    ) : GameScoreState()

    data class GreedScoreState(
        val playerScores: Map<Int, Int> = emptyMap(),
        val turnScore: Int = 0,
        val heldDice: Set<Int> = emptySet(),
        val scoringDice: Set<Int> = emptySet(),
        val canReroll: Boolean = true,
        val roundHistory: Map<Int, List<Int>> = emptyMap(),
        override val currentPlayerIndex: Int = 0,
        override val playerCount: Int = 2,
        override val isGameOver: Boolean = false,
        override val message: String = ""
    ) : GameScoreState() {
        companion object {
            val SCORING_VALUES = mapOf(1 to 100, 5 to 50)
        }
    }

    data class BalutScoreState(
        val playerScores: Map<Int, Map<String, Int>> = emptyMap(),
        val rollsLeft: Int = 4, // changed to 4 so you can roll 3 times
        val heldDice: Set<Int> = emptySet(),
        val currentRound: Int = 1,
        val maxRounds: Int = 11,
        override val currentPlayerIndex: Int = 0,
        override val playerCount: Int = 2,
        override val isGameOver: Boolean = false,
        override val message: String = ""
    ) : GameScoreState()

    data class CustomScoreState(
        val gameId: String = "",
        val diceCount: Int = 2,
        val scoreHistory: Map<Int, List<String>> = emptyMap(),
        val gameName: String = "Custom Dice Game",
        val playerNames: Map<Int, String> = mapOf(0 to "Player 1", 1 to "Player 2"),
        val notes: List<String> = emptyList(),
        val playerScores: Map<Int, Int> = emptyMap(),
        override val currentPlayerIndex: Int = 0,
        override val message: String = "",
        override val playerCount: Int = 2,
        override val isGameOver: Boolean = false,
    ) : GameScoreState()

}

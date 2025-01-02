package com.mayor.kavi.data.models

import kotlinx.serialization.Serializable

@Serializable
sealed class GameScoreState {
    abstract val isGameOver: Boolean
    abstract val currentPlayerIndex: Int

    @Serializable
    data class PigScoreState(
        val playerScores: Map<Int, Int> = emptyMap(),
        val currentTurnScore: Int = 0,
        override val currentPlayerIndex: Int = 0,
        override val isGameOver: Boolean = false
    ) : GameScoreState()

    @Serializable
    data class GreedScoreState(
        val playerScores: Map<Int, Int> = emptyMap(),
        val currentTurnScore: Int = 0,
        val heldDice: Set<Int> = emptySet(),
        val scoringDice: Set<Int> = emptySet(),
        val canReroll: Boolean = true,
        val lastRoll: List<Int> = emptyList(),
        override val currentPlayerIndex: Int = 0,
        override val isGameOver: Boolean = false
    ) : GameScoreState() {
        companion object {
            val SCORING_VALUES = mapOf(1 to 100, 5 to 50)
        }
    }

    @Serializable
    data class BalutScoreState(
        val playerScores: Map<Int, Map<String, Int>> = emptyMap(),
        val rollsLeft: Int = 4,
        val heldDice: Set<Int> = emptySet(),
        val currentRound: Int = 1,
        val maxRounds: Int = 11,
        override val currentPlayerIndex: Int = 0,
        override val isGameOver: Boolean = false
    ) : GameScoreState()

    @Serializable
    data class CustomScoreState(
        val diceCount: Int = 2,
        val scoreHistory: Map<Int, List<String>> = emptyMap(),
        val gameName: String = "Custom Dice Game",
        val playerNames: Map<Int, String> = mapOf(0 to "Player 1", 1 to "Player 2"),
        val notes: List<String> = emptyList(),
        val playerScores: Map<Int, Int> = emptyMap(),
        override val currentPlayerIndex: Int = 0,
        override val isGameOver: Boolean = false
    ) : GameScoreState()
}

package com.mayor.kavi.data.games

data class GreedScoreState(
    val currentScore: Int = 0,
    val turnScore: Int = 0,
    val heldDice: Set<Int> = emptySet(),
    val scoringDice: Set<Int> = emptySet(),
    val canReroll: Boolean = true,
    val isGameOver: Boolean = false,
    val resultMessage: String = "",
    val roundHistory: List<Int> = emptyList() // Track scores for each round
) {
    companion object {
        // Scoring rules
        val SCORING_VALUES = mapOf(
            1 to 100,  // Single 1 = 100
            5 to 50    // Single 5 = 50
        )

        // Special combinations
        val COMBINATIONS = mapOf(
            listOf(1,1,1) to 1000,      // Three 1s = 1000
            listOf(2,2,2) to 200,       // Three 2s = 200
            listOf(3,3,3) to 300,       // Three 3s = 300
            listOf(4,4,4) to 400,       // Three 4s = 400
            listOf(5,5,5) to 500,       // Three 5s = 500
            listOf(6,6,6) to 600        // Three 6s = 600
        )
    }
}
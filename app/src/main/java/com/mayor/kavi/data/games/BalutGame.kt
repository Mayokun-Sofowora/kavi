package com.mayor.kavi.data.games

data class BalutScoreState(
    val currentRound: Int = 0,
    val maxRounds: Int = categories.size,
    val rollsLeft: Int = 3,
    val currentCategory: String = categories.first(),
    val heldDice: Set<Int> = emptySet(), // Indices of held dice
    val categoryScores: Map<String, Int> = emptyMap()
) {
    companion object {
        val categories = listOf(
            "Aces", "Twos", "Threes", "Fours", "Fives", "Sixes",
            "Straight", "Full House", "Four of a Kind", "Five of a Kind", "Choice"
        )
    }
}

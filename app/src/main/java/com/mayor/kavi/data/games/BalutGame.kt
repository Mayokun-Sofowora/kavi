package com.mayor.kavi.data.games

data class BalutScoreState(
    val currentCategory: String = "Aces",
    val currentRound: Int = 1,
    val maxRolls: Int = 3,
    val rollsLeft: Int = 3
) {
    companion object {
        val categories = listOf(
            "Aces", "Twos", "Threes", "Fours", "Fives", "Sixes",
            "Straight", "Full House", "Four of a Kind", "Five of a Kind", "Choice"
        )
    }
}

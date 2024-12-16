package com.mayor.kavi.data.games

data class ChicagoScoreState(
    val currentRound: Int = 2,
    val totalScore: Int = 0,
    val roundScore: Int = 0,
    val hasScored: Boolean = false,
    val roundScores: Map<Int, Int> = emptyMap() // Track scores for each round
)

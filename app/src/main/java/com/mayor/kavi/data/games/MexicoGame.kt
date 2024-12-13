package com.mayor.kavi.data.games

data class MexicoScoreState(
    val currentRound: Int = 1,
    val lives: Int = 6,
    val isFirstRound: Boolean = true,
    val roundScores: MutableList<Int> = mutableListOf()
)

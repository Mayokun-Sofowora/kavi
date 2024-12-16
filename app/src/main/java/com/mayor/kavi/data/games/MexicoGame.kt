package com.mayor.kavi.data.games

data class MexicoScoreState(
    val roundScores: MutableList<Int> = mutableListOf(),
    val lives: Int = 3,
    val roundTimeSeconds: Int = 15,
    val isTimerRunning: Boolean = false,
    val currentRoundNumber: Int = 1,
    val highestScore: Int = 0,
    val isGameOver: Boolean = false,
    val gameStatus: String = "",
    val resultMessage: String = ""
)

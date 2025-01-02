package com.mayor.kavi.data.models

data class LeaderboardEntry(
    val userId: String = "",
    val position: Int = 0,
    val displayName: String = "",
    val score: Int = 0,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val lastUpdated: Long = 0
) 
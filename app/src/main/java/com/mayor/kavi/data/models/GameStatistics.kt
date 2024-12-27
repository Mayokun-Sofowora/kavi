package com.mayor.kavi.data.models

import kotlinx.serialization.Serializable

@Serializable
data class GameStatistics(
    val gamesPlayed: Int = 0,
    val highScores: Map<String, Int> = emptyMap(),
    val winRates: Map<String, WinRate> = emptyMap(),
    val lastSeen: Long? = System.currentTimeMillis(),
    val playerAnalysis: PlayerAnalysis? = null
)

@Serializable
data class WinRate(
    var wins: Int = 0,
    var total: Int = 0
)

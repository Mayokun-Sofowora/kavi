package com.mayor.kavi.data.models

data class GameResult(
    val id: String = "",
    val gameType: String = "",
    val playerIds: List<String> = emptyList(),
    val scores: Map<String, Int> = emptyMap(),
    val winnerId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)


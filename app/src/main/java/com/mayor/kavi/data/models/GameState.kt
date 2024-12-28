package com.mayor.kavi.data.models

data class GameState(
    val status: String = "pending",
    val scores: Map<String, Int> = emptyMap(),
    val turnData: List<TurnData> = emptyList(),
    val lastUpdate: Long = 0,
    val currentPlayerId: String = ""
)
package com.mayor.kavi.data.models

data class GameState(
    val status: String = "pending",
    val scores: Map<String, Int> = mapOf(),
    val turnData: List<TurnData> = listOf(),
    val lastAction: String = "",
    val lastUpdate: Long = System.currentTimeMillis()
)
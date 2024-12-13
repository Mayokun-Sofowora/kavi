package com.mayor.kavi.data

data class GameSession(
    val id: String = "",
    val players: List<String> = listOf(), // UIDs of players
    val currentTurn: String = "", // UID of current player
    val gameState: Map<String, Any> = mapOf(),
    val scores: Map<String, Int> = mapOf(),
    val timestamp: Long = System.currentTimeMillis()
)
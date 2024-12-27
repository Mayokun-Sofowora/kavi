// GameSession.kt
package com.mayor.kavi.data.models

data class GameSession(
    val id: String = "",
    val players: List<PlayerInfoData> = listOf(),
    val currentTurn: String = "",
    val gameMode: String? = null,
    val gameState: GameState = GameState(),
    val isGameStarted: Boolean = false,
    val scores: Map<String, Int> = mapOf(),
    val lastAction: String = "",
    val turnData: List<TurnData> = listOf(),
    val timestamp: Long = System.currentTimeMillis()
)

data class PlayerInfoData(
    val id: String = "",
    val name: String = "",
    val score: Int = 0,
    val isCurrentTurn: Boolean = false,
    val isReady: Boolean = false
)

data class TurnData(
    val playerID: String = "",
    val diceRoll: List<Int> = listOf(),
    val bankedScore: Int = 0,
    val heldDice: List<Int> = listOf(),
    val roundScore: Int = 0
)

sealed class PlayMode {
    object SinglePlayer : PlayMode()
    object Multiplayer : PlayMode()
}

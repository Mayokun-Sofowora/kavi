// GameSessionManager.kt
package com.mayor.kavi.data.manager

import com.mayor.kavi.data.models.*
import com.mayor.kavi.data.repository.GameRepository
import com.mayor.kavi.util.GameBoard
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * GameSessionManager is responsible for managing game sessions and their associated data.
 */
class GameSessionManager @Inject constructor(
    private val gameRepository: GameRepository
) {
    private val _gameSessionUpdates = MutableSharedFlow<GameSession>()
    val gameSessionUpdates: SharedFlow<GameSession> = _gameSessionUpdates

    sealed class GameAction {
        data class Roll(
            val diceResults: List<Int>,
            val heldDice: Set<Int>,
            val turnScore: Int,
            val gameMode: String
        ) : GameAction()

        data class BankScore(val totalScore: Int, val gameMode: String) : GameAction()

    }

    fun handleGameAction(session: GameSession, action: GameAction) =
        CoroutineScope(Dispatchers.IO).launch {
            when (action) {
                is GameAction.Roll -> handleRollAction(session, action)
                is GameAction.BankScore -> handleBankScoreAction(session, action)
            }
        }

    private suspend fun handleRollAction(gameSession: GameSession, action: GameAction.Roll) {
        var turnScores = gameSession.scores.toMutableMap()
        val currentScore = turnScores[gameSession.currentTurn] ?: 0
        val updatedScores =
            when (gameSession.gameMode) {
                GameBoard.GREED.modeName -> turnScores.apply {
                    this[gameSession.currentTurn] = currentScore + action.turnScore
                }
                else -> turnScores
            }
        val state = GameState(
            scores = updatedScores,
            lastUpdate = System.currentTimeMillis(),
            status = gameSession.gameState.status
        )
        gameRepository.updateGameState(gameSession.id, state)
        gameRepository.switchTurn(gameSession.id)
    }

    private suspend fun handleBankScoreAction(gameSession: GameSession, action: GameAction.BankScore){
        val currentScores = gameSession.scores.toMutableMap()
        currentScores[gameSession.currentTurn] = action.totalScore

        val state = GameState(
            scores = currentScores,
            lastUpdate = System.currentTimeMillis(),
            status = gameSession.gameState.status,
        )
        gameRepository.updateGameState(gameSession.id, state)
        gameRepository.switchTurn(gameSession.id)
    }
}

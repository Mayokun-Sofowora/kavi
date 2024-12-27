package com.mayor.kavi.data.repository

import com.mayor.kavi.data.models.GameSession
import com.mayor.kavi.data.models.GameState
import com.mayor.kavi.data.models.UserProfile
import com.mayor.kavi.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * This repository handles the interaction with the backend for game-related operations.
 */
interface GameRepository {
    fun getSessionId(): String?
    suspend fun createGameSession(opponent: String): Result<GameSession>
    suspend fun setPlayerReady(sessionId: String, isReady: Boolean): Result<Unit>
    suspend fun joinGameSession(sessionId: String): Result<GameSession>
    suspend fun cleanupGameSession(sessionId: String): Result<Unit>
    suspend fun getGameSession(sessionId: String): Result<GameSession>
    suspend fun updateGameState(sessionId: String, state: GameState): Result<Unit>
    suspend fun switchTurn(sessionId: String): Result<Unit>
    suspend fun getScorePoints(): Result<String>
    fun listenToGameUpdates(sessionId: String): Flow<GameSession>
    fun getOnlinePlayers(): Flow<List<UserProfile>>
    fun listenForGameInvites(): Flow<GameSession>
}
package com.mayor.kavi.data.repository.fakes

import com.mayor.kavi.data.models.GameSessionsEntity
import com.mayor.kavi.data.repository.GameSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.atomic.AtomicLong

class FakeGameSessionsRepository : GameSessionRepository {

    // In-memory storage for game sessions
    private val gameSessionsMap = mutableMapOf<Long, MutableList<GameSessionsEntity>>()
    private val sessionIdCounter = AtomicLong(1) // Atomic counter to simulate unique session IDs

    override suspend fun insertSession(session: GameSessionsEntity) {
        val newSession = session.copy(sessionId = sessionIdCounter.getAndIncrement()) // Assign unique sessionId
        val gameSessions = gameSessionsMap.getOrPut(newSession.gameId) { mutableListOf() }
        gameSessions.add(newSession)
    }

    override suspend fun updateSession(session: GameSessionsEntity) {
        val gameSessions = gameSessionsMap[session.gameId]
        gameSessions?.let {
            val index = it.indexOfFirst { existingSession -> existingSession.sessionId == session.sessionId }
            if (index != -1) {
                it[index] = session.copy(updatedAt = session.updatedAt) // Update session details
            }
        }
    }

    override suspend fun deleteSession(session: GameSessionsEntity) {
        val gameSessions = gameSessionsMap[session.gameId]
        gameSessions?.removeIf { it.sessionId == session.sessionId } // Remove session by sessionId
    }

    override suspend fun getSessionById(sessionId: Long): GameSessionsEntity? {
        return gameSessionsMap.values.flatten().find { it.sessionId == sessionId }
    }

    override fun getSessionsByGameId(gameId: Long): Flow<List<GameSessionsEntity>> {
        return flow {
            emit(gameSessionsMap[gameId]?.toList() ?: emptyList()) // Emit the list of sessions for a game
        }
    }

    override suspend fun getAllSessions(): List<GameSessionsEntity> {
        return gameSessionsMap.values.flatten() // Return a flat list of all sessions
    }
}

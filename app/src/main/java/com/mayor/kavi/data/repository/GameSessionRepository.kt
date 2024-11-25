package com.mayor.kavi.data.repository

import com.mayor.kavi.data.dao.GameSessions
import com.mayor.kavi.data.dao.GameSessionsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GameSessionsRepository {

    /**
     * Inserts a new game session.
     */
    suspend fun insertSession(session: GameSessions)

    /**
     * Updates an existing game session.
     */
    suspend fun updateSession(session: GameSessions)

    /**
     * Deletes a game session.
     */
    suspend fun deleteSession(session: GameSessions)

    /**
     * Retrieves a game session by its ID.
     */
    suspend fun getSessionById(sessionId: Long): GameSessions?

    /**
     * Retrieves all sessions associated with a specific game.
     */
    fun getSessionsByGameId(gameId: Long): Flow<List<GameSessions>>
}

class GameSessionsRepositoryImpl @Inject constructor(
    private val gameSessionsDao: GameSessionsDao
) : GameSessionsRepository {

    override suspend fun insertSession(session: GameSessions) {
        gameSessionsDao.insertSession(session)
    }

    override suspend fun updateSession(session: GameSessions) {
        gameSessionsDao.updateSession(session)
    }

    override suspend fun deleteSession(session: GameSessions) {
        gameSessionsDao.deleteSession(session)
    }

    override suspend fun getSessionById(sessionId: Long): GameSessions? {
        return gameSessionsDao.getSessionById(sessionId)
    }

    override fun getSessionsByGameId(gameId: Long): Flow<List<GameSessions>> {
        return flow {
            emit(gameSessionsDao.getSessionsByGameId(gameId))
        }
    }
}

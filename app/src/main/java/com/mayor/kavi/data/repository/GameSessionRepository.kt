package com.mayor.kavi.data.repository

import com.mayor.kavi.data.models.GameSessionsEntity
import com.mayor.kavi.data.dao.GameSessionsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GameSessionRepository {

    /**
     * Inserts a new game session.
     */
    suspend fun insertSession(session: GameSessionsEntity)

    /**
     * Updates an existing game session.
     */
    suspend fun updateSession(session: GameSessionsEntity)

    /**
     * Deletes a game session.
     */
    suspend fun deleteSession(session: GameSessionsEntity)

    /**
     * Retrieves a game session by its ID.
     */
    suspend fun getSessionById(sessionId: Long): GameSessionsEntity?

    /**
     * Retrieves all sessions associated with a specific game.
     */
    fun getSessionsByGameId(gameId: Long): Flow<List<GameSessionsEntity>>

    suspend fun getAllSessions(): List<GameSessionsEntity>
}

class GameSessionRepositoryImpl @Inject constructor(
    private val gameSessionsDao: GameSessionsDao
) : GameSessionRepository {

    override suspend fun insertSession(session: GameSessionsEntity) {
        gameSessionsDao.insertGameSession(session)
    }

    override suspend fun updateSession(session: GameSessionsEntity) {
        gameSessionsDao.updateGameSession(session)
    }

    override suspend fun deleteSession(session: GameSessionsEntity) {
        gameSessionsDao.deleteGameSession(session)
    }

    override suspend fun getSessionById(sessionId: Long): GameSessionsEntity? {
        return gameSessionsDao.getSessionById(sessionId)
    }

    override fun getSessionsByGameId(gameId: Long): Flow<List<GameSessionsEntity>> {
        return flow {
            emit(gameSessionsDao.getSessionsByGameId(gameId))
        }
    }

    override suspend fun getAllSessions(): List<GameSessionsEntity> {
        return gameSessionsDao.getAllSessions()
    }
}

package com.mayor.kavi.data.repository

import com.mayor.kavi.data.dao.GameResultsDao
import com.mayor.kavi.data.models.GameResultsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GameResultsRepository {

    /**
     * Inserts a new game result.
     */
    suspend fun insertResult(result: GameResultsEntity)

    /**
     * Updates an existing game result.
     */
    suspend fun updateResult(result: GameResultsEntity)

    /**
     * Deletes a game result.
     */
    suspend fun deleteResult(result: GameResultsEntity)

    /**
     * Retrieves a game result by its ID.
     */
    suspend fun getResultById(resultId: Long): GameResultsEntity?

    /**
     * Retrieves all game results associated with a specific session.
     */
    fun getResultsBySessionId(sessionId: Long): Flow<List<GameResultsEntity>>
}

class GameResultsRepositoryImpl @Inject constructor(
    private val gameResultsDao: GameResultsDao
) : GameResultsRepository {

    override suspend fun insertResult(result: GameResultsEntity) {
        gameResultsDao.insertGameResult(result)
    }

    override suspend fun updateResult(result: GameResultsEntity) {
        gameResultsDao.updateGameResult(result)
    }

    override suspend fun deleteResult(result: GameResultsEntity) {
        gameResultsDao.deleteGameResult(result)
    }

    override suspend fun getResultById(resultId: Long): GameResultsEntity? {
        return gameResultsDao.getResultById(resultId)
    }

    override fun getResultsBySessionId(sessionId: Long): Flow<List<GameResultsEntity>> {
        return flow {
            emit(gameResultsDao.getResultsBySession(sessionId))
        }
    }
}

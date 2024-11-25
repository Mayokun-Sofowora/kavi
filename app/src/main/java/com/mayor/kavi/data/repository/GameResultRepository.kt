package com.mayor.kavi.data.repository

import com.mayor.kavi.data.dao.GameResults
import com.mayor.kavi.data.dao.GameResultsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

interface GameResultsRepository {

    /**
     * Inserts a new game result.
     */
    suspend fun insertResult(result: GameResults)

    /**
     * Updates an existing game result.
     */
    suspend fun updateResult(result: GameResults)

    /**
     * Deletes a game result.
     */
    suspend fun deleteResult(result: GameResults)

    /**
     * Retrieves a game result by its ID.
     */
    suspend fun getResultById(resultId: Long): GameResults?

    /**
     * Retrieves all game results associated with a specific session.
     */
    fun getResultsBySessionId(sessionId: Long): Flow<List<GameResults>>
}

class GameResultsRepositoryImpl @Inject constructor(
    private val gameResultsDao: GameResultsDao
) : GameResultsRepository {

    override suspend fun insertResult(result: GameResults) {
        gameResultsDao.insertResult(result)
    }

    override suspend fun updateResult(result: GameResults) {
        gameResultsDao.updateResult(result)
    }

    override suspend fun deleteResult(result: GameResults) {
        gameResultsDao.deleteResult(result)
    }

    override suspend fun getResultById(resultId: Long): GameResults? {
        return gameResultsDao.getResultById(resultId)
    }

    override fun getResultsBySessionId(sessionId: Long): Flow<List<GameResults>> {
        return flow {
            emit(gameResultsDao.getResultsBySessionId(sessionId))
        }
    }
}

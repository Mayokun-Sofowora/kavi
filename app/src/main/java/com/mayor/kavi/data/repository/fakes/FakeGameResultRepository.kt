package com.mayor.kavi.data.repository.fakes

import com.mayor.kavi.data.models.GameResultsEntity
import com.mayor.kavi.data.repository.GameResultsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

class FakeGameResultsRepository : GameResultsRepository {

    private val gameResults = mutableListOf<GameResultsEntity>()
    private val gameResultsFlow = MutableStateFlow<List<GameResultsEntity>>(emptyList())
    private var resultIdCounter = 1L

    override suspend fun insertResult(result: GameResultsEntity) {
        val newResult = if (result.resultId == 0L) {
            result.copy(resultId = resultIdCounter++)
        } else {
            result
        }
        gameResults.add(newResult)
        emitGameResults()
    }

    override suspend fun updateResult(result: GameResultsEntity) {
        val index = gameResults.indexOfFirst { it.resultId == result.resultId }
        if (index != -1) {
            gameResults[index] = result
            emitGameResults()
        }
    }

    override suspend fun deleteResult(result: GameResultsEntity) {
        gameResults.removeIf { it.resultId == result.resultId }
        emitGameResults()
    }

    override suspend fun getResultById(resultId: Long): GameResultsEntity? {
        return gameResults.find { it.resultId == resultId }
    }

    override fun getResultsBySessionId(sessionId: Long): Flow<List<GameResultsEntity>> {
        return flow {
            emit(gameResults.filter { it.sessionId == sessionId })
        }
    }

    private fun emitGameResults() {
        gameResultsFlow.value = gameResults.toList()
    }
}

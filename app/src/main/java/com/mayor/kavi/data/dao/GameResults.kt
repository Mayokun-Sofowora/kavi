package com.mayor.kavi.data.dao

import androidx.room.*
import com.mayor.kavi.data.models.GameResultsEntity

@Dao
interface GameResultsDao {
    @Insert
    suspend fun insertGameResult(gameResult: GameResultsEntity)

    @Update
    suspend fun updateGameResult(gameResult: GameResultsEntity)

    @Delete
    suspend fun deleteGameResult(gameResult: GameResultsEntity)

    @Query("SELECT * FROM game_results WHERE session_id = :sessionId")
    suspend fun getResultsBySession(sessionId: Long): List<GameResultsEntity>

    @Query("SELECT * FROM game_results WHERE resultId = :resultId")
    suspend fun getResultById(resultId: Long): GameResultsEntity?

    @Query("SELECT * FROM game_results WHERE created_at BETWEEN :startTime AND :endTime")
    suspend fun getResultsByTimestampRange(startTime: Long, endTime: Long): List<GameResultsEntity>

    @Query("SELECT * FROM game_results WHERE final_score LIKE '%' || :playerName || '%'")
    suspend fun getResultsByPlayer(playerName: String): List<GameResultsEntity>
}

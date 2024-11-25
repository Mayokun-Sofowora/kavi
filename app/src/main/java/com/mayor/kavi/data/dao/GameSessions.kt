package com.mayor.kavi.data.dao

import androidx.room.*
import com.mayor.kavi.data.models.GameSessionsEntity

@Dao
interface GameSessionsDao {
    @Insert
    suspend fun insertGameSession(gameSession: GameSessionsEntity)

    @Update
    suspend fun updateGameSession(gameSession: GameSessionsEntity)

    @Delete
    suspend fun deleteGameSession(gameSession: GameSessionsEntity)

    @Query("SELECT * FROM game_sessions WHERE game_id = :gameId")
    suspend fun getSessionsByGameId(gameId: Long): List<GameSessionsEntity>

    @Query("SELECT * FROM game_sessions WHERE session_start BETWEEN :startTime AND :endTime")
    suspend fun getSessionsByTimestampRange(startTime: Long, endTime: Long): List<GameSessionsEntity>

    @Query("SELECT * FROM game_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: Long): GameSessionsEntity?

    @Query("SELECT * FROM game_sessions")
    suspend fun getAllSessions(): List<GameSessionsEntity>
}
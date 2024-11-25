package com.mayor.kavi.data.dao

import androidx.room.*
import com.mayor.kavi.data.models.GamePlayersEntity

@Dao
interface GamePlayersDao {
    @Insert
    suspend fun insertGamePlayer(gamePlayer: GamePlayersEntity)

    @Update
    suspend fun updateGamePlayer(gamePlayer: GamePlayersEntity)

    @Delete
    suspend fun deleteGamePlayer(gamePlayer: GamePlayersEntity)

    @Query("SELECT * FROM game_players WHERE game_id = :gameId")
    suspend fun getPlayersByGame(gameId: Long): List<GamePlayersEntity>

    @Query("SELECT * FROM game_players WHERE player_id = :userId")
    suspend fun getPlayerByUser(userId: Long): List<GamePlayersEntity>

    @Query("SELECT * FROM game_players WHERE device_id = :deviceId")
    suspend fun getPlayerByDevice(deviceId: String): List<GamePlayersEntity>

    @Query("SELECT * FROM game_players WHERE is_host = 1 AND game_id = :gameId")
    suspend fun getHostForGame(gameId: Long): GamePlayersEntity?

    @Query("SELECT * FROM game_players WHERE game_id = :gameId AND is_ready = 1")
    suspend fun getReadyPlayersForGame(gameId: Long): List<GamePlayersEntity>

    @Query("UPDATE game_players SET is_ready = 1 WHERE game_id = :gameId AND player_id = :userId")
    suspend fun setPlayerReady(gameId: Long, userId: Long)
}

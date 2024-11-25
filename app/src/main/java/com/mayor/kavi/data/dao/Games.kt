package com.mayor.kavi.data.dao

import androidx.room.*
import com.mayor.kavi.data.models.*

@Dao
interface GamesDao {
    @Insert
    suspend fun insertGame(game: GamesEntity)

    @Update
    suspend fun updateGame(game: GamesEntity)

    @Delete
    suspend fun deleteGame(game: GamesEntity)

    @Query("SELECT * FROM games WHERE gameId = :gameId")
    suspend fun getGameById(gameId: Long): GamesEntity?

    @Query("SELECT * FROM games")
    suspend fun getAllGames(): List<GamesEntity>

    @Query("SELECT * FROM games WHERE game_mode = :mode")
    suspend fun getGamesByMode(mode: GameModes): List<GamesEntity>

    @Query("SELECT * FROM games WHERE game_type = :type")
    suspend fun getGamesByType(type: GameTypes): List<GamesEntity>

}

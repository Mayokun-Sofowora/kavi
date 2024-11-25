package com.mayor.kavi.data.dao

import androidx.room.*
import com.mayor.kavi.data.models.GameModes
import com.mayor.kavi.data.models.GameTypes
import java.time.LocalDateTime

@Entity(tableName = "games")
data class Games(
    @PrimaryKey(autoGenerate = true) val gameId: Long = 0,
    @ColumnInfo(name = "game_type") val gameType: GameTypes,
    @ColumnInfo(name = "game_mode") val gameMode: GameModes,
    @ColumnInfo(name = "players") val players: String,
    @ColumnInfo(name = "min_players") val minPlayers: Int,
    @ColumnInfo(name = "max_players") val maxPlayers: Int,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime
)

@Dao
interface GameDao {
    @Insert
    suspend fun insertGame(game: Games)

    @Update
    suspend fun updateGame(game: Games)

    @Delete
    suspend fun deleteGame(game: Games)

    @Query("SELECT * FROM games WHERE gameId = :gameId")
    suspend fun getGameById(gameId: Long): Games?

    @Query("SELECT * FROM games")
    suspend fun getAllGames(): List<Games>

    @Query("SELECT * FROM games WHERE game_mode = :mode")
    suspend fun getGamesByMode(mode: GameModes): List<Games>

    @Query("SELECT * FROM games WHERE game_type = :type")
    suspend fun getGamesByType(type: GameTypes): List<Games>

    @Query("SELECT * FROM games WHERE players LIKE '%' || :playerName || '%'")
    suspend fun getGamesByPlayer(playerName: String): List<Games>

    @Query("SELECT * FROM games WHERE created_at BETWEEN :startTime AND :endTime")
    suspend fun getGamesByTimestampRange(startTime: Long, endTime: Long): List<Games>
}

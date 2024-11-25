package com.mayor.kavi.data.dao

import androidx.room.*
import java.time.LocalDateTime

@Entity(
    tableName = "game_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Games::class,
            parentColumns = ["gameId"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Users::class,
            parentColumns = ["userId"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GameSessions(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    @ColumnInfo(name = "game_id") val gameId: Long,
    @ColumnInfo(name = "player_id") val playerId: Long,
    @ColumnInfo(name = "scores") val scores: String,
    @ColumnInfo(name = "rolled_dice") val rolledDice: String,
    @ColumnInfo(name = "session_start") val sessionStart: LocalDateTime,
    @ColumnInfo(name = "session_end") val sessionEnd: LocalDateTime?
)

@Dao
interface GameSessionsDao {
    @Insert
    suspend fun insertSession(session: GameSessions)

    @Update
    suspend fun updateSession(session: GameSessions)

    @Delete
    suspend fun deleteSession(session: GameSessions)

    @Query("SELECT * FROM game_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: Long): GameSessions?

    @Query("SELECT * FROM game_sessions WHERE game_id = :gameId")
    suspend fun getSessionsByGameId(gameId: Long): List<GameSessions>
}

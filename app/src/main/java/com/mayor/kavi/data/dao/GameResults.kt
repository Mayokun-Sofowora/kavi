package com.mayor.kavi.data.dao

import androidx.room.*
import java.time.LocalDateTime

@Entity(
    tableName = "game_results",
    foreignKeys = [
        ForeignKey(
            entity = GameSessions::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GameResults(
    @PrimaryKey(autoGenerate = true) val resultId: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "final_score") val finalScore: String,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime
)

@Dao
interface GameResultsDao {
    @Insert
    suspend fun insertResult(result: GameResults)

    @Update
    suspend fun updateResult(result: GameResults)

    @Delete
    suspend fun deleteResult(result: GameResults)

    @Query("SELECT * FROM game_results WHERE resultId = :resultId")
    suspend fun getResultById(resultId: Long): GameResults?

    @Query("SELECT * FROM game_results WHERE session_id = :sessionId")
    suspend fun getResultsBySessionId(sessionId: Long): List<GameResults>
}

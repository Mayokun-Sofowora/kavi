package com.mayor.kavi.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "leaderboard_entries")
data class LeaderboardEntry(
    @PrimaryKey val rank: Int,
    @ColumnInfo(name = "player_name")val playerName: String,
    @ColumnInfo(name = "total_wins") val totalWins: Int,
    @ColumnInfo(name = "total_games") val totalGames: Int,
    @ColumnInfo(name = "level") val level: String
)

@Dao
interface LeaderboardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboardEntry(entry: LeaderboardEntry)

    @Query("SELECT * FROM leaderboard_entries WHERE rank = :rank")
    suspend fun getLeaderboardEntryByRank(rank: Int): LeaderboardEntry?

    @Query("SELECT * FROM leaderboard_entries ORDER BY total_wins DESC, total_games DESC LIMIT 10")
    fun getTopLeaderboardEntries(): Flow<List<LeaderboardEntry>> // Fetch top 10 players

    @Query("SELECT * FROM leaderboard_entries")
    fun getAllLeaderboardEntries(): Flow<List<LeaderboardEntry>>

    @Delete
    suspend fun deleteLeaderboardEntry(entry: LeaderboardEntry)
}


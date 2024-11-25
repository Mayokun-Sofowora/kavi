package com.mayor.kavi.data.dao

import androidx.room.*
import com.mayor.kavi.data.models.UserLevel
import com.mayor.kavi.data.models.Users
import com.mayor.kavi.data.models.GameModes
import com.mayor.kavi.data.models.ActionType
import java.time.LocalDateTime

@Entity(
    tableName = "user_statistics",
    foreignKeys = [
        ForeignKey(
            entity = Users::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserStatistics(
    @PrimaryKey(autoGenerate = true) val statisticsId: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "level") val level: UserLevel,
    @ColumnInfo(name = "action_type") val actionType: ActionType,
    @ColumnInfo(name = "total_games_played") var totalGamesPlayed: Int,
    @ColumnInfo(name = "total_games_won") var totalGamesWon: Int,
    @ColumnInfo(name = "total_games_lost") var totalGamesLost: Int,
    @ColumnInfo(name = "achievements") var achievements: String,
    @ColumnInfo(name = "highest_score") var highestScore: Int,
    @ColumnInfo(name = "average_score") var averageScore: Double,
    @ColumnInfo(name = "favorite_game_mode") var favoriteGameMode: GameModes,
    @ColumnInfo(name = "total_play_time") var totalPlayTime: Long,
    @ColumnInfo(name = "updated_at") var updatedAt: LocalDateTime
)

@Dao
interface UserStatisticsDao {
    @Insert
    suspend fun insertStatistics(statistics: UserStatistics)

    @Update
    suspend fun updateStatistics(statistics: UserStatistics)

    @Delete
    suspend fun deleteStatistics(statistics: UserStatistics)

    @Query("SELECT * FROM user_statistics WHERE user_id = :userId")
    suspend fun getStatisticsByUserId(userId: Long): UserStatistics?

    @Query("SELECT * FROM user_statistics")
    suspend fun getAllUserStatistics(): List<UserStatistics>
}

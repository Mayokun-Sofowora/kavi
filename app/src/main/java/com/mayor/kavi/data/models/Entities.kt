package com.mayor.kavi.data.models

import androidx.room.*
import com.mayor.kavi.utils.MyTypeConverters
import java.time.LocalDateTime

// Enum representing different types of games.
enum class GameTypes {
    LOCAL_MULTIPLAYER,
    COMPUTER_AI,
    // ONLINE_MULTIPLAYER
}

// Enum for the game mode of the different gameplay mechanics.
enum class GameModes {
    CLASSIC,
    VIRTUAL,
}

enum class UserLevel {
    BEGINNER,
    INTERMEDIATE,
    EXPERT
}

enum class ActionType {
    LOGIN,
    LOGOUT,
    START_GAME,
    END_GAME,
    ROLL,
    INVITE,
    ACCEPT_INVITE,
    DECLINE_INVITE
}

enum class FriendStatus {
    PENDING,
    ACCEPTED,
    BLOCKED
}

enum class Dice(val value: Int) {
    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6);

    companion object {
        // Converts a list of integers to corresponding dice values
        fun from(integers: List<Int>): List<Dice> {
            return integers.map { num -> Dice.entries[num - 1] }
        }
    }
}

// User data class holding essential user details.
@Entity(tableName = "users")
data class UsersEntity(
    @PrimaryKey(autoGenerate = true) val userId: Long = 0,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String?=null,
    @ColumnInfo(name = "is_guest") val isGuest: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

// Game data class holding all information about one game instance.
@Entity(tableName = "games")
data class GamesEntity(
    @PrimaryKey(autoGenerate = true) val gameId: Long = 0,
    @ColumnInfo(name = "game_type") val gameType: GameTypes,
    @ColumnInfo(name = "game_mode") val gameMode: GameModes,
    @ColumnInfo(name = "min_players") val minPlayers: Int,
    @ColumnInfo(name = "max_players") val maxPlayers: Int,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime
)

@Entity(
    tableName = "game_players",
    foreignKeys = [
        ForeignKey(
            entity = GamesEntity::class,
            parentColumns = ["gameId"],
            childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsersEntity::class,
            parentColumns = ["userId"],
            childColumns = ["player_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["game_id"]), Index(value = ["player_id"])]
)
data class GamePlayersEntity(
    @PrimaryKey(autoGenerate = true) val gamePlayerId: Long = 0,
    @ColumnInfo(name = "game_id") val gameId: Long,
    @ColumnInfo(name = "player_id") val userId: Long,
    @ColumnInfo(name = "device_id") val deviceId: String,  // Device or connection ID (for cross-device sync)
    @ColumnInfo(name = "is_host") val isHost: Boolean,  // To track if the player is the host
    @ColumnInfo(name = "is_ready") val isReady: Boolean = false,  // To track player's readiness in local multiplayer
    @ColumnInfo(name = "joined_at") val joinedAt: LocalDateTime
)

// Game session data class tracking each individual game session.
@Entity(
    tableName = "game_sessions",
    foreignKeys = [
        ForeignKey(
            entity = GamesEntity::class, parentColumns = ["gameId"], childColumns = ["game_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsersEntity::class, parentColumns = ["userId"], childColumns = ["player_id"],
            onDelete = ForeignKey.CASCADE
        )],
    indices = [Index(value = ["game_id"]), Index(value = ["player_id"])]
)
data class GameSessionsEntity(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    @ColumnInfo(name = "game_id") val gameId: Long,
    @ColumnInfo(name = "player_id") val playerId: Long,
    @TypeConverters(MyTypeConverters::class)
    @ColumnInfo(name = "scores") val scores: Map<ScoreType, Int>,
    @TypeConverters(MyTypeConverters::class)
    @ColumnInfo(name = "rolled_dice") val rolledDice: List<Dice>,
    @ColumnInfo(name = "session_start") val sessionStart: LocalDateTime,
    @ColumnInfo(name = "session_end") val sessionEnd: LocalDateTime?,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime
)

// Game result data class storing results for each game session.
@Entity(
    tableName = "game_results",
    foreignKeys = [
        ForeignKey(
            entity = GameSessionsEntity::class, parentColumns = ["sessionId"],
            childColumns = ["session_id"], onDelete = ForeignKey.CASCADE
        )],
    indices = [Index(value = ["session_id"])]
)
data class GameResultsEntity(
    @PrimaryKey(autoGenerate = true) val resultId: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "final_score") val finalScore: Map<String, Int>,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime
)

// User settings data class for managing user preferences.
@Entity(
    tableName = "user_settings",
    foreignKeys = [
        ForeignKey(
            entity = UsersEntity::class, parentColumns = ["userId"], childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )],
    indices = [Index(value = ["user_id"])]
)
data class UserSettingsEntity(
    @PrimaryKey(autoGenerate = true) val settingsId: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "settings_key") val settingsKey: String,
    @ColumnInfo(name = "settings_value") val settingsValue: String,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime
)

// User statistics data class for tracking individual user performance.
@Entity(
    tableName = "user_statistics",
    foreignKeys = [
        ForeignKey(
            entity = UsersEntity::class, parentColumns = ["userId"], childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )],
    indices = [Index(value = ["user_id"])]
)
data class UserStatisticsEntity(
    @PrimaryKey(autoGenerate = true) val statisticsId: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @TypeConverters(MyTypeConverters::class)
    @ColumnInfo(name = "level") val level: UserLevel, // Level of the player (Beginner, Intermediate, Expert),
    @ColumnInfo(name = "action_type") val actionType: ActionType,
    @ColumnInfo(name = "total_games_played") var totalGamesPlayed: Int,
    @ColumnInfo(name = "total_games_won") var totalGamesWon: Int,
    @ColumnInfo(name = "total_games_lost") var totalGamesLost: Int,
    @TypeConverters(MyTypeConverters::class)
    @ColumnInfo(name = "achievements") var achievements: Set<AchievementsEntity>, // mutable now
    @ColumnInfo(name = "highest_score") var highestScore: Int,
    @ColumnInfo(name = "average_score") var averageScore: Double,
    @TypeConverters(MyTypeConverters::class)
    @ColumnInfo(name = "favorite_game_mode") var favoriteGameMode: GameModes,
    @ColumnInfo(name = "total_play_time") var totalPlayTime: Long,
    @ColumnInfo(name = "updated_at") var updatedAt: LocalDateTime = LocalDateTime.now()
)

// Achievement class for tracking user achievements.
@Entity(
    tableName = "achievements",
    foreignKeys = [
        ForeignKey(
            entity = UsersEntity::class, parentColumns = ["userId"], childColumns = ["user_id"]
        )],
    indices = [Index(value = ["user_id"])]
)
data class AchievementsEntity(
    @PrimaryKey val achievementId: Long,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "unlocked") val unlocked: Boolean,
    @ColumnInfo(name = "icon_resource") val iconResource: Int
)

// Friends entity representing the social feature for users.
@Entity(
    tableName = "friends",
    foreignKeys = [
        ForeignKey(
            entity = UsersEntity::class,
            parentColumns = ["userId"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsersEntity::class,
            parentColumns = ["userId"],
            childColumns = ["friend_user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"]), Index(value = ["friend_user_id"])]
)
data class FriendsEntity(
    @PrimaryKey(autoGenerate = true) val friendId: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "friend_user_id") val friendUserId: Long,
    @TypeConverters(MyTypeConverters::class)
    @ColumnInfo(name = "status") val status: FriendStatus,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime = LocalDateTime.now(),
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime = LocalDateTime.now()
)
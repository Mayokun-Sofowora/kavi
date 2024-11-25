package com.mayor.kavi.data.models

import java.time.LocalDateTime

// User data class holding essential user details.
data class Users(
    val userId: Long,
    val username: String,
    val email: String,
    val passwordHash: String,
    val createdAt: Long,
    val updatedAt: Long
)

// Game data class holding all information about one game instance.
data class Games(
    val gameId: Long,
    val gameType: GameTypes,
    val gameMode: GameModes,
    val players: List<Users>,
    val minPlayers: Int,
    val maxPlayers: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

// Enum representing different types of games.
enum class GameTypes {
    LOCAL_MULTIPLAYER, // Local multiplayer
    COMPUTER_AI, // Against an AI
    // ONLINE_MULTIPLAYER // Online multiplayer (future feature)
}

// Enum for the game mode of the different gameplay mechanics.
enum class GameModes {
    CLASSIC, // Traditional dice rolling
    VIRTUAL, // Includes image recognition and AR features
}

// Game session data class tracking each individual game session.
data class GameSessions(
    val sessionId: Long,
    val gameId: Long,
    val playerId: Long,
    val scores: Map<ScoreType, Int>,
    val rolledDice: List<Dice>,
    val sessionStart: LocalDateTime,
    val sessionEnd: LocalDateTime?
)

// Game result data class storing results for each game session.
data class GameResults(
    val resultId: Long,
    val sessionId: Long,
    val finalScore: Map<String, Int>,
    val createdAt: LocalDateTime
)

// User settings data class for managing user preferences.
data class UserSettings(
    val settingsId: Long,
    val userId: Long,
    val settingsKey: String,
    val settingsValue: String,
    val updatedAt: LocalDateTime
)

// User statistics data class for tracking individual user performance.
data class UserStatistics(
    val statisticsId: Long,
    val userId: Long,
    val level: UserLevel, // Level of the player (Beginner, Intermediate, Expert)
    val actionType: ActionType,
    val totalGamesPlayed: Int,
    val totalGamesWon: Int,
    val totalGamesLost: Int,
    val achievements: Set<Achievement>,
    val highestScore: Int,
    val averageScore: Double,
    val favoriteGameMode: GameModes,
    val totalPlayTime: Long, // Total time spent playing the game (in milliseconds)
    val updatedAt: LocalDateTime
)

enum class UserLevel {
    BEGINNER, INTERMEDIATE, EXPERT
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

// Achievement class for tracking user achievements.
data class Achievement(
    val achievementId: Long,
    val userId: Long,
    val name: String,
    val description: String,
    val unlocked: Boolean,
    val iconResource: Int
)

data class Friends(
    val friendId: Long,
    val userId: Long,
    val friendUserId: Long,
    val status: FriendStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

enum class FriendStatus {
    PENDING,
    ACCEPTED,
    BLOCKED
}

// Future Entities (for AI Models, AR Assets, etc.) will be similarly defined.
// Simplified database definitions:
// 1. Users
// 2. Dice
// 3. Games
// 4. Game Sessions
// 5. Game Results
// More entities to be added based on the project requirements.

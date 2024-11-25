package com.mayor.kavi.utils

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mayor.kavi.data.models.*
import java.time.LocalDateTime

/**
 * TypeConverter class to handle all custom data types for Room database serialization and deserialization.
 */
class MyTypeConverters {

    private val gson = Gson()

    // For Map<ScoreType, Int>
    @TypeConverter
    fun fromScoreTypeIntMap(value: Map<ScoreType, Int>?): String? {
        return value?.let {
            // Convert Map<ScoreType, Int> to a JSON string
            gson.toJson(value)
        }
    }

    @TypeConverter
    fun toScoreTypeIntMap(value: String?): Map<ScoreType, Int>? {
        return value?.let {
            // Deserialize the JSON string back to a Map<ScoreType, Int>
            val type = object : TypeToken<Map<ScoreType, Int>>() {}.type
            gson.fromJson(value, type)
        }
    }

    // For Map<String, Int>
    @TypeConverter
    fun fromStringIntMap(value: Map<String, Int>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringIntMap(value: String?): Map<String, Int>? {
        return value?.let {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            gson.fromJson(it, type)
        }
    }

    // For List<Dice>
    @TypeConverter
    fun fromDiceList(value: List<Dice>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toDiceList(value: String?): List<Dice>? {
        return value?.let {
            val type = object : TypeToken<List<Dice>>() {}.type
            gson.fromJson(it, type)
        }
    }

    // For List<Users>
    @TypeConverter
    fun fromUsersList(users: List<UsersEntity>?): String? {
        return if (users == null) null else gson.toJson(users)
    }

    @TypeConverter
    fun toUsersList(usersString: String?): List<UsersEntity>? {
        return if (usersString == null) null else gson.fromJson(
            usersString,
            object : TypeToken<List<UsersEntity>>() {}.type
        )
    }

    // For Set<Achievement>
    @TypeConverter
    fun fromAchievementSet(value: Set<AchievementsEntity>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toAchievementSet(value: String?): Set<AchievementsEntity>? {
        return value?.let {
            val type = object : TypeToken<Set<AchievementsEntity>>() {}.type
            gson.fromJson(it, type)
        }
    }

//    // For Enums (Specific to ScoreType)
//    @TypeConverter
//    fun fromScoreTypeToString(value: ScoreType?): String? {
//        return value?.name
//    }
//
//    @TypeConverter
//    fun fromStringToScoreType(value: String?): ScoreType? {
//        return value?.let { ScoreType.valueOf(it) }
//    }
//
//    // For Enums (Specific to GameTypes)
//    @TypeConverter
//    fun fromGameTypeToString(value: GameTypes?): String? {
//        return value?.name
//    }
//
//    @TypeConverter
//    fun fromStringToGameType(value: String?): GameTypes? {
//        return value?.let { GameTypes.valueOf(it) }
//    }
//
//    // For Enums (Specific to GameModes)
//    @TypeConverter
//    fun fromGameModeToString(value: GameModes?): String? {
//        return value?.name
//    }
//
//    @TypeConverter
//    fun toGameMode(value: String?): GameModes? {
//        return value?.let { GameModes.valueOf(it) }
//    }
//
//    // For Enums (Specific to UserLevel)
//    @TypeConverter
//    fun fromUserLevelToString(value: UserLevel?): String? {
//        return value?.name
//    }
//
//    @TypeConverter
//    fun toUserLevel(value: String?): UserLevel? {
//        return value?.let { UserLevel.valueOf(it) }
//    }
//
//    // For Enums (Specific to ActionType)
//    @TypeConverter
//    fun fromActionTypeToString(value: ActionType?): String? {
//        return value?.name
//    }
//
//    // For Enums (Specific to FriendStatus)
//    @TypeConverter
//    fun fromFriendStatusToString(value: FriendStatus?): String? {
//        return value?.name
//    }
//
//    @TypeConverter
//    fun fromStringToFriendStatus(value: String?): FriendStatus? {
//        return value?.let { FriendStatus.valueOf(it) }
//    }
//
//    @TypeConverter
//    fun toActionType(value: String?): ActionType? {
//        return value?.let { ActionType.valueOf(it) }
//    }
//
//    // For LocalDateTime
//    @TypeConverter
//    fun fromLocalDateTime(value: LocalDateTime?): Long? {
//        return value?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
//    }

    @TypeConverter
    fun toLocalDateTime(value: Long?): LocalDateTime? {
        return value?.let {
            LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(it),
                java.time.ZoneId.systemDefault()
            )
        }
    }
}
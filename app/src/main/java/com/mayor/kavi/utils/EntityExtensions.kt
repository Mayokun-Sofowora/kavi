package com.mayor.kavi.utils
//
//import com.google.gson.Gson
//import com.google.gson.reflect.TypeToken
//import com.mayor.kavi.data.dao.*
//import com.mayor.kavi.data.models.*
//
//// Convert UserEntity to User
//fun Users.toUser(): Users {
//    return try {
//        User(
//            id = this.id,
//            username = this.username,
//            preferences = this.preferences
//        )
//    } catch (exception: Exception) {
//        // Fallback in case of deserialization failure
//        User(id = this.id, username = this.username, preferences = this.preferences)
//    }
//}
//
//// Convert User to UserEntity
//fun User.toEntity(): UserEntity {
//    return UserEntity(
//        id = this.id,
//        username = this.username,
//        preferences = Gson().toJson(this.preferences) // Save preferences as JSON string in the database
//    )
//}
//
//// Convert Game to GameEntity
//fun Game.toEntity(): GameEntity {
//    return GameEntity(
//        id = this.id,
//        players = this.players, // Keep List<String> as is, Room will handle it with TypeConverters
//        score = this.score, // Keep Map<String, Int> as is, Room will handle it with TypeConverters
//        diceValues = this.diceValues,
//        lockedDice = this.lockedDice,
//        remainingRolls = this.remainingRolls,
//        gameMode = this.gameMode, // Store GameMode enum directly, Room will handle it with TypeConverters
//        gameType = this.gameType, // Store GameType enum directly, Room will handle it with TypeConverters
//        imageRecognitionMode = this.imageRecognitionMode,
//        recognizedDiceValues = this.recognizedDiceValues,
//        currentPlayerIndex = this.currentPlayerIndex,
//        timestamp = this.timestamp
//    )
//}
//
//fun GameEntity.toDomain(): Game {
//    return try {
//        // Room handles the conversion automatically for List<String> and Map<String, Int>
//        val playersList: List<String> = this.players // Already a List<String>, Room handles it
//        val scoreMap: Map<String, Int> = this.score // Already a Map<String, Int>, Room handles it
//        val diceValues: List<Int> = this.diceValues // Room handles List<Int> conversion
//        val lockedDice: List<Boolean> = this.lockedDice // Room handles List<Boolean> conversion
//        val recognizedDiceValues: List<Int>? =
//            this.recognizedDiceValues // Room handles List<Int> or null
//
//        // Convert GameMode and GameType enums
//        val gameMode = this.gameMode // Already a GameMode, Room handles it
//        val gameType = this.gameType // Already a GameType, Room handles it
//
//        // Return the Game domain object
//        Game(
//            id = this.id,
//            players = playersList,
//            score = scoreMap,
//            diceValues = diceValues,
//            lockedDice = lockedDice,
//            remainingRolls = this.remainingRolls,
//            gameMode = gameMode,
//            imageRecognitionMode = this.imageRecognitionMode,
//            recognizedDiceValues = recognizedDiceValues,
//            timestamp = this.timestamp,
//            currentPlayerIndex = this.currentPlayerIndex,
//            gameType = gameType,
//            isPlayerWinner = { playerName -> false },
//            isPlayersTurn = { playerName -> false }
//        )
//    } catch (exception: Exception) {
//        // In case of failure, return a fallback Game object with default values
//        Game(
//            id = this.id,
//            players = emptyList(),
//            score = emptyMap(),
//            timestamp = this.timestamp,
//            diceValues = emptyList(),
//            lockedDice = emptyList(),
//            remainingRolls = 0,
//            gameMode = GameMode.CLASSIC, // Default to CLASSIC
//            imageRecognitionMode = false,
//            recognizedDiceValues = null,
//            currentPlayerIndex = 0,
//            gameType = GameType.LOCAL_MULTIPLAYER,
//            isPlayerWinner = {playerName -> false},
//            isPlayersTurn = {playerName -> false}
//        )
//    }
//}
//
//fun PlayerStats.toEntity(): PlayerStatsEntity {
//    return PlayerStatsEntity(
//        playerId = this.playerId,
//        level = this.level,
//        totalGamesPlayed = this.totalGamesPlayed,
//        totalGamesWon = this.totalGamesWon,
//        achievements = serializeAchievements(achievements),
//        highestScore = this.highestScore,
//        averageScore = this.averageScore,
//        favoriteGameMode = this.favoriteGameMode,
//        totalPlayTime = this.totalPlayTime
//    )
//}
//
//fun PlayerStatsEntity.toDomain(): PlayerStats {
//    return PlayerStats(
//        playerId = this.playerId,
//        level = this.level,
//        totalGamesPlayed = this.totalGamesPlayed,
//        totalGamesWon = this.totalGamesWon,
//        achievements = deserializeAchievements(achievements),
//        highestScore = this.highestScore,
//        averageScore = this.averageScore,
//        favoriteGameMode = GameMode.valueOf(this.favoriteGameMode.toString()),
//        totalPlayTime = this.totalPlayTime,
//    )
//}
//
//// Deserialize JSON string to Set<Achievement>
//fun deserializeAchievements(json: String): Set<Achievement> {
//    return Gson().fromJson(json, object : TypeToken<Set<Achievement>>() {}.type)
//}
//
//// Serialize Set<Achievement> to JSON string
//fun serializeAchievements(achievements: Set<Achievement>): String {
//    return Gson().toJson(achievements)
//}

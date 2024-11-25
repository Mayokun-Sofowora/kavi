package com.mayor.kavi.data.repository.fakes

import com.mayor.kavi.data.dao.PlayerStatsEntity
import com.mayor.kavi.data.repository.GameRepository
import com.mayor.kavi.data.models.Game
import com.mayor.kavi.data.models.GameMode
import com.mayor.kavi.data.models.GameType
import com.mayor.kavi.data.models.PlayerStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeGameRepository : GameRepository {

    private val games = MutableStateFlow<List<Game>>(emptyList())
    private val playerStatsMap =
        mutableMapOf<String, PlayerStatsEntity>() // Simulating player stats storage

    override fun getAllGames(): Flow<List<Game>> = games

    override suspend fun saveGame(game: Game) {
        games.update { currentGames ->
            val existingGame = currentGames.find { it.id == game.id }
            if (existingGame != null) {
                currentGames - existingGame + game
            } else {
                currentGames + game
            }
        }
    }

    override suspend fun deleteGame(gameId: Long) {
        games.update { currentGames ->
            currentGames.filterNot { it.id == gameId }
        }
    }

    override suspend fun getGameById(gameId: Long): Game? {
        return games.value.find { it.id == gameId }
    }

    // New functions to implement the filtering functionality:

    override suspend fun getLatestGame(): Game? {
        return games.value.maxByOrNull { it.timestamp }
    }

    override suspend fun countGamesByMode(mode: GameMode): Int {
        return games.value.count { it.gameMode == mode }
    }

    override fun getGamesByMode(mode: GameMode): Flow<List<Game>> {
        return MutableStateFlow(games.value.filter { it.gameMode == mode })
    }

    override fun getGamesByType(type: GameType): Flow<List<Game>> {
        return MutableStateFlow(games.value.filter { it.gameType == type })
    }

    override fun getGamesByPlayer(playerName: String): Flow<List<Game>> {
        return MutableStateFlow(games.value.filter { playerName in it.players })
    }

    override fun getGamesByTimestampRange(startTime: Long, endTime: Long): Flow<List<Game>> {
        return MutableStateFlow(games.value.filter { it.timestamp in startTime..endTime })
    }

    override fun getGamesByMultipleFilters(
        mode: GameMode,
        type: GameType,
        playerName: String
    ): Flow<List<Game>> {
        return MutableStateFlow(games.value.filter {
            it.gameMode == mode && it.gameType == type && playerName in it.players
        })
    }

    override fun getGamesByPlayerAndMode(playerName: String, mode: GameMode): Flow<List<Game>> {
        return MutableStateFlow(games.value.filter {
            it.gameMode == mode && playerName in it.players
        })
    }

    override suspend fun deletePlayerStats(playerName: String) {
        games.update { currentGames ->
            currentGames.map { game ->
                game.copy(score = game.score.filterKeys { it != playerName })
            }
        }
    }

    override fun getPlayerStats(playerNames: List<String>): Flow<PlayerStats> {
        val relevantGames = games.value.filter { game ->
            playerNames.any { it in game.players }
        }

        val totalGamesPlayed = relevantGames.size
        val totalGamesWon = relevantGames.count { game ->
            game.score.maxByOrNull { it.value }?.key in playerNames
        }
        val highestScore = relevantGames.flatMap { it.score.values }.maxOrNull() ?: 0
        val averageScore = relevantGames.flatMap { it.score.values }.average()
        val favoriteGameMode =
            relevantGames.groupingBy { it.gameMode }.eachCount().maxByOrNull { it.value }?.key
                ?: GameMode.CLASSIC

        return MutableStateFlow(
            PlayerStats(
                playerId = playerNames.joinToString(","),
                level = "Intermediate", // Default logic for demo
                totalGamesPlayed = totalGamesPlayed,
                totalGamesWon = totalGamesWon,
                achievements = emptySet(),
                highestScore = highestScore,
                averageScore = averageScore,
                favoriteGameMode = favoriteGameMode,
                totalPlayTime = 0L // Placeholder for demo
            )
        )
    }

    override suspend fun updatePlayerStats(game: Game) {
        // Update stats for each player in the game
        game.score.forEach { (playerName, score) ->
            // Check if player stats already exist
            val existingStats = playerStatsMap[playerName]

            if (existingStats != null) {
                // If the player exists, update their statistics
                val newTotalGamesPlayed = existingStats.totalGamesPlayed + 1
                val newTotalGamesWon = if (game.isPlayerWinner(playerName)) {
                    existingStats.totalGamesWon + 1
                } else {
                    existingStats.totalGamesWon
                }
                val newHighestScore = maxOf(existingStats.highestScore, score)
                val newAverageScore =
                    ((existingStats.averageScore * existingStats.totalGamesPlayed) + score) / newTotalGamesPlayed

                // Update the player stats in the map
                playerStatsMap[playerName] = existingStats.copy(
                    totalGamesPlayed = newTotalGamesPlayed,
                    totalGamesWon = newTotalGamesWon,
                    highestScore = newHighestScore,
                    averageScore = newAverageScore,
                    favoriteGameMode = game.gameMode // You can adjust the logic for determining the favorite mode
                )
            } else {
                // If no stats exist, create new PlayerStatsEntity
                val newStats = PlayerStatsEntity(
                    playerId = playerName,
                    level = "Beginner", // Default logic, can be improved later
                    totalGamesPlayed = 1,
                    totalGamesWon = if (game.isPlayerWinner(playerName)) 1 else 0,
                    highestScore = score,
                    averageScore = score.toDouble(),
                    favoriteGameMode = game.gameMode,
                    totalPlayTime = 0L, // Placeholder for demo
                    achievements = "" // TODO: Achievements logic can be added later
                )
                playerStatsMap[playerName] = newStats // Save new player stats in the map
            }
        }
    }

    // TODO: Additional methods to retrieve player stats for testing purposes
    fun getPlayerStats(playerName: String): PlayerStatsEntity? {
        return playerStatsMap[playerName]
    }

}

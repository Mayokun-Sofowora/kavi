package com.mayor.kavi.data.repository.fakes

import com.mayor.kavi.data.models.*
import com.mayor.kavi.data.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime

class FakeGameRepository : GameRepository {

    private val games = mutableListOf<GamesEntity>()
    private val gameFlow = MutableStateFlow<List<GamesEntity>>(emptyList())
    private val userStatistics = mutableMapOf<Long, UserStatisticsEntity>()
    private var gameIdCounter = 1L
    private var statisticsIdCounter = 1L

    override fun getAllGames(): Flow<List<GamesEntity>> {
        return gameFlow.asStateFlow()
    }

    override suspend fun saveGame(game: GamesEntity) {
        val newGame = if (game.gameId == 0L) {
            game.copy(gameId = gameIdCounter++)
        } else {
            game
        }
        games.removeIf { it.gameId == newGame.gameId }
        games.add(newGame)
        emitGames()
    }

    override suspend fun deleteGame(gameId: Long) {
        games.removeIf { it.gameId == gameId }
        emitGames()
    }

    override suspend fun getGameById(gameId: Long): GamesEntity? {
        return games.find { it.gameId == gameId }
    }

    override suspend fun getLatestGame(): GamesEntity? {
        return games.maxByOrNull { it.createdAt }
    }

    override suspend fun countGamesByMode(mode: GameModes): Int {
        return games.count { it.gameMode == mode }
    }

    override fun getGamesByMode(mode: GameModes): Flow<List<GamesEntity>> {
        return flow {
            emit(games.filter { it.gameMode == mode })
        }
    }

    override fun getGamesByType(type: GameTypes): Flow<List<GamesEntity>> {
        return flow {
            emit(games.filter { it.gameType == type })
        }
    }

    override fun getGamesByModeAndType(
        mode: GameModes,
        type: GameTypes,
        playerName: String
    ): Flow<List<GamesEntity>> {
        return flow {
            emit(
                games.filter {
                    it.gameMode == mode && it.gameType == type
                    // Assuming `players` is a property in the game entity,
                    // replace `.players.contains(playerName)` with relevant logic if needed.
                }
            )
        }
    }

    override suspend fun deletePlayerStats(playerName: String) {
        val playerStat = userStatistics.values.find { it.userId.toString() == playerName }
        playerStat?.let { userStatistics.remove(it.userId) }
    }

    override fun getPlayerStats(playerNames: List<String>): Flow<List<UserStatisticsEntity>> {
        return flow {
            emit(userStatistics.values.filter { playerNames.contains(it.userId.toString()) })
        }
    }

    override suspend fun updatePlayerStats(game: GamesEntity, playerId: Long, gameWon: Boolean) {
        val stats = userStatistics[playerId] ?: UserStatisticsEntity(
            userId = playerId,
            totalGamesPlayed = 0,
            totalGamesWon = 0,
            totalGamesLost = 0,
            statisticsId = statisticsIdCounter++,
            level = UserLevel.BEGINNER,
            actionType = ActionType.END_GAME,
            achievements = emptySet(),
            highestScore = 0,
            averageScore = 0.0,
            favoriteGameMode = game.gameMode,
            totalPlayTime = 0L,
            updatedAt = LocalDateTime.now()
        )

        stats.totalGamesPlayed++
        if (gameWon) stats.totalGamesWon++ else stats.totalGamesLost++
        stats.updatedAt = LocalDateTime.now()

        userStatistics[playerId] = stats
    }

    private fun emitGames() {
        gameFlow.value = games.toList()
    }
}

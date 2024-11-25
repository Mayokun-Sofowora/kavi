package com.mayor.kavi.data.repository

import com.mayor.kavi.data.dao.*
import com.mayor.kavi.data.models.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GameRepository {

    fun getAllGames(): Flow<List<GamesEntity>>

    suspend fun saveGame(game: GamesEntity)

    suspend fun deleteGame(gameId: Long)

    suspend fun getGameById(gameId: Long): GamesEntity?

    suspend fun getLatestGame(): GamesEntity?

    suspend fun countGamesByMode(mode: GameModes): Int

    fun getGamesByMode(mode: GameModes): Flow<List<GamesEntity>>

    fun getGamesByType(type: GameTypes): Flow<List<GamesEntity>>

    fun getGamesByModeAndType(
        mode: GameModes,
        type: GameTypes,
        playerName: String
    ): Flow<List<GamesEntity>>

    suspend fun deletePlayerStats(playerName: String)

    fun getPlayerStats(playerNames: List<String>): Flow<List<UserStatisticsEntity>>

    suspend fun updatePlayerStats(game: GamesEntity, playerId: Long, gameWon: Boolean)
}

class GameRepositoryImpl @Inject constructor(
    private val gameDao: GamesDao,
    private val userDao: UsersDao,
    private val userStatisticsDao: UserStatisticsDao
) : GameRepository {

    override fun getAllGames(): Flow<List<GamesEntity>> {
        return flow {
            emit(gameDao.getAllGames())
        }
    }

    override suspend fun saveGame(game: GamesEntity) {
        gameDao.insertGame(game)
    }

    override suspend fun deleteGame(gameId: Long) {
        val game = gameDao.getGameById(gameId)
        game?.let { gameDao.deleteGame(it) }
    }

    override suspend fun getGameById(gameId: Long): GamesEntity? {
        return gameDao.getGameById(gameId)
    }

    override suspend fun getLatestGame(): GamesEntity? {
        val allGames = gameDao.getAllGames()
        return allGames.lastOrNull()
    }

    override suspend fun countGamesByMode(mode: GameModes): Int {
        val games = gameDao.getGamesByMode(mode)
        return games.size // Emitting count of games
    }

    override fun getGamesByMode(mode: GameModes): Flow<List<GamesEntity>> {
        return flow {
            emit(gameDao.getGamesByMode(mode))
        }
    }

    override fun getGamesByType(type: GameTypes): Flow<List<GamesEntity>> {
        return flow {
            emit(gameDao.getGamesByType(type))
        }
    }

    override fun getGamesByModeAndType(
        mode: GameModes,
        type: GameTypes,
        playerName: String
    ): Flow<List<GamesEntity>> {
        return flow {
            val gamesByMode = gameDao.getGamesByMode(mode)
            val gamesByType = gameDao.getGamesByType(type)

            // Combine and eliminate duplicates
            val combinedGames = (gamesByMode + gamesByType).distinct()
            emit(combinedGames)
        }
    }

    override suspend fun deletePlayerStats(playerName: String) {
        // Fetch all user statistics
        val userStat = userStatisticsDao.getAllUserStatistics().find {
            it.userId.toString() == playerName
        }
        userStat?.let { userStatisticsDao.deleteStatistics(it) }
    }

    override fun getPlayerStats(playerNames: List<String>): Flow<List<UserStatisticsEntity>> = flow {
        val userStats = playerNames.mapNotNull { userStatisticsDao.getStatisticsByUserId(it.toLong()) }
        emit(userStats)
    }

    override suspend fun updatePlayerStats(game: GamesEntity, playerId: Long, gameWon: Boolean) {
        // Fetch user and user statistics
        val user = userDao.getUserById(playerId)
        val userStat = userStatisticsDao.getStatisticsByUserId(playerId)

        // Ensure userStat is not null for updates
        userStat?.let {
            // Update total games played
            it.totalGamesPlayed++
            // Update the won and lost counts based on the result
            if (gameWon) {
                it.totalGamesWon++
            } else {
                it.totalGamesLost++
            }

            // Update user statistics in the database
            userStatisticsDao.updateStatistics(it)

            // Optionally update the user (if necessary)
            user?.let { userDao.updateUser(it) }
        }
    }
}

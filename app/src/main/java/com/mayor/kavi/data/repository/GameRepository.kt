package com.mayor.kavi.data.repository

import com.mayor.kavi.data.dao.GameDao
import com.mayor.kavi.data.dao.UserStatisticsDao
import com.mayor.kavi.data.dao.Games
import com.mayor.kavi.data.dao.UserDao
import com.mayor.kavi.data.models.GameModes
import com.mayor.kavi.data.models.GameTypes
import com.mayor.kavi.data.dao.UserStatistics
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface GameRepository {

    fun getAllGames(): Flow<List<Games>>

    suspend fun saveGame(game: Games)

    suspend fun deleteGame(gameId: Long)

    suspend fun getGameById(gameId: Long): Games?

    suspend fun getLatestGame(): Games?

    suspend fun countGamesByMode(mode: GameModes): Int

    fun getGamesByMode(mode: GameModes): Flow<List<Games>>

    fun getGamesByType(type: GameTypes): Flow<List<Games>>

    fun getGamesByPlayer(playerName: String): Flow<List<Games>>

    fun getGamesByTimestampRange(startTime: Long, endTime: Long): Flow<List<Games>>

    fun getGamesByMultipleFilters(
        mode: GameModes,
        type: GameTypes,
        playerName: String
    ): Flow<List<Games>>

    fun getGamesByPlayerAndMode(playerName: String, mode: GameModes): Flow<List<Games>>

    suspend fun deletePlayerStats(playerName: String)

    fun getPlayerStats(playerNames: List<String>): Flow<List<UserStatistics>>

    suspend fun updatePlayerStats(game: Games, playerId: Long, gameWon: Boolean)
}

class GameRepositoryImpl @Inject constructor(
    private val gameDao: GameDao,
    private val userDao: UserDao,
    private val userStatisticsDao: UserStatisticsDao
) : GameRepository {

    override fun getAllGames(): Flow<List<Games>> {
        return flow {
            emit(gameDao.getAllGames())
        }
    }

    override suspend fun saveGame(game: Games) {
        gameDao.insertGame(game)
    }

    override suspend fun deleteGame(gameId: Long) {
        val game = gameDao.getGameById(gameId)
        game?.let { gameDao.deleteGame(it) }
    }

    override suspend fun getGameById(gameId: Long): Games? {
        return gameDao.getGameById(gameId)
    }

    override suspend fun getLatestGame(): Games? {
        val allGames = gameDao.getAllGames()
        return allGames.lastOrNull()
    }

    override suspend fun countGamesByMode(mode: GameModes): Int {
        val games = gameDao.getGamesByMode(mode)
        return games.size
    }

    override fun getGamesByMode(mode: GameModes): Flow<List<Games>> {
        return flow {
            emit(gameDao.getGamesByMode(mode))
        }
    }

    override fun getGamesByType(type: GameTypes): Flow<List<Games>> {
        return flow {
            emit(gameDao.getGamesByType(type))
        }
    }

    override fun getGamesByPlayer(playerName: String): Flow<List<Games>> {
        return flow {
            emit(gameDao.getGamesByPlayer(playerName))
        }
    }

    override fun getGamesByTimestampRange(startTime: Long, endTime: Long): Flow<List<Games>> {
        return flow {
            emit(gameDao.getGamesByTimestampRange(startTime, endTime))
        }
    }

    override fun getGamesByMultipleFilters(
        mode: GameModes,
        type: GameTypes,
        playerName: String
    ): Flow<List<Games>> {
        return flow {
            val games =
                gameDao.getGamesByMode(mode) + gameDao.getGamesByType(type) + gameDao.getGamesByPlayer(
                    playerName
                )
            emit(games.distinct())  // Avoid duplicates
        }
    }

    override fun getGamesByPlayerAndMode(playerName: String, mode: GameModes): Flow<List<Games>> {
        return flow {
            emit(gameDao.getGamesByPlayer(playerName).filter { it.gameMode == mode })
        }
    }

    override suspend fun deletePlayerStats(playerName: String) {
        // Fetch all user statistics
        val userStat = userStatisticsDao.getAllUserStatistics().find {
            it.userId.toString() == playerName
        }
        userStat?.let { userStatisticsDao.deleteStatistics(it) }
    }

    override fun getPlayerStats(playerNames: List<String>): Flow<List<UserStatistics>> = flow {
        val userStats =
            playerNames.mapNotNull { userStatisticsDao.getStatisticsByUserId(it.toLong()) }
        emit(userStats)
    }

    override suspend fun updatePlayerStats(game: Games, playerId: Long, gameWon: Boolean) {
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
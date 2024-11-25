package com.mayor.kavi.data.repository

import com.mayor.kavi.data.dao.GamePlayersDao
import com.mayor.kavi.data.models.GamePlayersEntity
import javax.inject.Inject

interface GamePlayerRepository {
    suspend fun addGamePlayer(gamePlayer: GamePlayersEntity)
    suspend fun updateGamePlayer(gamePlayer: GamePlayersEntity)
    suspend fun removeGamePlayer(gamePlayer: GamePlayersEntity)
    suspend fun getPlayersByGame(gameId: Long): List<GamePlayersEntity>
    suspend fun getPlayerByUser(userId: Long): List<GamePlayersEntity>
    suspend fun getPlayerByDevice(deviceId: String): List<GamePlayersEntity>
    suspend fun getHostForGame(gameId: Long): GamePlayersEntity?
    suspend fun getReadyPlayersForGame(gameId: Long): List<GamePlayersEntity>
    suspend fun setPlayerReady(gameId: Long, userId: Long)
}

class GamePlayersRepositoryImpl @Inject constructor(
    private val gamePlayersDao: GamePlayersDao
) : GamePlayerRepository {

    override suspend fun addGamePlayer(gamePlayer: GamePlayersEntity) {
        gamePlayersDao.insertGamePlayer(gamePlayer)
    }

    override suspend fun updateGamePlayer(gamePlayer: GamePlayersEntity) {
        gamePlayersDao.updateGamePlayer(gamePlayer)
    }

    override suspend fun removeGamePlayer(gamePlayer: GamePlayersEntity) {
        gamePlayersDao.deleteGamePlayer(gamePlayer)
    }

    override suspend fun getPlayersByGame(gameId: Long): List<GamePlayersEntity> {
        return gamePlayersDao.getPlayersByGame(gameId)
    }

    override suspend fun getPlayerByUser(userId: Long): List<GamePlayersEntity> {
        return gamePlayersDao.getPlayerByUser(userId)
    }

    override suspend fun getPlayerByDevice(deviceId: String): List<GamePlayersEntity> {
        return gamePlayersDao.getPlayerByDevice(deviceId)
    }

    override suspend fun getHostForGame(gameId: Long): GamePlayersEntity? {
        return gamePlayersDao.getHostForGame(gameId)
    }

    override suspend fun getReadyPlayersForGame(gameId: Long): List<GamePlayersEntity> {
        return gamePlayersDao.getReadyPlayersForGame(gameId)
    }

    override suspend fun setPlayerReady(gameId: Long, userId: Long) {
        gamePlayersDao.setPlayerReady(gameId, userId)
    }
}

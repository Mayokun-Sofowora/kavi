package com.mayor.kavi.data.repository

import com.mayor.kavi.data.models.UserProfile
import com.mayor.kavi.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * This repository handles the interaction with the backend for user-related operations.
 */
interface UserRepository {
    fun getCurrentUserId(): String?
    suspend fun getUserById(userId: String): Result<UserProfile>
    suspend fun updateUserProfile(profile: UserProfile): Result<UserProfile>
    suspend fun getCurrentUser(): Result<UserProfile>
    suspend fun getAllPlayers(): Result<List<UserProfile>>
    suspend fun setUserOnlineStatus(isOnline: Boolean): Result<Unit>
    suspend fun setUserGameStatus(
        isInGame: Boolean,
        isWaitingForPlayers: Boolean,
        gameId: String
    ): Result<Unit>

    fun listenForOnlinePlayers(): Flow<List<UserProfile>>
}
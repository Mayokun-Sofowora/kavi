package com.mayor.kavi.data.repository

import com.mayor.kavi.data.models.UserProfile
import com.mayor.kavi.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * This repository handles the interaction with the backend for user-related operations.
 */
interface UserRepository {
    fun getCurrentUserId(): String?
    suspend fun getUserById(userId: String): Result<UserProfile>
    suspend fun updateUserProfile(profile: UserProfile): Result<UserProfile>
    suspend fun getCurrentUser(): Result<UserProfile>
}
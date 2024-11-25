package com.mayor.kavi.data.service

import com.mayor.kavi.data.models.UsersEntity
import com.mayor.kavi.data.repository.UserRepository
import java.util.UUID
import javax.inject.Inject

interface AuthService {
    suspend fun signInAsGuest(): UsersEntity
    fun createLocalUser(userName: String): UsersEntity
    fun getFriendCode(userId: Long): String
}

class AuthServiceImpl @Inject constructor(private val userRepository: UserRepository) : AuthService {

    override suspend fun signInAsGuest(): UsersEntity {
        val userId = generateUserId()
        val user = UsersEntity(
            userId = userId,
            username = "Guest_$userId",
            email = "",
            isGuest = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        userRepository.saveUser(user) // Persist user
        return user
    }

    override fun createLocalUser(
        userName: String
    ): UsersEntity {
        return UsersEntity(
            userId = generateUserId(),
            username = userName,
            email = "",
            passwordHash = "",
            isGuest = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    override fun getFriendCode(userId: Long): String {
        return userId.toString(36).take(6) // Base-36 encoding for shorter codes
    }

    private fun generateUserId(): Long {
        return UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE
    }
}

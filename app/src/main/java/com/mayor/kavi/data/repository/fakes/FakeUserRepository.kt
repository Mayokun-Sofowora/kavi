package com.mayor.kavi.data.repository.fakes

import com.mayor.kavi.utils.Result
import com.mayor.kavi.data.dao.UserEntity
import com.mayor.kavi.data.repository.UserRepository
import com.mayor.kavi.data.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeUserRepository : UserRepository {
    private val users = mutableListOf<UserEntity>()
    private val currentUser = MutableStateFlow<User?>(null)

    override suspend fun saveUser(user: UserEntity): Result<Unit> {
        val existingUser = users.find { it.id == user.id }
        if (existingUser != null) {
            users.remove(existingUser)
        }
        users.add(user)
        return Result.Success(Unit)
    }

    override suspend fun getUserById(userId: Long): Result<UserEntity> {
        return users.find { it.id == userId }?.let { Result.Success(it) }
            ?: Result.Error(Exception("User not found"))
    }

    override suspend fun updateUser(user: UserEntity): Result<Unit> {
        val existingUserIndex = users.indexOfFirst { it.id == user.id }
        return if (existingUserIndex != -1) {
            users[existingUserIndex] = user
            Result.Success(Unit)
        } else {
            Result.Error(Exception("User not found"))
        }
    }

    override suspend fun clearCurrentUser(): Result<Unit> {
        return try {
            currentUser.value = null
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteUser(userId: Long): Result<Unit> {
        val user = users.find { it.id == userId }
        return if (user != null) {
            users.remove(user)
            if (currentUser.value?.id == userId) {
                currentUser.value = null
            }
            Result.Success(Unit)
        } else {
            Result.Error(Exception("User not found"))
        }
    }

    // TODO: Confirm if this is needed.
    override suspend fun authenticate(username: String): Result<User> {
        val user = users.find { it.username == username }
        return if (user != null) {
            val authenticatedUser = User(user.id, user.username, user.preferences)
            currentUser.value = authenticatedUser
            Result.Success(authenticatedUser)
        } else {
            Result.Error(Exception("Invalid credentials"))
        }
    }

    override fun getCurrentUser(): Flow<User?> = currentUser

    override fun getCurrentUserId(): Flow<Long?> {
        return currentUser.map { it?.id }
    }

}

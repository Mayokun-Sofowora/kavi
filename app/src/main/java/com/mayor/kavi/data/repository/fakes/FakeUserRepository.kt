package com.mayor.kavi.data.repository.fakes

import com.mayor.kavi.data.models.UsersEntity
import com.mayor.kavi.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeUserRepository : UserRepository {

    // In-memory storage for users
    private val usersMap = mutableMapOf<Long, UsersEntity>()
    private val usersFlow = MutableStateFlow<List<UsersEntity>>(emptyList())
    private var userIdCounter = 1L // Counter to simulate unique user IDs

    override suspend fun saveUser(user: UsersEntity) {
        // Assign a new ID if the user is new
        val userWithId = if (user.userId == 0L) {
            user.copy(userId = userIdCounter++)
        } else {
            user
        }
        usersMap[userWithId.userId] = userWithId
        emitUsers()
    }

    override suspend fun getUserById(userId: Long): UsersEntity? {
        return usersMap[userId]
    }

    override suspend fun updateUser(user: UsersEntity) {
        usersMap[user.userId]?.let {
            usersMap[user.userId] = user
            emitUsers()
        }
    }

    override fun getAllUsers(): Flow<List<UsersEntity>> {
        return usersFlow.asStateFlow()
    }

    override suspend fun deleteUser(user: UsersEntity) {
        usersMap.remove(user.userId)
        emitUsers()
    }

    override suspend fun authenticateUser(username: String, passwordHash: String): UsersEntity? {
        return usersMap.values.firstOrNull {
            it.username == username && it.passwordHash == passwordHash
        }
    }

    /**
     * Emits the current user list to the flow.
     */
    private fun emitUsers() {
        usersFlow.value = usersMap.values.toList()
    }
}

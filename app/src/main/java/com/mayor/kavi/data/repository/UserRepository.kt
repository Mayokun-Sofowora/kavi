package com.mayor.kavi.data.repository

import com.mayor.kavi.data.dao.UserDao
import com.mayor.kavi.data.dao.Users
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface UserRepository {

    /**
     * Inserts or updates a user in the database.
     */
    suspend fun saveUser(user: Users)

    /**
     * Retrieves a user by their unique ID.
     */
    suspend fun getUserById(userId: Long): Users?

    /**
     * Updates user information.
     */
    suspend fun updateUser(user: Users)

    /**
     * Retrieves all users as a Flow.
     */
    fun getAllUsers(): Flow<List<Users>>

    /**
     * Deletes a user from the database.
     */
    suspend fun deleteUser(user: Users)

    /**
     * Authenticates a user using their username and password hash.
     */
    suspend fun authenticateUser(username: String, passwordHash: String): Users?
}

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {

    override suspend fun saveUser(user: Users) {
        userDao.insertUser(user)
    }

    override suspend fun getUserById(userId: Long): Users? {
        return userDao.getUserById(userId)
    }

    override suspend fun updateUser(user: Users) {
        userDao.updateUser(user)
    }

    override fun getAllUsers(): Flow<List<Users>> {
        return userDao.getAllUsers()
    }

    override suspend fun deleteUser(user: Users) {
        userDao.deleteUser(user)
    }

    override suspend fun authenticateUser(username: String, passwordHash: String): Users? {
        // Simulating a query by filtering users
        val allUsers = userDao.getAllUsers() // Flow<List<Users>>
        var authenticatedUser: Users? = null

        allUsers.collect { users ->
            authenticatedUser = users.firstOrNull {
                it.username == username && it.passwordHash == passwordHash
            }
        }

        return authenticatedUser
    }
}

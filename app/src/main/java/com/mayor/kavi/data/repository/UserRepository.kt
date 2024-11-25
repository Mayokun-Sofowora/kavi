package com.mayor.kavi.data.repository

import com.mayor.kavi.data.dao.UsersDao
import com.mayor.kavi.data.models.UsersEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface UserRepository {

    /**
     * Inserts or updates a user in the database.
     */
    suspend fun saveUser(user: UsersEntity)

    /**
     * Retrieves a user by their unique ID.
     */
    suspend fun getUserById(userId: Long): UsersEntity?

    /**
     * Updates user information.
     */
    suspend fun updateUser(user: UsersEntity)

    /**
     * Retrieves all users as a Flow.
     */
    fun getAllUsers(): Flow<List<UsersEntity>>

    /**
     * Deletes a user from the database.
     */
    suspend fun deleteUser(user: UsersEntity)

    /**
     * Authenticates a user using their username and password hash.
     */
    suspend fun authenticateUser(username: String, passwordHash: String): UsersEntity?
}

class UserRepositoryImpl @Inject constructor(
    private val usersDao: UsersDao
) : UserRepository {

    override suspend fun saveUser(user: UsersEntity) {
        usersDao.insertUser(user)
    }

    override suspend fun getUserById(userId: Long): UsersEntity? {
        return usersDao.getUserById(userId)
    }

    override suspend fun updateUser(user: UsersEntity) {
        usersDao.updateUser(user)
    }

    override fun getAllUsers(): Flow<List<UsersEntity>> {
        return usersDao.getAllUsers()
    }

    override suspend fun deleteUser(user: UsersEntity) {
        usersDao.deleteUser(user)
    }

    override suspend fun authenticateUser(username: String, passwordHash: String): UsersEntity? {
        // Simulating a query by filtering users
        val allUsers = usersDao.getAllUsers() // Flow<List<Users>>
        var authenticatedUser: UsersEntity? = null

        allUsers.collect { users ->
            authenticatedUser = users.firstOrNull {
                it.username == username && it.passwordHash == passwordHash
            }
        }

        return authenticatedUser
    }
}

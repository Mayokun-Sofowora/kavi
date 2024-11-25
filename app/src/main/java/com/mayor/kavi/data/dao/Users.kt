package com.mayor.kavi.data.dao

import androidx.room.*
import com.mayor.kavi.data.models.UsersEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsersDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UsersEntity)

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: Long): UsersEntity?

    @Update
    suspend fun updateUser(user: UsersEntity)

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): UsersEntity?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UsersEntity?

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UsersEntity>>

    @Delete
    suspend fun deleteUser(user: UsersEntity)
}

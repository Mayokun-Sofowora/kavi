package com.mayor.kavi.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow


@Entity(tableName = "users")
data class Users(
    @PrimaryKey(autoGenerate = true) val userId: Long = 0,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: Users)

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: Long): Users?

    @Update
    suspend fun updateUser(user: Users)

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<Users>>

    @Delete
    suspend fun deleteUser(user: Users)
}

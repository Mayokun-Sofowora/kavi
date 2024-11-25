package com.mayor.kavi.data.dao

import androidx.room.*
import com.mayor.kavi.data.models.FriendStatus
import com.mayor.kavi.data.models.FriendsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Handle upsert functionality
    suspend fun insertFriend(friend: FriendsEntity)

    @Update
    suspend fun updateFriend(friend: FriendsEntity)

    @Delete
    suspend fun deleteFriend(friend: FriendsEntity)

    @Query("SELECT * FROM friends WHERE user_id = :userId")
    suspend fun getFriendsForUser(userId: Long): List<FriendsEntity>

    @Query("SELECT * FROM friends WHERE user_id = :userId AND status = :status")
    fun getFriendsByStatus(
        userId: Long,
        status: FriendStatus
    ): Flow<List<FriendsEntity>>

    @Query("SELECT COUNT(*) FROM friends WHERE user_id = :userId")
    suspend fun getCountOfFriends(userId: Long): Int

    @Query("SELECT * FROM friends WHERE user_id = :userId AND friend_user_id = :friendUserId")
    suspend fun getFriendship(userId: Long, friendUserId: Long): FriendsEntity?

    @Query("SELECT * FROM friends WHERE user_id = :userId OR friend_user_id = :userId")
    fun getAllFriendships(userId: Long): Flow<List<FriendsEntity>> // Added method to get all friendships
}
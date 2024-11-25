package com.mayor.kavi.data.repository

import com.mayor.kavi.data.models.Friends
import com.mayor.kavi.data.models.FriendStatus
import com.mayor.kavi.data.dao.FriendsDao
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface FriendsRepository {
    suspend fun insertFriendship(friend: Friends)

    suspend fun getFriendship(userId: Long, friendUserId: Long): Friends?

    fun getFriendsByStatus(userId: Long, status: FriendStatus): Flow<List<Friends>>

    fun getAllFriends(userId: Long): Flow<List<Friends>>

    suspend fun updateFriendship(friend: Friends)

    suspend fun deleteFriendship(friend: Friends)

    fun getAllFriendships(userId: Long): Flow<List<Friends>>
}

class FriendsRepositoryImpl @Inject constructor(
    private val friendsDao: FriendsDao
) : FriendsRepository {

    // Insert a new friendship or update an existing one
    override suspend fun insertFriendship(friend: Friends) {
        // Use upsert (replace) to handle the case where a friendship might already exist
        friendsDao.insertFriendship(friend)
    }

    // Get a specific friendship by user ID and friend user ID
    override suspend fun getFriendship(userId: Long, friendUserId: Long): Friends? {
        return friendsDao.getFriendship(userId, friendUserId)
    }

    // Get all friends of a specific user with a specific status
    override fun getFriendsByStatus(userId: Long, status: FriendStatus): Flow<List<Friends>> {
        return friendsDao.getFriendsByStatus(userId, status)
    }

    // Get all friends of a specific user (either as user or friend)
    override fun getAllFriends(userId: Long): Flow<List<Friends>> {
        return friendsDao.getAllFriends(userId)
    }

    // Update a friendship status (e.g., Accept, Block, etc.)
    override suspend fun updateFriendship(friend: Friends) {
        friendsDao.updateFriendship(friend)
    }

    // Delete a specific friendship
    override suspend fun deleteFriendship(friend: Friends) {
        friendsDao.deleteFriendship(friend)
    }

    // Get all friendships for a user, regardless of status
    override fun getAllFriendships(userId: Long): Flow<List<Friends>> {
        return friendsDao.getAllFriendships(userId)
    }
}

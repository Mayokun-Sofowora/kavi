package com.mayor.kavi.data.repository

import com.mayor.kavi.data.models.FriendsEntity
import com.mayor.kavi.data.models.FriendStatus
import com.mayor.kavi.data.dao.FriendsDao
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface FriendsRepository {
    suspend fun insertFriendship(friend: FriendsEntity)

    suspend fun getFriendship(userId: Long, friendUserId: Long): FriendsEntity?

    fun getFriendsByStatus(userId: Long, status: FriendStatus): Flow<List<FriendsEntity>>

    fun getAllFriends(userId: Long): Flow<List<FriendsEntity>>

    suspend fun updateFriendship(friend: FriendsEntity)

    suspend fun deleteFriendship(friend: FriendsEntity)

    fun getAllFriendships(userId: Long): Flow<List<FriendsEntity>>
}

class FriendsRepositoryImpl @Inject constructor(
    private val friendsDao: FriendsDao
) : FriendsRepository {

    override suspend fun insertFriendship(friend: FriendsEntity) {
        // Use upsert (replace) to handle the case where a friendship might already exist
        friendsDao.insertFriend(friend)
    }

    override suspend fun getFriendship(userId: Long, friendUserId: Long): FriendsEntity? {
        return friendsDao.getFriendship(userId, friendUserId)
    }

    override fun getFriendsByStatus(userId: Long, status: FriendStatus): Flow<List<FriendsEntity>> {
        return friendsDao.getFriendsByStatus(userId, status)
    }

    override fun getAllFriends(userId: Long): Flow<List<FriendsEntity>> {
        return flow {
            // Emit the list of friends as a flow
            emit(friendsDao.getFriendsForUser(userId))
        }
    }

    override suspend fun updateFriendship(friend: FriendsEntity) {
        friendsDao.updateFriend(friend)
    }

    override suspend fun deleteFriendship(friend: FriendsEntity) {
        friendsDao.deleteFriend(friend)
    }

    override fun getAllFriendships(userId: Long): Flow<List<FriendsEntity>> {
        return friendsDao.getAllFriendships(userId)
    }
}

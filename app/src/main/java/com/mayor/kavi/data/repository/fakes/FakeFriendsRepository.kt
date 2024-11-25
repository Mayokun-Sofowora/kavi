package com.mayor.kavi.data.repository.fakes

import com.mayor.kavi.data.models.FriendsEntity
import com.mayor.kavi.data.models.FriendStatus
import com.mayor.kavi.data.repository.FriendsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

class FakeFriendsRepository : FriendsRepository {

    private val friendships = mutableListOf<FriendsEntity>()
    private val friendshipsFlow = MutableStateFlow<List<FriendsEntity>>(emptyList())
    private var friendIdCounter = 1L

    override suspend fun insertFriendship(friend: FriendsEntity) {
        val existing = friendships.find {
            it.userId == friend.userId && it.friendUserId == friend.friendUserId
        }
        if (existing != null) {
            friendships.remove(existing)
        }
        val newFriend = if (friend.friendId == 0L) {
            friend.copy(friendId = friendIdCounter++)
        } else {
            friend
        }
        friendships.add(newFriend)
        emitFriendships()
    }

    override suspend fun getFriendship(userId: Long, friendUserId: Long): FriendsEntity? {
        return friendships.find { it.userId == userId && it.friendUserId == friendUserId }
    }

    override fun getFriendsByStatus(userId: Long, status: FriendStatus): Flow<List<FriendsEntity>> {
        return flow {
            emit(friendships.filter { it.userId == userId && it.status == status })
        }
    }

    override fun getAllFriends(userId: Long): Flow<List<FriendsEntity>> {
        return flow {
            emit(friendships.filter { it.userId == userId })
        }
    }

    override suspend fun updateFriendship(friend: FriendsEntity) {
        val index = friendships.indexOfFirst { it.friendId == friend.friendId }
        if (index != -1) {
            friendships[index] = friend
            emitFriendships()
        }
    }

    override suspend fun deleteFriendship(friend: FriendsEntity) {
        friendships.removeIf { it.friendId == friend.friendId }
        emitFriendships()
    }

    override fun getAllFriendships(userId: Long): Flow<List<FriendsEntity>> {
        return flow {
            emit(friendships.filter { it.userId == userId || it.friendUserId == userId })
        }
    }

    private fun emitFriendships() {
        friendshipsFlow.value = friendships.toList()
    }
}

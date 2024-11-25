package com.mayor.kavi.data.dao

import androidx.room.*
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import com.mayor.kavi.data.models.Friends
import com.mayor.kavi.data.models.FriendStatus

@Entity(
    tableName = "friends",
    foreignKeys = [
        ForeignKey(
            entity = Users::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Users::class,
            parentColumns = ["userId"],
            childColumns = ["friendUserId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Friends(
    @PrimaryKey(autoGenerate = true) val friendId: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "friend_user_id") val friendUserId: Long,
    val status: FriendStatus,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime
)

@Dao
interface FriendsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriendship(friend: Friends)

    @Query("SELECT * FROM friends WHERE user_id = :userId AND friend_user_id = :friendUserId")
    suspend fun getFriendship(userId: Long, friendUserId: Long): Friends?

    @Query("SELECT * FROM friends WHERE user_id = :userId AND status = :status")
    fun getFriendsByStatus(userId: Long, status: FriendStatus): Flow<List<Friends>>

    @Query("SELECT * FROM friends WHERE user_id = :userId OR friend_user_id = :userId")
    fun getAllFriends(userId: Long): Flow<List<Friends>>

    @Update
    suspend fun updateFriendship(friend: Friends)

    @Delete
    suspend fun deleteFriendship(friend: Friends)

    @Query("SELECT * FROM friends WHERE user_id = :userId OR friend_user_id = :userId")
    fun getAllFriendships(userId: Long): Flow<List<Friends>>
}

package com.mayor.kavi.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mayor.kavi.data.models.Avatar
import com.mayor.kavi.data.models.UserProfile
import com.mayor.kavi.util.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : UserRepository {
    companion object {
        private const val COLLECTION_USERS = "users"
    }

    private var isNetworkConnected = true

    override suspend fun getUserById(userId: String): Result<UserProfile> =
        try {
            val document = firestore
                .collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val profile = document.toObject(UserProfile::class.java)
                if (profile != null) {
                    Result.Success(profile)
                } else {
                    // If profile parsing fails, create a default profile
                    val defaultProfile = UserProfile(
                        id = userId,
                        name = "Player",
                        email = "",
                        avatar = Avatar.DEFAULT,
                        lastSeen = System.currentTimeMillis()
                    )
                    // Save the default profile
                    firestore
                        .collection(COLLECTION_USERS)
                        .document(userId)
                        .set(defaultProfile)
                        .await()
                    Result.Success(defaultProfile)
                }
            } else {
                // If document doesn't exist, create a default profile
                val defaultProfile = UserProfile(
                    id = userId,
                    name = "Player",
                    email = "",
                    avatar = Avatar.DEFAULT,
                    lastSeen = System.currentTimeMillis()
                )
                // Save the default profile
                firestore
                    .collection(COLLECTION_USERS)
                    .document(userId)
                    .set(defaultProfile)
                    .await()
                Result.Success(defaultProfile)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting user profile")
            Result.Error("Error getting user profile", e)
        }

    override suspend fun setUserOnlineStatus(isOnline: Boolean): Result<Unit> =
        try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            
            val userDoc = firestore
                .collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            if (!userDoc.exists()) {
                // Create a default profile if it doesn't exist
                val defaultProfile = UserProfile(
                    id = userId,
                    name = "Player",
                    email = "",
                    avatar = Avatar.DEFAULT,
                    lastSeen = System.currentTimeMillis(),
                    isOnline = isOnline
                )
                firestore
                    .collection(COLLECTION_USERS)
                    .document(userId)
                    .set(defaultProfile)
                    .await()
            } else {
                // Update online status
                firestore
                    .collection(COLLECTION_USERS)
                    .document(userId)
                    .update(
                        mapOf(
                            "isOnline" to isOnline,
                            "lastSeen" to System.currentTimeMillis()
                        )
                    )
                    .await()
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating user online status")
            Result.Error("Error updating user online status", e)
        }

    override suspend fun getCurrentUser(): Result<UserProfile> =
        try {
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            getUserById(userId)
        } catch (e: Exception) {
            Timber.e(e, "Error getting current user")
            Result.Error("Error getting current user", e)
        }

    override fun listenForOnlinePlayers(): Flow<List<UserProfile>> = callbackFlow {
        val subscription = firestore.collection(COLLECTION_USERS)
            .whereEqualTo("isOnline", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val players = snapshot?.documents
                    ?.mapNotNull { it.toObject(UserProfile::class.java) }
                    ?.filter { it.id != getCurrentUserId() }
                    ?.map { it.copy(
                        isInGame = it.currentGameId.isNotEmpty() && it.isInGame,
                        isWaitingForPlayers = it.currentGameId.isNotEmpty() && it.isWaitingForPlayers
                    ) }
                    ?: emptyList()

                trySend(players)
            }

        awaitClose { subscription.remove() }
    }

    override suspend fun updateUserProfile(profile: UserProfile): Result<UserProfile> =
        try {
            Timber.d("Updating user profile for user: ${profile.id}")
            val userProfile = firestore
                .collection(COLLECTION_USERS)
                .document(profile.id)
            
            // Use merge option to update only the specified fields
            val updates = mapOf(
                "name" to profile.name,
                "avatar" to profile.avatar,
                "lastSeen" to System.currentTimeMillis()
            )
            
            userProfile.set(updates, SetOptions.merge())
                .await()
            
            Timber.i("Successfully updated user profile for: ${profile.id}")
            Result.Success(profile)
        } catch (e: Exception) {
            Timber.e(e, "Error updating user profile")
            Result.Error("Error updating user profile", e)
        }

    override suspend fun getAllPlayers(): Result<List<UserProfile>> = try {
        val snapshot = firestore
            .collection(COLLECTION_USERS)
            .get()
            .await()
        Result.Success(snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) })
    } catch (e: Exception) {
        Result.Error("Error updating user profile", e)
    }

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    override suspend fun setUserGameStatus(
        isInGame: Boolean,
        isWaitingForPlayers: Boolean,
        gameId: String
    ): Result<Unit> = try {
        val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
        
        val updates = mapOf(
            "isInGame" to isInGame,
            "isWaitingForPlayers" to isWaitingForPlayers,
            "currentGameId" to gameId,
            "lastSeen" to System.currentTimeMillis()
        )
        
        firestore
            .collection(COLLECTION_USERS)
            .document(userId)
            .update(updates)
            .await()
            
        Result.Success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Error updating user game status")
        Result.Error("Error updating user game status", e)
    }
}
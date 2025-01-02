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
                    Timber.e("Failed to parse user profile for ID: $userId")
                    Result.Error("Failed to parse user profile")
                }
            } else {
                Timber.e("No user profile found for ID: $userId")
                Result.Error("User profile not found")
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
                Timber.e("No user profile found for ID: $userId")
                Result.Error("User profile not found")
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
                
                Result.Success(Unit)
            }
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
            val userId = getCurrentUserId() ?: throw Exception("User not authenticated")
            Timber.d("Updating user profile for user: $userId")
            
            val userProfile = firestore
                .collection(COLLECTION_USERS)
                .document(userId)
            
            // Update all profile fields except id
            val updates = mapOf(
                "name" to profile.name,
                "email" to profile.email,
                "avatar" to profile.avatar,
                "lastSeen" to System.currentTimeMillis(),
                "isOnline" to profile.isOnline,
                "isInGame" to profile.isInGame,
                "isWaitingForPlayers" to profile.isWaitingForPlayers,
                "currentGameId" to profile.currentGameId
            )
            
            userProfile.set(updates, SetOptions.merge())
                .await()
            
            Timber.i("Successfully updated user profile for: $userId")
            Result.Success(profile.copy(id = userId))
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
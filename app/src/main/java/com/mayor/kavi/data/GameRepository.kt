package com.mayor.kavi.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

interface GameRepository {
    fun getCurrentUserId(): String?
    suspend fun getUserById(id: String): Result<UserProfile>
    suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit>
    suspend fun getAllPlayers(): Result<List<UserProfile>>
    suspend fun searchPlayer(query: String): Result<List<UserProfile>>

    suspend fun getScorePoints(): Result<String>
    suspend fun getScoreModifiers(): Result<List<String>>
    suspend fun getDiceFaces(): Result<List<String>>
    suspend fun getDiceNumbers(): Result<List<String>>
    suspend fun getDiceSideNumbers(): Result<List<String>>
    suspend fun getBackgroundColors(): Result<List<String>>
}

class GameRepositoryImpl @Inject constructor(
    private val firebaseFirestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val context: Context
) : GameRepository {
    private val dataStore = DataStoreManager.getInstance(context)
    override fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid

    override suspend fun getUserById(id: String): Result<UserProfile> = try {
        val snapshot = firebaseFirestore
            .collection("users")
            .document(id)
            .get()
            .await()
        val user = snapshot.toObject(UserProfile::class.java)
        if (user != null) {
            Result.success(user)
        } else {
            Result.failure(Exception("User not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit> = try {
        firebaseFirestore
            .collection("users")
            .document(userProfile.uid)
            .set(userProfile)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getAllPlayers(): Result<List<UserProfile>> = try {
        val snapshot = firebaseFirestore
            .collection("users")
            .get()
            .await()
        val players = snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
        Result.success(players)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun searchPlayer(query: String): Result<List<UserProfile>> = try {
        val snapshot = firebaseFirestore
            .collection("users")
            .whereGreaterThanOrEqualTo("username", query)
            .whereLessThanOrEqualTo("username", query + "\uf8ff")
            .get()
            .await()
        val players = snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
        Result.success(players)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getScorePoints(): Result<String> = fetchGameData("score_points")

    override suspend fun getScoreModifiers(): Result<List<String>> =
        fetchGameListData("score_modifiers")

    override suspend fun getDiceFaces(): Result<List<String>> = fetchGameListData("dice_faces")

    override suspend fun getDiceNumbers(): Result<List<String>> = fetchGameListData("dice_numbers")

    override suspend fun getDiceSideNumbers(): Result<List<String>> =
        fetchGameListData("dice_side_numbers")

    override suspend fun getBackgroundColors(): Result<List<String>> =
        fetchGameListData("background_colors")

    // Helper for fetching single string data
    private suspend fun fetchGameData(documentId: String): Result<String> = try {
        val snapshot = firebaseFirestore
            .collection("game_data")
            .document(documentId)
            .get()
            .await()
        val data = snapshot.getString("value")
        if (data != null) {
            Result.success(data)
        } else {
            Result.failure(Exception("Data not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Helper for fetching list data
    private suspend fun fetchGameListData(documentId: String): Result<List<String>> = try {
        val snapshot = firebaseFirestore
            .collection("game_data")
            .document(documentId)
            .get()
            .await()
        val data = snapshot["value"] as? List<String>
        if (data != null) {
            Result.success(data)
        } else {
            Result.failure(Exception("Data not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

package com.mayor.kavi.data.repository

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mayor.kavi.data.models.*
import com.mayor.kavi.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class StatisticsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val context: Context
) : StatisticsRepository {
    private var firebaseApp: FirebaseApp? = null

    init {
        initializeFirebaseIfNeeded(context)
    }

    private fun initializeFirebaseIfNeeded(context: Context) {
        if (firebaseApp == null) {
            firebaseApp = FirebaseApp.initializeApp(context)
        }
    }

    private fun getUserDocumentRef(userId: String) =
        firestore.collection("users").document(userId)

    override suspend fun getGameStatistics(): Result<GameStatistics> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
            val document = getUserDocumentRef(currentUserId).get().await()
            
            if (document.exists()) {
                val stats = document.toObject(GameStatistics::class.java)
                    ?: GameStatistics()
                Timber.d("Successfully retrieved game statistics for user: $currentUserId")
                Result.Success(stats)
            } else {
                // Create default statistics if none exist
                val defaultStats = GameStatistics()
                getUserDocumentRef(currentUserId).set(defaultStats).await()
                Timber.d("Created default statistics for user: $currentUserId")
                Result.Success(defaultStats)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get game statistics")
            Result.Error("Failed to get game statistics", e)
        }
    }

    override suspend fun updateGameStatistics(stats: GameStatistics): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
            Timber.d("Updating game statistics for user: $currentUserId")
            
            getUserDocumentRef(currentUserId).set(stats).await()
            Timber.d("Successfully updated game statistics")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update game statistics")
            Result.Error("Failed to update game statistics", e)
        }
    }

    override suspend fun updatePlayerAnalysis(userId: String, analysis: PlayerAnalysis): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Updating player analysis for user: $userId")
            getUserDocumentRef(userId)
                .update("playerAnalysis", analysis)
                .await()
            Timber.d("Successfully updated player analysis")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update player analysis")
            Result.Error("Failed to update player analysis", e)
        }
    }

    override suspend fun updateUserOnlineStatus(isOnline: Boolean): Result<Unit> = try {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")

        getUserDocumentRef(currentUserId).update(
            mapOf(
                "isOnline" to isOnline,
                "lastSeen" to System.currentTimeMillis()
            )
        ).await()

        Result.Success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Error updating user online status")
        Result.Error("Error updating user online status", e)
    }

    override suspend fun cleanupGameSession(sessionId: String): Result<Unit> {
        // Implementation for cleaning up game session logic
        return Result.Error("Not implemented")
    }

    override suspend fun clearUserStatistics(): Result<Unit> = try {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
        Timber.i("Clearing remote statistics for user: $currentUserId")
        
        getUserDocumentRef(currentUserId).set(GameStatistics()).await()
        Timber.d("Successfully cleared remote statistics for user: $currentUserId")
        
        Result.Success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to clear remote statistics")
        Result.Error("Failed to clear remote statistics", e)
    }

    private fun getCurrentUserId(): String? = auth.currentUser?.uid
}
package com.mayor.kavi.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mayor.kavi.data.models.LeaderboardEntry
import com.mayor.kavi.util.Result
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class LeaderboardRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : LeaderboardRepository {

    override suspend fun getGlobalLeaderboard(): List<LeaderboardEntry> {
        return try {
            // Get all entries sorted by score
            val entries = firestore.collection("leaderboard")
                .orderBy("score", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(LeaderboardEntry::class.java)
                }
                
            // Update positions based on score ranking (highest score = position 1)
            entries.mapIndexed { index, entry ->
                entry.copy(position = index + 1)
            }.also { updatedEntries ->
                // Update positions in Firestore
                updatedEntries.forEach { entry ->
                    firestore.collection("leaderboard")
                        .document(entry.userId)
                        .update("position", entry.position)
                }
            }
        } catch (e: Exception) {
            throw Exception("Failed to fetch leaderboard: ${e.message}")
        }
    }

    override suspend fun deleteLeaderboardEntry(userId: String) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("leaderboard")
                .document(userId)
                .delete()
                .await()
            
            // Update positions after deletion
            getGlobalLeaderboard()
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete leaderboard entry: ${e.message}")
            throw e
        }
    }

    override suspend fun getLeaderboard(): Result<List<LeaderboardEntry>> = withContext(Dispatchers.IO) {
        try {
            val entries = firestore.collection("leaderboard")
                .orderBy("score", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(LeaderboardEntry::class.java)
                }
            Result.Success(entries)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get leaderboard: ${e.message}")
            Result.Error("Failed to get leaderboard", e)
        }
    }

    override suspend fun updateLeaderboardEntry(entry: LeaderboardEntry) {
        try {
            firestore.runTransaction { transaction ->
                // Get existing entry if it exists
                val docRef = firestore.collection("leaderboard").document(entry.userId)
                val existingDoc = transaction.get(docRef)

                val updatedEntry = if (existingDoc.exists()) {
                    val existing = existingDoc.toObject(LeaderboardEntry::class.java)!!
                    entry.copy(
                        score = maxOf(entry.score, existing.score), // Keep highest score
                        gamesPlayed = existing.gamesPlayed + 1, // Increment by 1 instead of adding entry.gamesPlayed
                        gamesWon = existing.gamesWon + (if (entry.gamesWon > existing.gamesWon) 1 else 0) // Increment by 1 if won
                    )
                } else {
                    entry.copy(
                        gamesPlayed = 1,
                        gamesWon = if (entry.gamesWon > 0) 1 else 0
                    )
                }

                // Save the entry in the transaction
                transaction.set(docRef, updatedEntry)
            }.await()

            // Update positions after transaction completes
            getGlobalLeaderboard()
        } catch (e: Exception) {
            Timber.e(e, "Failed to update leaderboard entry: ${e.message}")
            throw e
        }
    }

    override suspend fun getLeaderboardEntry(userId: String): LeaderboardEntry? = withContext(Dispatchers.IO) {
        try {
            val document = firestore.collection("leaderboard")
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                document.toObject(LeaderboardEntry::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting leaderboard entry: ${e.message}")
            null
        }
    }
} 
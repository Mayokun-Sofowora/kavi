package com.mayor.kavi.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mayor.kavi.data.models.LeaderboardEntry
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

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

    override suspend fun updateLeaderboardEntry(entry: LeaderboardEntry) {
        try {
            // Get existing entry if it exists
            val existingDoc = firestore.collection("leaderboard")
                .document(entry.userId)
                .get()
                .await()

            val updatedEntry = if (existingDoc.exists()) {
                val existing = existingDoc.toObject(LeaderboardEntry::class.java)!!
                entry.copy(
                    score = maxOf(entry.score, existing.score), // Keep highest score
                    gamesPlayed = existing.gamesPlayed + entry.gamesPlayed,
                    gamesWon = existing.gamesWon + entry.gamesWon
                )
            } else {
                entry
            }

            // Save the entry
            firestore.collection("leaderboard")
                .document(entry.userId)
                .set(updatedEntry)
                .await()

            // Update all positions after an entry is updated
            getGlobalLeaderboard()
        } catch (e: Exception) {
            throw Exception("Failed to update leaderboard entry: ${e.message}")
        }
    }
} 
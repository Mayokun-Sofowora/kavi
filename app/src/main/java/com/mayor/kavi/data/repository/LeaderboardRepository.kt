package com.mayor.kavi.data.repository

import com.mayor.kavi.data.models.LeaderboardEntry
import com.mayor.kavi.util.Result

interface LeaderboardRepository {
    suspend fun getLeaderboard(): Result<List<LeaderboardEntry>>
    suspend fun updateLeaderboardEntry(entry: LeaderboardEntry)
    suspend fun getLeaderboardEntry(userId: String): LeaderboardEntry?
    suspend fun getGlobalLeaderboard(): List<LeaderboardEntry>
    suspend fun deleteLeaderboardEntry(userId: String): List<LeaderboardEntry>
} 
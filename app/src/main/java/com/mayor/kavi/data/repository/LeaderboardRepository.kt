package com.mayor.kavi.data.repository

import com.mayor.kavi.data.models.LeaderboardEntry

interface LeaderboardRepository {
    suspend fun getGlobalLeaderboard(): List<LeaderboardEntry>
    suspend fun updateLeaderboardEntry(entry: LeaderboardEntry)
} 
package com.mayor.kavi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mayor.kavi.data.repository.LeaderboardRepository
import com.mayor.kavi.data.models.LeaderboardEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/***
 * Purpose: Manage leaderboard data (local or remote).
 * Responsibilities:
 * Fetch and display the leaderboard.
 * Update the leaderboard based on completed games.
 */
@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val leaderboardRepository: LeaderboardRepository
) : ViewModel() {

    // Holds the leaderboard entries to be displayed
    private val _leaderboardEntries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
//    val leaderboardEntries: StateFlow<List<LeaderboardEntry>> = _leaderboardEntries
    val leaderboardEntries = _leaderboardEntries.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Fetch all leaderboard entries
    fun fetchLeaderboard() {
        viewModelScope.launch {
            try {
                leaderboardRepository.getLeaderboard().collect { entries ->
                    _leaderboardEntries.value = entries
                    _errorMessage.value = null // Clear the error if successful
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load leaderboard: ${e.message}"
            }
        }
    }

    // Add a new leaderboard entry (e.g., after a game ends)
    fun addLeaderboardEntry(playerId: String, playerName: String, wins: Int, gamesPlayed: Int) {
        viewModelScope.launch {
            leaderboardRepository.addPlayerToLeaderboard(playerId, playerName, wins, gamesPlayed)
            fetchLeaderboard() // Refresh leaderboard after adding a new entry
        }
    }

    // Update an existing leaderboard entry (e.g., after a player has more wins)
    fun updateLeaderboard(playerId: String, wins: Int, gamesPlayed: Int) {
        viewModelScope.launch {
            leaderboardRepository.updateLeaderboard(playerId, wins, gamesPlayed)
            fetchLeaderboard() // Refresh leaderboard after updating an entry
        }
    }

    // Delete a leaderboard entry (for example, for testing or resetting)
    fun deleteLeaderboardEntry(playerId: String) {
        viewModelScope.launch {
            // Assuming you have a method to delete by playerId in your repository
            leaderboardRepository.deleteLeaderboardEntry(playerId)
            fetchLeaderboard() // Refresh leaderboard after deletion
        }
    }
}

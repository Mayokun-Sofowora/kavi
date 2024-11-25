package com.mayor.kavi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mayor.kavi.data.repository.StatisticsRepository
import com.mayor.kavi.data.models.PlayerStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Purpose: Handle statistics for the current user.
 * Responsibilities:
 * Track user performance (e.g., number of wins, losses, and high scores).
 * Fetch stats from the database or calculate them on the fly.
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatisticsRepository
) : ViewModel() {

    // Holds player stats to be displayed
    private val _playerStats = MutableStateFlow<PlayerStats?>(null)
    val playerStats: StateFlow<PlayerStats?> = _playerStats

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun fetchPlayerStats(playerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                statsRepository.getPlayerStats(playerId).collect { stats ->
                    _playerStats.value = stats
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load stats: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Update player stats (e.g., after a game)
    fun updatePlayerStats(playerId: String, updatedStats: PlayerStats) {
        viewModelScope.launch {
            statsRepository.updatePlayerStats(updatedStats.toString(), false)
            fetchPlayerStats(playerId) // Refresh player stats
        }
    }

    // Reset player stats (optional, for testing or resetting)
    fun resetPlayerStats(playerId: String) {
        viewModelScope.launch {
            statsRepository.resetPlayerStats(playerId)
            fetchPlayerStats(playerId) // Refresh player stats after reset
        }
    }
}

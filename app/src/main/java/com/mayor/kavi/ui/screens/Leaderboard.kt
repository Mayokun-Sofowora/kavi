package com.mayor.kavi.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mayor.kavi.ui.viewmodel.LeaderboardViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun LeaderboardScreen(navController: NavController, viewModel: LeaderboardViewModel) {
    // Collect leaderboard entries from the viewModel state
    val leaderboardEntries = viewModel.leaderboardEntries.collectAsState()

    // Fetch leaderboard entries when the screen is displayed
    LaunchedEffect(Unit) {
        viewModel.fetchLeaderboard()
    }

    Column {
        // Check if the leaderboardEntries list is empty to display loading state or message
        if (leaderboardEntries.value.isEmpty()) {
            Text(text = "Loading leaderboard...")
        } else {
            leaderboardEntries.value.forEach { entry ->
                Text(
                    // Display leaderboard entries in a user-friendly format
                    modifier = Modifier.padding(8.dp),
                    text = "Rank: ${entry.rank}, Player: ${entry.playerName}, " +
                            "Wins: ${entry.totalWins}, Games Played: ${entry.totalGames}, " +
                            "Level: ${entry.level}"
                )
            }
        }
    }
}
package com.mayor.kavi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.mayor.kavi.data.models.PlayerStats
import com.mayor.kavi.ui.viewmodel.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(navController: NavController, playerId: String?, viewModel: StatsViewModel) {
    if (playerId == null) {
        // Handle the case where playerId is null.
        Text("Player ID is null!", color = MaterialTheme.colorScheme.error)
        return
    }

    // Collect states without delegation for safer handling
    val playerStats by viewModel.playerStats.collectAsState(initial = null)
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val errorMessage by viewModel.errorMessage.collectAsState(initial = null)

    // Trigger stats fetching when the screen is displayed
    LaunchedEffect(playerId) {
        viewModel.fetchPlayerStats(playerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player Statistics") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> CircularProgressIndicator()

                    // Use explicit check for null to avoid Unwrapping issues
                    errorMessage != null -> {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    playerStats != null -> {
                        StatsDetails(playerStats = playerStats!!)
                    }

                    else -> {
                        Text("No stats available.")
                    }
                }
            }
        }
    )
}

@Composable
fun StatsDetails(playerStats: PlayerStats) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Player: ${playerStats.playerId}", fontSize = 20.sp)
        Text(text = "Level: ${playerStats.level}")
        Text(text = "Games Played: ${playerStats.totalGamesPlayed}")
        Text(text = "Games Won: ${playerStats.totalGamesWon}")
        Text(text = "Highest Score: ${playerStats.highestScore}")
        Text(text = "Achievements: ${playerStats.achievements.joinToString()}")
        Text(text = "Avg Score: ${playerStats.averageScore}")
        Text(text = "Favorite Mode: ${playerStats.favoriteGameMode}")
        Text(text = "Total Play Time: ${playerStats.totalPlayTime} minutes")
    }
}
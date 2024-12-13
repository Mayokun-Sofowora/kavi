package com.mayor.kavi.ui.screens.main

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.asFlow
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.mayor.kavi.R
import com.mayor.kavi.data.*
import com.mayor.kavi.data.games.GameBoard
import com.mayor.kavi.data.manager.GameStats
import com.mayor.kavi.ui.viewmodel.*
import com.mayor.kavi.util.NetworkConnection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: DiceViewModel = hiltViewModel(),
    navController: NavController
) {
    val context = LocalContext.current
    val networkConnection = remember {
        NetworkConnection(
            context,
            connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        )
    }
    val isConnected by networkConnection.asFlow().collectAsState(initial = false)

    val gameStats by viewModel.gameStats.collectAsState(initial = GameStats(
        0,
        emptyMap(),
        emptyMap()
    )
    )
    val userProfile by viewModel.userProfile.collectAsState()
    val nearbyPlayers by viewModel.nearbyPlayers.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Back")
                    }
                },
                actions = {
                    Icon(
                        imageVector = if (isConnected)
                            Icons.Default.SignalWifi4Bar
                        else
                            Icons.Default.SignalWifiOff,
                        contentDescription = "Network Status",
                        tint = if (isConnected) Color.Green else Color.Red
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // User Profile Card
            UserProfileCard(userProfile)

            // Nearby Players Card (only shown when connected)
            if (isConnected && nearbyPlayers.isNotEmpty()) {
                NearbyPlayersCard(nearbyPlayers)
            }

            // Overall Stats Card
            StatCard(
                title = "Overall Statistics",
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("Games", gameStats.gamesPlayed.toString())
                        StatItem("Win Rate", "${calculateWinRate(gameStats.winRates)}%")
                        StatItem("High Score", gameStats.highScores.values.maxOrNull()?.toString() ?: "0")
                    }
                }
            )

            // Game-specific Stats
            GameBoard.entries.forEach { board ->
                GameStatCard(board, gameStats)
            }
        }
    }
}

@Composable
private fun UserProfileCard(profile: UserProfile?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = profile?.image ?: R.drawable.default_avatar,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = profile?.name ?: "Guest",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = profile?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Favorite Game: ${profile?.favoriteGames?.firstOrNull() ?: "None"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun NearbyPlayersCard(players: List<UserProfile>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Nearby Players",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            players.forEach { player ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = player.image ?: R.drawable.default_avatar,
                        contentDescription = "Player Avatar",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(player.name)
                }
            }
        }
    }
}

@Composable
private fun GameStatCard(board: GameBoard, stats: GameStats) {
    StatCard(
        title = board.modeName,
        content = {
            Column {
                val boardStats = stats.winRates[board.modeName]
                val winRate = if (boardStats != null) {
                    (boardStats.first.toFloat() / boardStats.second.toFloat())
                } else 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Win Rate", "${(winRate * 100).toInt()}%")
                    StatItem("High Score", "${stats.highScores[board.modeName] ?: 0}")
                }
            }
        }
    )
}

private fun calculateWinRate(winRates: Map<String, Pair<Int, Int>>): Int {
    val totalWins = winRates.values.sumOf { it.first }
    val totalGames = winRates.values.sumOf { it.second }
    return if (totalGames > 0) {
        ((totalWins.toFloat() / totalGames.toFloat()) * 100).toInt()
    } else 0
}

@Composable
private fun StatCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
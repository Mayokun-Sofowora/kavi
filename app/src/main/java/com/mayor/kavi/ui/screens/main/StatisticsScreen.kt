package com.mayor.kavi.ui.screens.main

import android.content.Context
import android.net.ConnectivityManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.*
import androidx.navigation.NavController
import com.mayor.kavi.R
import com.mayor.kavi.util.*
import com.mayor.kavi.data.manager.*
import com.mayor.kavi.data.models.*
import com.mayor.kavi.ui.components.AnalyticsDashboard
import com.mayor.kavi.ui.viewmodel.*
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    appViewModel: AppViewModel = hiltViewModel(),
    gameViewModel: GameViewModel = hiltViewModel(),
    navController: NavController,
) {
    var showAnalytics by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val statisticsManager = StatisticsManager.LocalStatisticsManager.current
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkConnection = remember { NetworkConnection(connectivityManager) }
    val isConnected by networkConnection.asFlow().collectAsState(initial = false)
    val userProfileState by appViewModel.userProfileState.collectAsState()
    val gameStats by statisticsManager.gameStatistics.collectAsState()
    val playerAnalysis by statisticsManager.playerAnalysis.collectAsState()

    // Calculate overall win rate
    val overallWinRate = gameStats?.winRates?.values?.let { winRates ->
        if (winRates.isEmpty()) 0L else {
            val totalWins = winRates.sumOf { it.wins }
            val totalGames = winRates.sumOf { it.total }
            if (totalGames > 0) (totalWins * 100L) / totalGames else 0L
        }
    } ?: 0L

    // Only reload profile if we don't have it or if there was an error
    LaunchedEffect(Unit) {
        val currentState = appViewModel.userProfileState.value
        if (currentState !is Result.Success && currentState !is Result.Loading) {
            appViewModel.loadUserProfile()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (showAnalytics) "Analytics" else "Statistics") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(id = R.color.primary_container),
                    titleContentColor = colorResource(id = R.color.on_primary_container)
                ),
                actions = {
                    // Toggle Analytics Button
                    IconButton(onClick = { showAnalytics = !showAnalytics }) {
                        Icon(
                            imageVector = if (showAnalytics)
                                Icons.Default.QueryStats
                            else
                                Icons.Default.Analytics,
                            contentDescription = if (showAnalytics) "Show Statistics" else "Show Analytics"
                        )
                    }
                    // Network Status Icon
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
        if (showAnalytics) {
            AnalyticsDashboard(
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    when (val state = userProfileState) {
                        is Result.Success -> {
                            UserProfileCard(
                                profile = state.data,
                                onProfileChange = { appViewModel.updateUserProfile(it) }
                            )
                        }

                        is Result.Error -> {
                            ErrorContent(
                                message = state.message,
                                onRetry = { appViewModel.loadUserProfile() }
                            )
                        }

                        is Result.Loading -> {
                            state.data?.let { profile ->
                                UserProfileCard(
                                    profile = profile,
                                    onProfileChange = { appViewModel.updateUserProfile(it) }
                                )
                            }
                            Timber.d("Loading user profile...")
                        }

                        else -> Timber.d("Unknown statistics screen state: $state")
                    }
                }

                if (userProfileState is Result.Success) {
                    val currentStats = gameStats
                    if (currentStats != null) {
                        item { OverallStatsCard(currentStats, overallWinRate) }
                        item { PlayerAnalysisGraph(playerAnalysis) }
                        item { PlayerAnalysisCard(playerAnalysis) }
                        item {
                            ResetStatsButton {
                                gameViewModel.viewModelScope.launch {
                                    statisticsManager.clearUserStatistics()
                                    Toast.makeText(
                                        context,
                                        "Statistics have been reset",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    } else {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No statistics available yet",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Play some games to see your statistics!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                } else if (userProfileState is Result.Loading) {
                    item { CircularProgressIndicator() }
                }
            }
        }
    }

    // Model retraining status
    appViewModel.modelRetrainingStatus.collectAsState().value?.let { status ->
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { /* dismiss */ }) {
                    Text("OK")
                }
            }
        ) {
            Text(status)
        }
    }
}

@Composable
private fun ResetStatsButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.RestartAlt,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text("Reset Statistics")
    }
}

@Composable
private fun PlayerAnalysisCard(analysis: PlayerAnalysis?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Player Analysis",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCircle(
                    value = (analysis?.predictedWinRate?.times(100)?.toLong() ?: 0),
                    label = "Predicted\nWin Rate",
                    color = MaterialTheme.colorScheme.primary,
                    suffix = "%"
                )

                StatCircle(
                    value = (analysis?.consistency?.times(100)?.toLong() ?: 0),
                    label = "Consistency",
                    suffix = "%",
                    color = MaterialTheme.colorScheme.secondary
                )

                StatCircle(
                    value = (analysis?.improvement?.times(100)?.toLong() ?: 0),
                    label = "Improvement",
                    suffix = "%",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            )
            {
                Text(
                    text = "Play Style: ${analysis?.playStyle ?: "N/A"}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun PlayerAnalysisGraph(analysis: PlayerAnalysis?) {
    val colorPrimary = colorResource(id = R.color.primary)
    val colorSecondary = colorResource(id = R.color.secondary)
    val colorTertiary = colorResource(id = R.color.tertiary)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(300.dp)
            .background(colorResource(id = R.color.surface))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Player Analysis Metrics", style = MaterialTheme.typography.headlineMedium)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp, end = 8.dp, bottom = 24.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val width = size.width
                    val height = size.height
                    val padding = 40f
                    val graphWidth = width - padding * 2
                    val graphHeight = height - padding * 2

                    // Draw axes
                    drawLine(
                        color = Color.Gray,
                        start = Offset(padding, padding),
                        end = Offset(padding, height - padding),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.Gray,
                        start = Offset(padding, height - padding),
                        end = Offset(width - padding, height - padding),
                        strokeWidth = 2f
                    )

                    // Draw grid lines
                    for (i in 0..4) {
                        val y = padding + (graphHeight * i / 4)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = Offset(padding, y),
                            end = Offset(width - padding, y),
                            strokeWidth = 1f
                        )
                        // Draw percentage labels
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 12.sp.toPx()
                                textAlign = android.graphics.Paint.Align.RIGHT
                            }
                            canvas.nativeCanvas.drawText(
                                "${(100 - i * 25)}%",
                                padding - 5f,
                                y + 5f,
                                paint
                            )
                        }
                    }

                    analysis?.let {
                        val metrics = listOf(
                            it.predictedWinRate to colorPrimary,
                            it.consistency to colorSecondary,
                            it.improvement to colorTertiary
                        )

                        // Draw metric lines and points
                        metrics.forEachIndexed { index, (value, color) ->
                            val x = padding + (graphWidth * (index + 1) / 4)
                            val y = padding + graphHeight * (1 - value)

                            // Draw vertical guide
                            drawLine(
                                color = color.copy(alpha = 0.2f),
                                start = Offset(x, padding),
                                end = Offset(x, height - padding),
                                strokeWidth = 1f
                            )

                            // Draw point
                            drawCircle(
                                color = color,
                                radius = 8f,
                                center = Offset(x, y)
                            )

                            // Draw label
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint().apply {
                                    this.color = color.toArgb()
                                    textSize = 10.sp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                                val label = when (index) {
                                    0 -> "Win Rate"
                                    1 -> "Consistency"
                                    else -> "Improvement"
                                }
                                canvas.nativeCanvas.drawText(
                                    label,
                                    x,
                                    height - padding / 3,
                                    paint
                                )
                            }
                        }

                        // Draw connecting lines between points
                        val path = Path()
                        metrics.forEachIndexed { index, (value, _) ->
                            val x = padding + (graphWidth * (index + 1) / 4)
                            val y = padding + graphHeight * (1 - value)
                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        // Draw gradient under the line
                        val gradientPath = Path().apply {
                            addPath(path)
                            lineTo(padding + (graphWidth * metrics.size / 4), height - padding)
                            lineTo(padding + (graphWidth / 4), height - padding)
                            close()
                        }

                        drawPath(
                            path = gradientPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    colorPrimary.copy(alpha = 0.2f),
                                    colorPrimary.copy(alpha = 0.0f)
                                )
                            )
                        )

                        // Draw the line connecting points
                        drawPath(
                            path = path,
                            color = colorPrimary,
                            style = Stroke(
                                width = 2f,
                                pathEffect = PathEffect.cornerPathEffect(10f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserProfileCard(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit
) {
    var showAvatarDialog by remember { mutableStateOf(false) }
    var editedUsername by remember { mutableStateOf(profile.name) }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = profile.avatar.resourceId),
                contentDescription = "Profile Avatar",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .clickable { showAvatarDialog = true }
            )

            if (isEditing) {
                OutlinedTextField(
                    value = editedUsername,
                    onValueChange = { editedUsername = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = {
                        onProfileChange(profile.copy(name = editedUsername))
                        isEditing = false
                    }) {
                        Text("Save")
                    }
                    Button(onClick = { isEditing = false }) {
                        Text("Cancel")
                    }
                }
            } else {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable { isEditing = true }
                )
            }

            // Delete Account Button
            Button(
                onClick = { showDeleteConfirmation = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }

    if (showAvatarDialog) {
        AvatarSelection(
            currentAvatar = profile.avatar,
            onAvatarSelected = {
                onProfileChange(profile.copy(avatar = it))
            },
            onDismiss = { showAvatarDialog = false }
        )
    }
}

@Composable
private fun OverallStatsCard(gameStats: GameStatistics, overallWinRate: Long) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Overall Statistics",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCircle(
                    value = gameStats.gamesPlayed.toLong(),
                    label = "Games\nPlayed",
                    color = MaterialTheme.colorScheme.primary
                )

                StatCircle(
                    value = overallWinRate,
                    label = "Win Rate",
                    suffix = "%",
                    color = MaterialTheme.colorScheme.secondary
                )

                StatCircle(
                    value = (gameStats.highScores.values.maxOrNull() ?: 0).toLong(),
                    label = "Highest\nScore",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun StatCircle(
    value: Long, label: String, suffix: String = "", color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f))
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$value$suffix",
                style = MaterialTheme.typography.titleLarge,
                color = color
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AvatarSelection(
    currentAvatar: Avatar, onAvatarSelected: (Avatar) -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Avatar") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(Avatar.entries.size) { index ->
                    val avatar = Avatar.entries[index]
                    Image(
                        painter = painterResource(id = avatar.resourceId),
                        contentDescription = avatar.name,
                        modifier = Modifier
                            .size(80.dp)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .clickable {
                                onAvatarSelected(avatar)
                                onDismiss()
                            }
                            .border(
                                width = if (avatar == currentAvatar) 2.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


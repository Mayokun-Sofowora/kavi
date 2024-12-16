package com.mayor.kavi.ui.screens.main

import android.content.Context
import android.net.ConnectivityManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.*
import com.mayor.kavi.R
import com.mayor.kavi.util.*
import com.mayor.kavi.data.*
import com.mayor.kavi.data.games.GameBoard
import com.mayor.kavi.data.manager.*
import com.mayor.kavi.ui.viewmodel.*
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    appViewModel: AppViewModel = hiltViewModel(),
    diceViewModel: DiceViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val networkConnection = remember {
        NetworkConnection(
            context,
            connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        )
    }
    val isConnected by networkConnection.asFlow().collectAsState(initial = false)
    val userProfileState by appViewModel.userProfileState.collectAsState()
    val gameStats by diceViewModel.gameStats.collectAsState()
    val playerAnalysis by diceViewModel.playerAnalysis.collectAsState()
    val shakeRates by diceViewModel.shakeRates.collectAsState()

    LaunchedEffect(Unit) {
        appViewModel.loadUserProfile()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(id = R.color.primary_container),
                    titleContentColor = colorResource(id = R.color.on_primary_container)
                ),
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                when (userProfileState) {
                    is Result.Success -> {
                        UserProfileCard(
                            profile = (userProfileState as Result.Success<UserProfile>).data,
                            onProfileChange = { appViewModel.updateUserProfile(it) }
                        )
                    }

                    is Result.Error -> {
                        ErrorContent(
                            message = (userProfileState as Result.Error).message,
                            onRetry = { appViewModel.loadUserProfile() }
                        )
                    }

                    is Result.Loading -> {
                        // Use the preserved data during loading
                        (userProfileState as Result.Loading<UserProfile>).data?.let { profile ->
                            UserProfileCard(
                                profile = profile,
                                onProfileChange = { appViewModel.updateUserProfile(it) }
                            )
                        }
                        Timber.d("Loading user profile...")
                    }
                }
            }

            if (userProfileState is Result.Success || userProfileState is Result.Loading) {
                item { OverallStatsCard(gameStats) }
                item { WinRateGraph(gameStats.winRates) }
                item { ShakeRateAnalysis(shakeRates) }
                item { GameStatsTable(gameStats) }
                item { PlayerAnalysisCard(playerAnalysis) }
                item { ClearUserStatsButton(diceViewModel) }
            }
        }
    }
}

@Composable
fun ClearUserStatsButton(diceViewModel: DiceViewModel) {
    val context = LocalContext.current
    Button(
        onClick = {
            diceViewModel.viewModelScope.launch {
                val userId = diceViewModel.gameRepository.getCurrentUserId()
                if (userId != null) {
                    diceViewModel.statisticsManager.clearUserStats(userId)
                    Toast.makeText(context, "User stats cleared", Toast.LENGTH_SHORT).show()

                } else {
                    Toast.makeText(context, "No user id", Toast.LENGTH_SHORT).show()

                }
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(id = R.color.primary),
            contentColor = colorResource(id = R.color.buttonTextColor)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("Reset My Stats")
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
                    value = (analysis?.predictedWinRate?.times(100)?.toInt() ?: 0),
                    label = "Predicted\nWin Rate",
                    color = MaterialTheme.colorScheme.primary,
                    suffix = "%"
                )

                StatCircle(
                    value = (analysis?.consistency?.times(100)?.toInt() ?: 0),
                    label = "Consistency",
                    suffix = "%",
                    color = MaterialTheme.colorScheme.secondary
                )

                StatCircle(
                    value = (analysis?.improvement?.times(100)?.toInt() ?: 0),
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
                    text = "Play Style: ${analysis?.playStyle}",
                    style = MaterialTheme.typography.bodyLarge
                )

            }
        }
    }
}

@Composable
private fun ShakeRateAnalysis(shakeRates: List<Pair<Long, Int>>) {
    val graphColor = colorResource(id = R.color.purple_700)
    val gridColor = Color.Gray.copy(alpha = 0.2f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(400.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Shake Rate Analysis", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Shows shake intensity over time (0-10 scale)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            if (shakeRates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No shake data available yet.\nPlay some games to see your shake patterns!",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        val xStep = size.width / 10
                        val yStep = size.height / 10

                        // Draw grid
                        for (i in 0..10) {
                            // Vertical lines
                            drawLine(
                                color = gridColor,
                                start = Offset(i * xStep, 0f),
                                end = Offset(i * xStep, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                            // Horizontal lines
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, i * yStep),
                                end = Offset(size.width, i * yStep),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        if (shakeRates.size > 1) {
                            val path = Path()
                            val points = shakeRates.mapIndexed { index, (_, rate) ->
                                Offset(
                                    x = size.width * (index.toFloat() / (shakeRates.size - 1)),
                                    y = size.height * (1 - rate.toFloat() / 10f) // Scale to 0-10
                                )
                            }

                            // Draw line graph
                            path.moveTo(points.first().x, points.first().y)
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val controlPoint1 = Offset(
                                    x = p1.x + (p2.x - p1.x) / 2f,
                                    y = p1.y
                                )
                                val controlPoint2 = Offset(
                                    x = p1.x + (p2.x - p1.x) / 2f,
                                    y = p2.y
                                )
                                path.cubicTo(
                                    controlPoint1.x, controlPoint1.y,
                                    controlPoint2.x, controlPoint2.y,
                                    p2.x, p2.y
                                )
                            }

                            // Draw filled area under the graph
                            val fillPath = Path().apply {
                                addPath(path)
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                close()
                            }
                            drawPath(
                                path = fillPath,
                                color = graphColor.copy(alpha = 0.1f)
                            )

                            // Draw the line
                            drawPath(
                                path = path,
                                color = graphColor,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )

                            // Draw points
                            points.forEach { point ->
                                drawCircle(
                                    color = graphColor,
                                    radius = 4.dp.toPx(),
                                    center = point
                                )
                            }
                        }
                    }

                    // Y-axis labels
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .align(Alignment.CenterStart)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in 10 downTo 0) {
                            Text(
                                text = "$i",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WinRateGraph(winRates: Map<String, Pair<Int, Int>>) {
    val colorToUse = colorResource(id = R.color.blue)
    val labelWidth = 15.dp // Space for percentage labels

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(300.dp)
            .background(colorResource(id = R.color.surface))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Win Rate Analysis", style = MaterialTheme.typography.headlineMedium)

            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp, end = 32.dp, top = 16.dp, bottom = 16.dp)
                ) {
                    val barWidth =
                        (size.width - labelWidth.toPx() - 32.dp.toPx()) / winRates.size
                    val spacing = barWidth * 0.75f
                    val startX =
                        labelWidth.toPx() + 8.dp.toPx() // Add some padding after labels

                    // Draw grid lines
                    for (i in 0..10) {
                        val y = size.height * (1 - i / 10f)
                        val x = size.width + spacing
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = Offset(startX, y),
                            end = Offset(x, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        // Draw percentage labels
                        drawContext.canvas.nativeCanvas.drawText(
                            "${i * 10}%",
                            2.dp.toPx(),
                            y - 2.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 12.sp.toPx()
                                textAlign = android.graphics.Paint.Align.LEFT
                            }
                        )
                    }

                    // Draw bars
                    winRates.entries.forEachIndexed { index, (game, stats) ->
                        val winRate = if (stats.second > 0) {
                            stats.first.toFloat() / stats.second
                        } else 0f

                        val barHeight = size.height * winRate
                        val x = startX + index * barWidth + spacing

                        // Draw bar
                        drawRoundRect(
                            color = colorToUse,
                            topLeft = Offset(x, size.height - barHeight),
                            size = Size(barWidth - spacing * 2, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )

                        // Draw game name
                        drawContext.canvas.nativeCanvas.drawText(
                            game,
                            x + (barWidth - spacing * 2) / 2,
                            size.height + 16.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 10.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameStatsTable(gameStats: GameStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Game Statistics", style = MaterialTheme.typography.headlineMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TableHeader("Game")
                TableHeader("Played")
                TableHeader("Win Rate")
                TableHeader("High Score")
            }

            GameBoard.entries.forEach { board ->
                val stats = gameStats.winRates[board.modeName]
                val highScore = gameStats.highScores[board.modeName]

                GameStatRow(
                    gameName = board.modeName,
                    gamesPlayed = stats?.second ?: 0,
                    winRate = if (stats != null && stats.second > 0) {
                        (stats.first.toFloat() / stats.second * 100).toInt()
                    } else 0,
                    highScore = highScore ?: 0
                )
            }
        }
    }
}

@Composable
private fun TableHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(8.dp)
    )
}

@Composable
private fun GameStatRow(
    gameName: String,
    gamesPlayed: Int,
    winRate: Int,
    highScore: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(gameName, modifier = Modifier.padding(8.dp))
        Text(gamesPlayed.toString(), modifier = Modifier.padding(8.dp))
        Text("$winRate%", modifier = Modifier.padding(8.dp))
        Text(highScore.toString(), modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun UserProfileCard(
    profile: UserProfile,
    onProfileChange: (UserProfile) -> Unit
) {
    var showGameSelector by remember { mutableStateOf(false) }
    var showAvatarSelector by remember { mutableStateOf(false) }
    var isEditingUsername by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf(profile.name) }

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
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .clickable { showAvatarSelector = true }
            ) {
                Image(
                    painter = painterResource(id = profile.avatar.resourceId),
                    contentDescription = "Profile Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .size(80.dp)
                        .clip(CircleShape)
                        .clickable { showAvatarSelector = true }
                )
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Change Avatar",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.05f))
                        .padding(2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                if (isEditingUsername) {
                    TextField(
                        value = username,
                        onValueChange = { username = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                isEditingUsername = false
                                onProfileChange(profile.copy(name = username))
                            }
                        ),
                        modifier = Modifier.focusRequester(remember { FocusRequester() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        trailingIcon = {
                            IconButton(onClick = {
                                isEditingUsername = false
                                onProfileChange(profile.copy(name = username))
                            }) {
                                Icon(Icons.Default.Check, "Save")
                            }
                        }
                    )
                } else {
                    Text(
                        text = profile.name,
                        modifier = Modifier.clickable { isEditingUsername = true },
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Favorite Game:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = profile.favoriteGames.firstOrNull()
                            ?: "None",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable { showGameSelector = true }
                    )
                }
            }
        }
    }

    if (showAvatarSelector) {
        AvatarSelection(
            currentAvatar = profile.avatar,
            onAvatarSelected = {
                onProfileChange(profile.copy(avatar = it))
                showAvatarSelector = false
            },
            onDismiss = { showAvatarSelector = false }
        )
    }

    if (showGameSelector) {
        GameSelector(
            currentFavorite = profile.favoriteGames.firstOrNull(),
            onGameSelected = { game ->
                onProfileChange(profile.copy(favoriteGames = listOf(game)))
                showGameSelector = false
            },
            onDismiss = { showGameSelector = false }
        )
    }
}

@Composable
private fun GameSelector(
    currentFavorite: String?,
    onGameSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Favorite Game") },
        text = {
            Column {
                GameBoard.entries.forEach { board ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGameSelected(board.modeName) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(board.modeName)
                        if (board.modeName == currentFavorite) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected"
                            )
                        }
                    }
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


@Composable
private fun OverallStatsCard(gameStats: GameStats) {
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
                    value = gameStats.gamesPlayed,
                    label = "Games\nPlayed",
                    color = MaterialTheme.colorScheme.primary
                )

                StatCircle(
                    value = calculateWinRate(gameStats.winRates),
                    label = "Win Rate",
                    suffix = "%",
                    color = MaterialTheme.colorScheme.secondary
                )

                StatCircle(
                    value = gameStats.highScores.values.maxOrNull() ?: 0,
                    label = "Highest\nScore",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun StatCircle(
    value: Int,
    label: String,
    suffix: String = "",
    color: Color
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
    currentAvatar: Avatar,
    onAvatarSelected: (Avatar) -> Unit,
    onDismiss: () -> Unit
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

private fun calculateWinRate(winRates: Map<String, Pair<Int, Int>>): Int {
    val totalWins = winRates.values.sumOf { it.first }
    val totalGames = winRates.values.sumOf { it.second }
    return if (totalGames > 0) {
        ((totalWins.toFloat() / totalGames.toFloat()) * 100).toInt()
    } else 0
}
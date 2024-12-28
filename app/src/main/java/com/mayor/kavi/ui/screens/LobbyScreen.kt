package com.mayor.kavi.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mayor.kavi.R
import com.mayor.kavi.data.models.*
import com.mayor.kavi.ui.Routes
import com.mayor.kavi.ui.viewmodel.*
import com.mayor.kavi.ui.viewmodel.GameViewModel.NavigationEvent
import com.mayor.kavi.util.*
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    navController: NavController,
    appViewModel: AppViewModel,
    gameViewModel: GameViewModel
) {
    val userProfileState by appViewModel.userProfileState.collectAsState()
    var showJoinSessionDialog by remember { mutableStateOf(false) }
    val onlinePlayers by gameViewModel.onlinePlayers.collectAsState()
    val navigationEvent by gameViewModel.navigationEvent.collectAsState(initial = null)
    LocalContext.current
    val scope = rememberCoroutineScope()

    // Set online status when entering/leaving lobby
    LaunchedEffect(Unit) {
        launch {
            gameViewModel.setUserOnlineStatus(true)
        }
        launch {
            gameViewModel.startListeningForOnlinePlayers()
        }
    }

    LaunchedEffect(navigationEvent) {
        Timber.d("NavigationEvent: $navigationEvent")
        when (navigationEvent) {
            is NavigationEvent.NavigateToBoard -> {
                val sessionId = (navigationEvent as NavigationEvent.NavigateToBoard).sessionId
                Timber.d("SessionId: $sessionId, PlayMode: ${gameViewModel.playMode.value}, " +
                        "Board: ${gameViewModel.selectedBoard.value}")
                if (gameViewModel.playMode.value == PlayMode.Multiplayer &&
                    gameViewModel.selectedBoard.value == GameBoard.GREED.modeName
                ) {
                    navController.navigate(Routes.MultiplayerBoard.route + "/${sessionId}") {
                        launchSingleTop = true
                    }
                }
            }

            is NavigationEvent.NavigateBack -> {
                gameViewModel.resetGameSession()
                gameViewModel.stopListeningForOnlinePlayers()
                navController.navigate(Routes.Boards.route) {
                    popUpTo(Routes.Lobby.route) { inclusive = true }
                }
            }

            else -> {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                gameViewModel.setUserOnlineStatus(false)
                gameViewModel.stopListeningForOnlinePlayers()
            }
        }
    }

    BackHandler {
        scope.launch {
            gameViewModel.resetGameSession()
            gameViewModel.stopListeningForOnlinePlayers()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Multiplayer Lobby",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colorResource(id = R.color.primary_container),
                    titleContentColor = colorResource(id = R.color.on_primary_container)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Background with overlay
            Image(
                painter = painterResource(id = R.drawable.background2),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(id = R.color.scrim).copy(alpha = 0.75f))
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Welcome Message
                when (userProfileState) {
                    is Result.Success -> {
                        val profile = (userProfileState as Result.Success).data
                        Text(
                            text = "Welcome, ${profile.name}!",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    offset = Offset(2f, 2f),
                                    blurRadius = 3f
                                )
                            ),
                            textAlign = TextAlign.Center
                        )
                    }

                    is Result.Loading -> {
                        CircularProgressIndicator()
                    }

                    is Result.Error -> {
                        Text(
                            text = "Welcome, Guest!",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Online Players List
                if (onlinePlayers.isNotEmpty()) {
                    Text(
                        text = "Online Players:",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center
                    )
                    OnlinePlayersList(
                        players = onlinePlayers,
                        onChallengeClick = { gameViewModel.createGameSession(it) },
                        currentUserId = gameViewModel.getCurrentUserId()
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Join Game Button
                CustomElevatedButton(
                    text = "Join Game Session",
                    onClick = { showJoinSessionDialog = true }
                )
            }
        }

        if (showJoinSessionDialog) {
            JoinGameSessionDialog(
                onDismiss = { showJoinSessionDialog = false },
                onJoin = { sessionId ->
                    gameViewModel.joinGameSession(sessionId)
                    showJoinSessionDialog = false
                }
            )
        }
    }
}

@Composable
fun JoinGameSessionDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var sessionId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Game Session") },
        text = {
            Column {
                OutlinedTextField(
                    value = sessionId,
                    onValueChange = { sessionId = it },
                    label = { Text("Enter Game ID") }
                )
            }
        },
        confirmButton = {
            CustomButton(text = "Join", onClick = { onJoin(sessionId) })
        },
        dismissButton = {
            CustomButton(text = "Cancel", onClick = onDismiss)
        }
    )
}

@Composable
fun OnlinePlayersList(
    players: List<UserProfile>,
    onChallengeClick: (String) -> Unit,
    currentUserId: String?
) {
    val surfaceVariantColor = colorResource(id = R.color.surface_variant)
    val primaryColor = colorResource(id = R.color.primary)
    val onlinePlayers = remember(players, currentUserId) {
        players.filter { it.id != currentUserId }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = onlinePlayers.size,
            key = { onlinePlayers[it].id }
        ) { index ->
            val player = onlinePlayers[index]
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = surfaceVariantColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = player.avatar.resourceId),
                            contentDescription = "avatar",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column(
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = player.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            val statusText = when {
                                player.isInGame -> "In Game"
                                player.isWaitingForPlayers -> "Waiting for Players"
                                else -> "Online"
                            }
                            val statusColor = when {
                                player.isInGame -> Color.Red
                                player.isWaitingForPlayers -> Color.Yellow
                                else -> Color.Green
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = statusColor
                            )
                        }
                    }
                    player.takeIf { !it.isInGame && !it.isWaitingForPlayers && it.id != currentUserId }
                        ?.let {
                            Button(
                                onClick = { onChallengeClick(player.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor
                                )
                            ) {
                                Text("Challenge")
                            }
                        }
                }
            }
        }
    }
}

@Composable
fun CustomElevatedButton(text: String, onClick: () -> Unit) {
    ElevatedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = colorResource(id = R.color.tertiary_container),
            contentColor = colorResource(id = R.color.on_tertiary_container)
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 6.dp
        )
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun CustomButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(id = R.color.secondary_container),
            contentColor = colorResource(id = R.color.on_secondary_container)
        )
    )
    {
        Text(text)
    }
}
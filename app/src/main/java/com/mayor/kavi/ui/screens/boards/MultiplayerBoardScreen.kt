package com.mayor.kavi.ui.screens.boards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mayor.kavi.R
import com.mayor.kavi.data.manager.SettingsManager.Companion.LocalSettingsManager
import com.mayor.kavi.data.models.*
import com.mayor.kavi.ui.components.*
import com.mayor.kavi.ui.viewmodel.*
import com.mayor.kavi.util.BoardColors
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerBoardScreen(
    viewModel: GameViewModel,
    navController: NavController,
) {
    var showExitDialog by remember { mutableStateOf(false) }
    val navigationEvent by viewModel.navigationEvent.collectAsState(null)
    val playerInfo by viewModel.playerInfo.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val isRolling by viewModel.isRolling.collectAsState()
    val diceImages by viewModel.diceImages.collectAsState()
    val heldDice by viewModel.heldDice.collectAsState()
    val isMyTurn by viewModel.isMyTurn.collectAsState()
    val settingsManager = LocalSettingsManager.current
    val boardColor by settingsManager.getBoardColor().collectAsState(initial = "default")
    val sessionVal = viewModel.gameSession.collectAsState().value

    LaunchedEffect(navigationEvent) {
        when (navigationEvent) {
            is GameViewModel.NavigationEvent.NavigateBack -> {
                navController.navigateUp() // Navigate back to Lobby
                viewModel.resetGameSession() // Reset session information on exit
            }

            is GameViewModel.NavigationEvent.ShowExitDialog -> showExitDialog = true
            else -> {}
        }
    }
    if (sessionVal.players.isEmpty() || !sessionVal.players.any { it.id == viewModel.getCurrentUserId() }) {
        LaunchedEffect(Unit) {
            navController.navigateUp() // Trigger navigation back to Lobby if session or player not found
        }
        return
    }

    BackHandler {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("End Game Session") },
            text = { Text("Are you sure you want to end this game session?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    viewModel.endGameSession()
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    // Check if session exists and player is part of it
    if (sessionVal.players.isEmpty() || sessionVal.players.none { it.id == viewModel.getCurrentUserId() }) {
        LaunchedEffect(Unit) {
            Timber.d("Session is null or user not found in session. Navigating back.")
            navController.navigateUp()
        }
        return
    }

    // Show waiting screen if game hasn't started or we don't have both players
    if (sessionVal.players.size < 2 || sessionVal.gameState.status != "active") {
        WaitingForPlayersScreen(
            players = playerInfo,
            onReadyClick = {
                viewModel.startMultiplayerGame(sessionVal.id)
            },
            currentUserId = viewModel.getCurrentUserId()
        )
        return
    }

    // Game board when all players are ready and game is active
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Greed Multiplayer") },
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
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(
                    when (val color = BoardColors.getColor(boardColor)) {
                        is Color -> Modifier.background(color = color)
                        is Brush -> Modifier.background(brush = color)
                        else -> Modifier.background(color = BoardColors.getColor("default") as Color)
                    }
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Players info and scores
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.surface_variant)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    playerInfo.forEach { player ->
                        PlayerInfoRow(
                            playerInfo = player,
                            isCurrentTurn = player.isCurrentTurn
                        )
                    }
                }
            }

            // Turn indicator
            if (isMyTurn) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(id = R.color.primary)
                    )
                ) {
                    Text(
                        text = "Your Turn!",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp),
                        color = colorResource(id = R.color.on_primary)
                    )
                }
            }

            // Dice Display
            DiceDisplay(
                diceImages = diceImages,
                isRolling = isRolling,
                heldDice = heldDice,
                isMyTurn = isMyTurn,
                onDiceHold = { viewModel.toggleDiceHold(it) },
                diceSize = 80.dp,
                arrangement = DiceArrangement.GRID
            )

            // Turn Score Display
            val greedState = gameState as? GameScoreState.GreedScoreState
            if ((greedState?.turnScore ?: 0) > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(id = R.color.surface_variant)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Turn Score",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${greedState?.turnScore ?: 0}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Game Controls (only enabled on player's turn)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.rollDice() },
                    enabled = isMyTurn && !isRolling,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Roll")
                }
                Button(
                    onClick = { viewModel.endGreedTurn() },
                    enabled = isMyTurn && !isRolling && (greedState?.turnScore ?: 0) > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Bank Score")
                }
            }
        }
    }
}

@Composable
private fun WaitingForPlayersScreen(
    players: List<PlayerInfoData>,
    onReadyClick: () -> Unit,
    currentUserId: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (players.size < 2) "Waiting for opponent..." else "Game ready to start!",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        players.forEach { player ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.surface_variant)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = player.name + if (player.id == currentUserId) " (You)" else "",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Present",
                        color = Color.Green,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (players.size == 2) {
            Button(
                onClick = onReadyClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.primary)
                )
            ) {
                Text("Start Game")
            }
        }
    }
}

@Composable
private fun PlayerInfoRow(
    playerInfo: PlayerInfoData,
    isCurrentTurn: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCurrentTurn) 2.dp else 0.dp,
                color = if (isCurrentTurn) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = playerInfo.name,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Score: ${playerInfo.score}",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

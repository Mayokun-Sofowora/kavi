package com.mayor.kavi.ui.screens.boards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mayor.kavi.R
import com.mayor.kavi.data.manager.SettingsManager.Companion.LocalSettingsManager
import com.mayor.kavi.data.models.*
import com.mayor.kavi.ui.components.*
import com.mayor.kavi.ui.viewmodel.*
import com.mayor.kavi.util.BoardColors
import com.mayor.kavi.util.GameBoard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerBoardScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }
    val navigationEvent by viewModel.navigationEvent.collectAsState(null)
    val session by viewModel.gameSession.collectAsState()
    val playerInfo by viewModel.playerInfo.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val isRolling by viewModel.isRolling.collectAsState()
    val diceImages by viewModel.diceImages.collectAsState()
    val heldDice by viewModel.heldDice.collectAsState()
    val isMyTurn by viewModel.isMyTurn.collectAsState()
    val settingsManager = LocalSettingsManager.current
    val boardColor by settingsManager.getBoardColor().collectAsState(initial = "default")

    LaunchedEffect(navigationEvent) {
        when (navigationEvent) {
            is GameViewModel.NavigationEvent.NavigateBack -> onBack()
            is GameViewModel.NavigationEvent.ShowExitDialog -> showExitDialog = true
            else -> {}
        }
    }

    BackHandler {
        viewModel.onBackPressed()
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("End Game Session") },
            text = { Text("Are you sure you want to end this game session?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    viewModel.endCurrentSession()
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
    if (session.players.isEmpty() || !session.players.any { it.id == viewModel.getCurrentUserId() }) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    // Show waiting screen if game hasn't started or players aren't ready
    if (session.gameState.status != "active" || !session.players.all { it.isReady }) {
        WaitingForPlayersScreen(
            players = playerInfo,
            onReadyClick = { viewModel.setPlayerReady(session.id, true) },
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
                    IconButton(onClick = { viewModel.onBackPressed() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            "Waiting for Players",
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
                        text = if (player.isReady) "Ready" else "Not Ready",
                        color = if (player.isReady) Color.Green else Color.Red,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onReadyClick,
            enabled = currentUserId?.let { uid ->
                players.find { it.id == uid }?.let { !it.isReady }
            } == true,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.primary)
            )
        ) {
            Text("Ready")
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

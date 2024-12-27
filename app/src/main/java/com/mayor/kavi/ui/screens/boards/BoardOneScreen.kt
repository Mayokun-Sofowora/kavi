package com.mayor.kavi.ui.screens.boards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mayor.kavi.R
import com.mayor.kavi.data.manager.SettingsManager.Companion.LocalSettingsManager
import com.mayor.kavi.data.models.*
import com.mayor.kavi.ui.Routes
import com.mayor.kavi.ui.viewmodel.*
import com.mayor.kavi.ui.viewmodel.GameViewModel.Companion.AI_PLAYER_ID
import com.mayor.kavi.ui.components.*
import com.mayor.kavi.util.BoardColors
import com.mayor.kavi.util.GameBoard
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardOneScreen(
    viewModel: GameViewModel = hiltViewModel(),
    navController: NavController,
    onBack: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val isRolling by viewModel.isRolling.collectAsState()
    val diceImages by viewModel.diceImages.collectAsState()
    val heldDice by viewModel.heldDice.collectAsState()
    val isRollAllowed by viewModel.isRollAllowed.collectAsState()
    val settingsManager = LocalSettingsManager.current
    val boardColor by settingsManager.getBoardColor().collectAsState(initial = "default")
    var showExitGameDialog by remember { mutableStateOf(false) }

    // Safe cast with early return if wrong game type
    val pigState = (gameState as? GameScoreState.PigScoreState) ?: run {
        LaunchedEffect(Unit) {
            showExitGameDialog = true
            onBack()
        }
        return
    }

    LaunchedEffect(Unit) {
        viewModel.setSelectedBoard(GameBoard.PIG.modeName)
        viewModel.resetGame()
    }

    // Handle AI turns
    LaunchedEffect(pigState.currentPlayerIndex, isRolling) {
        if (pigState.currentPlayerIndex == AI_PLAYER_ID.hashCode() && !pigState.isGameOver && !isRolling) {
            delay(1000) // Initial delay before AI turn

            // First roll
            viewModel.rollDice()
            delay(2000) // Wait for roll animation

            // After the roll completes, check if AI should bank
            if (!pigState.isGameOver && pigState.currentTurnScore > 0 &&
                viewModel.shouldAIBank(
                    currentTurnScore = pigState.currentTurnScore,
                    aiTotalScore = pigState.playerScores[AI_PLAYER_ID.hashCode()] ?: 0,
                    playerTotalScore = pigState.playerScores[0] ?: 0
                )
            ) {
                delay(1000)
                viewModel.endPigTurn()
            } else if (!pigState.isGameOver && pigState.currentPlayerIndex == AI_PLAYER_ID.hashCode()) {
                // If AI decides to roll again
                delay(1000)
                viewModel.rollDice()
            }
        }
    }

    DisposableEffect(Unit) {
        viewModel.resumeShakeDetection()
        onDispose {
            viewModel.pauseShakeDetection()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pig Dice Game") },
                navigationIcon = {
                    IconButton(onClick = { showExitGameDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(id = R.color.primary_container),
                    titleContentColor = colorResource(id = R.color.on_primary_container)
                ),
                actions = {
                    IconButton(
                        onClick = { navController.navigate(Routes.Settings.route) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            modifier = Modifier.size(24.dp),
                            tint = colorResource(id = R.color.on_primary_container)
                        )
                    }
                }
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
            // Score Display
            ScoreDisplay(
                gameMode = "Pig",
                scores = pigState.playerScores,
                currentTurnScore = pigState.currentTurnScore,
                message = pigState.message,
                currentPlayerIndex = pigState.currentPlayerIndex
            )

            // Dice Display (Pig only uses one die)
            DiceDisplay(
                diceImages = diceImages.take(1),
                isRolling = isRolling,
                heldDice = heldDice,
                isMyTurn = pigState.currentPlayerIndex != AI_PLAYER_ID.hashCode(),
                onDiceHold = null, // Pig doesn't use held dice
                diceSize = 150.dp,
                arrangement = DiceArrangement.ROW
            )

            // Turn Score Display
            if (pigState.currentTurnScore > 0) {
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
                            text = "${pigState.currentTurnScore}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Game Controls
            GameControls(
                onRoll = { viewModel.rollDice() },
                onEndTurn = if (pigState.currentPlayerIndex == AI_PLAYER_ID.hashCode()) null
                else { -> viewModel.endPigTurn() },
                isRolling = isRolling,
                canReroll = isRollAllowed && !pigState.isGameOver &&
                        pigState.currentPlayerIndex != AI_PLAYER_ID.hashCode()
            )
        }
    }

    // Game End Dialog
    if (pigState.isGameOver) {
        GameEndDialog(
            message = pigState.message,
            onPlayAgain = { viewModel.resetGame() },
            onExit = onBack
        )
    }

    // Exit Game Dialog
    if (showExitGameDialog) {
        AlertDialog(
            onDismissRequest = { showExitGameDialog = false },
            title = { Text("Exit Game") },
            text = { Text("Are you sure you want to exit the game? Your progress will be lost.") },
            confirmButton = {
                TextButton(onClick = onBack) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitGameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

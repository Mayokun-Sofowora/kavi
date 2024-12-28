package com.mayor.kavi.ui.screens.boards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mayor.kavi.R
import com.mayor.kavi.data.manager.SettingsManager.Companion.LocalSettingsManager
import com.mayor.kavi.data.models.*
import com.mayor.kavi.ui.Routes
import com.mayor.kavi.ui.viewmodel.*
import com.mayor.kavi.ui.components.*
import com.mayor.kavi.ui.viewmodel.GameViewModel.Companion.AI_PLAYER_ID
import com.mayor.kavi.util.BoardColors
import com.mayor.kavi.util.GameBoard
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardTwoScreen(
    viewModel: GameViewModel = hiltViewModel(),
    navController: NavController
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
    val greedState = (gameState as? GameScoreState.GreedScoreState) ?: run {
        LaunchedEffect(Unit) {
            showExitGameDialog = true
            navController.navigateUp()
        }
        return
    }

    BackHandler {
        showExitGameDialog = true
    }
    LaunchedEffect(Unit) {
        viewModel.setSelectedBoard(GameBoard.GREED.modeName)
        viewModel.resetGame()
    }

    // Handle AI turns
    LaunchedEffect(greedState.currentPlayerIndex, isRolling) {
        if (greedState.currentPlayerIndex == AI_PLAYER_ID.hashCode() && !greedState.isGameOver &&
            !isRolling
        ) {
            delay(500) // Short initial delay

            // Keep rolling until AI decides to bank or busts
            while (greedState.currentPlayerIndex == AI_PLAYER_ID.hashCode() &&
                !greedState.isGameOver
            ) {
                // Roll the dice
                viewModel.rollDice()
                delay(100) // Wait for roll animation and to show result

                // If the turn score is 0, AI busted
                if (greedState.turnScore == 0) {
                    delay(500) // Wait a moment to show the bust
                    viewModel.endGreedTurn()
                    break
                }

                // Check if AI should bank
                if (viewModel.shouldAIBank(
                        currentTurnScore = greedState.turnScore,
                        aiTotalScore = greedState.playerScores[AI_PLAYER_ID.hashCode()] ?: 0,
                        playerTotalScore = greedState.playerScores[0] ?: 0
                    )
                ) {
                    delay(500) // Wait a moment before banking
                    viewModel.endGreedTurn()
                    break
                }

                delay(1000) // Wait before potentially rolling again
            }
        }
    }

    // Reset held dice when player changes
    LaunchedEffect(greedState.currentPlayerIndex) {
        viewModel.resetHeldDice()
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
                title = { Text("Greed Dice Game") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Score Display
            ScoreDisplay(
                gameMode = "Greed",
                scores = greedState.playerScores,
                currentTurnScore = greedState.turnScore,
                message = greedState.message,
                currentPlayerIndex = greedState.currentPlayerIndex
            )

            // Dice Display with holdable dice
            DiceDisplay(
                diceImages = diceImages,
                isRolling = isRolling,
                heldDice = heldDice,
                isMyTurn = greedState.currentPlayerIndex != AI_PLAYER_ID.hashCode(),
                onDiceHold = if (greedState.currentPlayerIndex != AI_PLAYER_ID.hashCode()
                    && !isRolling
                ) { index ->
                    viewModel.toggleDiceHold(index)
                } else null,
                diceSize = 120.dp,
                arrangement = DiceArrangement.GRID
            )

            // Scoring Combinations Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.surface_variant)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Scoring Combinations",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val scoringCombinations = listOf(
                        "Straight (1-2-3-4-5-6): 1000",
                        "Six of a Kind: Five of a kind × 2",
                        "Five of a Kind: Four of a kind × 2",
                        "Four of a Kind: Three of a kind × 2",
                        "Three Pairs: 1000",
                        "Three of a Kind (of 1): 1000",
                        "Three of a Kind: Number × 100",
                        "Single 1: 100",
                        "Single 5: 50"
                    )

                    scoringCombinations.forEach { combination ->
                        Text(
                            text = combination,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Game Controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                GameControls(
                    onRoll = { viewModel.rollDice() },
                    onEndTurn = if (greedState.currentPlayerIndex == AI_PLAYER_ID.hashCode()) null
                    else { -> viewModel.endGreedTurn() },
                    isRolling = isRolling,
                    canReroll = isRollAllowed &&
                            !greedState.isGameOver &&
                            greedState.currentPlayerIndex != AI_PLAYER_ID.hashCode() &&
                            greedState.canReroll
                )
            }
        }
    }

    // Game End Dialog
    if (greedState.isGameOver) {
        GameEndDialog(
            message = greedState.message,
            onPlayAgain = { viewModel.resetGame() },
            onExit = navController::navigateUp
        )
    }

    // Exit Game Dialog
    if (showExitGameDialog) {
        AlertDialog(
            onDismissRequest = { showExitGameDialog = false },
            title = { Text("Exit Game") },
            text = { Text("Are you sure you want to exit the game? Your progress will be lost.") },
            confirmButton = {
                TextButton(onClick = navController::navigateUp) {
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
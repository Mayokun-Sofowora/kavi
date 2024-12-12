package com.mayor.kavi.ui.screens.boards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.*
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mayor.kavi.R
import com.mayor.kavi.ui.Routes
import com.mayor.kavi.ui.viewmodel.*
import com.mayor.kavi.util.DiceResultImage
import com.mayor.kavi.ui.components.DiceRollAnimation
import com.mayor.kavi.ui.viewmodel.DiceViewModel.GameMode
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardOneScreen(
    viewModel: DiceViewModel = hiltViewModel(),
    navController: NavController
) {
    var showExitGameDialog by remember { mutableStateOf(false) }
    val scoreState by viewModel.scoreState.collectAsState()
    val diceImages by viewModel.diceImages.collectAsState()
    var isRolling by remember { mutableStateOf(false) }
    var showWinDialog by remember { mutableStateOf(false) }
    val shakeEnabled by viewModel.shakeEnabled.observeAsState(initial = false)
    val gameMode by viewModel.gameMode.collectAsState()
    val isAITurn = gameMode is GameMode.VsAI && !isRolling

    // Add BackHandler to prevent back navigation during game
    BackHandler(enabled = true) {
        if (showWinDialog) {
            // Do nothing when dialog is shown
            return@BackHandler
        } else {
            // Show confirmation dialog before exiting game
            showExitGameDialog = true
        }
    }

    // Start shake detection when screen is active
    LaunchedEffect(Unit) {
        viewModel.setSelectedBoard(GameBoard.PIG.modeName)
        if (shakeEnabled) {
            viewModel.startShakeDetection()
        }
    }

    // Cleanup shake detection when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopShakeDetection()
        }
    }

    // Listen for shake events
    LaunchedEffect(diceImages) {
        if (diceImages.firstOrNull() != R.drawable.empty_dice && !isRolling) {
            isRolling = true
            delay(2000)
            isRolling = false
        }
    }

    // Check for game over condition
    LaunchedEffect(scoreState) {
        showWinDialog = scoreState.isGameOver
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (isAITurn) "Pig Dice Game (AI's Turn)"
                            else "Pig Dice Game"
                        )
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dice display area
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // First row of dice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DiceRollAnimation(
                            isRolling = isRolling,
                            diceImage = diceImages.firstOrNull() ?: R.drawable.empty_dice,
                            modifier = Modifier.size(150.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Score display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(id = R.color.surface_variant),
                        contentColor = colorResource(id = R.color.on_surface_variant)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Pig Dice",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = scoreState.resultMessage,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Divider(
                            modifier = Modifier.fillMaxWidth(0.8f),
                            color = colorResource(id = R.color.on_surface_variant)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Overall Score",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "${scoreState.overallScore}",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Turn Score",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "${scoreState.currentTurnScore}",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                }

                // Game controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isRolling = true
                            viewModel.rollDice(GameBoard.PIG.modeName)
                            MainScope().launch {
                                delay(2000)
                                isRolling = false
                                if (gameMode is GameMode.VsAI) {
                                    viewModel.processAITurn()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.primary),
                            contentColor = colorResource(id = R.color.on_primary),
                            disabledContainerColor = colorResource(id = R.color.outline),
                            disabledContentColor = colorResource(id = R.color.on_surface_variant)
                        ),
                        modifier = Modifier.weight(1f),
                        enabled = !isRolling && !isAITurn
                    ) {
                        Text("Roll")
                    }

                    Button(
                        onClick = {
                            viewModel.endTurn(GameBoard.PIG.modeName)
                            if (gameMode is GameMode.VsAI) {
                                viewModel.processAITurn()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.primary),
                            contentColor = colorResource(id = R.color.on_primary)
                        ),
                        modifier = Modifier.weight(1f),
                        enabled = !isAITurn
                    ) {
                        Text("End Turn")
                    }
                }

                DiceResultImage(
                    gameMode = GameBoard.PIG.modeName,
                    diceResult = scoreState.resultMessage
                )
            }
        }

        if (showWinDialog) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AlertDialog(
                    containerColor = colorResource(id = R.color.surface),
                    titleContentColor = colorResource(id = R.color.on_surface),
                    textContentColor = colorResource(id = R.color.on_surface),
                    onDismissRequest = { showWinDialog = false },
                    title = { Text("Congratulations!") },
                    text = { Text("You win with ${scoreState.overallScore} points!") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.resetGame()
                                showWinDialog = false
                            }, colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(id = R.color.primary),
                                contentColor = colorResource(id = R.color.on_primary)
                            )
                        ) {
                            Text("Play Again")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                viewModel.resetGame()
                                navController.navigate(Routes.Boards.route) {
                                    popUpTo(Routes.Boards.route) { inclusive = true }
                                }
                                showWinDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(id = R.color.primary),
                                contentColor = colorResource(id = R.color.on_primary)
                            )
                        ) {
                            Text("Exit")
                        }
                    }
                )
                ConfettiAnimation()
            }
        }
    }

    // Add Exit Game Dialog
    if (showExitGameDialog) {
        AlertDialog(
            onDismissRequest = { showExitGameDialog = false },
            title = { Text("Exit Game?") },
            text = { Text("Are you sure you want to exit? Your progress will be lost.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetGame()
                        navController.navigate(Routes.Boards.route) {
                            popUpTo(Routes.Boards.route) { inclusive = true }
                        }
                    }, colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.primary),
                        contentColor = colorResource(id = R.color.on_primary)
                    )
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showExitGameDialog = false }, colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.primary),
                        contentColor = colorResource(id = R.color.on_primary)
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

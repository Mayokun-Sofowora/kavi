package com.mayor.kavi.ui.screens.boards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.*
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mayor.kavi.R
import com.mayor.kavi.data.games.BoardColors
import com.mayor.kavi.data.games.GameBoard
import com.mayor.kavi.data.manager.LocalSettingsManager
import com.mayor.kavi.ui.Routes
import com.mayor.kavi.ui.viewmodel.*
import com.mayor.kavi.util.DiceResultImage
import com.mayor.kavi.ui.components.DiceRollAnimation
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardTwoScreen(
    viewModel: DiceViewModel = hiltViewModel(),
    navController: NavController
) {
    var showExitGameDialog by remember { mutableStateOf(false) }
    val scoreState by viewModel.scoreState.collectAsState()
    val diceImages by viewModel.diceImages.collectAsState()
    val isRolling by viewModel.isRolling.collectAsState()
    var showWinDialog by remember { mutableStateOf(false) }
    val settingsManager = LocalSettingsManager.current
    val boardColor by settingsManager.getBoardColor().collectAsState(initial = "default")

    // Back handler
    BackHandler(enabled = true) {
        if (showWinDialog) {
            return@BackHandler
        } else {
            showExitGameDialog = true
        }
    }

    // Check for game over condition
    LaunchedEffect(scoreState) {
        showWinDialog = scoreState.isGameOver
    }

    DisposableEffect(Unit) {
        viewModel.resumeShakeDetection()
        onDispose {
            viewModel.pauseShakeDetection()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Greed (10,000)") },
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
                    .background(color = BoardColors.getColor(boardColor))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        diceImages.take(3).forEach { diceImage ->
                            DiceRollAnimation(
                                isRolling = isRolling,
                                diceImage = diceImage,
                                modifier = Modifier.size(100.dp)
                            )
                        }
                    }

                    // Second row of dice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        diceImages.drop(3).take(3).forEach { diceImage ->
                            DiceRollAnimation(
                                isRolling = isRolling,
                                diceImage = diceImage,
                                modifier = Modifier.size(100.dp)
                            )
                        }
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
                            text = "Greed Dice",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = scoreState.resultMessage,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        HorizontalDivider(
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
                                    text = "Total Score",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "${scoreState.overallScore}",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Bank Score",
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
                            viewModel.rollDice(GameBoard.GREED.modeName)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.primary),
                            contentColor = colorResource(id = R.color.on_primary),
                            disabledContainerColor = colorResource(id = R.color.outline),
                            disabledContentColor = colorResource(id = R.color.on_surface_variant)
                        ),
                        modifier = Modifier.weight(1f),
                        enabled = !isRolling
                    ) {
                        Text("Roll")
                    }

                    Button(
                        onClick = { viewModel.endTurn(GameBoard.GREED.modeName) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.primary),
                            contentColor = colorResource(id = R.color.on_primary),
                            disabledContainerColor = colorResource(id = R.color.outline),
                            disabledContentColor = colorResource(id = R.color.on_surface_variant)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Bank Score")
                    }
                }

                DiceResultImage(
                    gameMode = GameBoard.GREED.modeName,
                    diceResult = scoreState.resultMessage
                )

            }
        }

        // Win dialog
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
                    text = { Text("You reached 10,000 points!") },
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
                            }, colors = ButtonDefaults.buttonColors(
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

        // Exit dialog
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
                        onClick = { showExitGameDialog = false },
                        colors = ButtonDefaults.buttonColors(
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
}
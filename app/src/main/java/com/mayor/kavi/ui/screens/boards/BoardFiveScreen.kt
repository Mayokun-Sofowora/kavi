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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mayor.kavi.ui.Routes
import com.mayor.kavi.ui.components.DiceRollAnimation
import com.mayor.kavi.ui.viewmodel.*
import com.mayor.kavi.util.DiceResultImage
import kotlinx.coroutines.*
import com.mayor.kavi.R
import com.mayor.kavi.data.games.*
import com.mayor.kavi.data.manager.LocalSettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardFiveScreen(
    viewModel: DiceViewModel = hiltViewModel(),
    navController: NavController
) {
    var showExitGameDialog by remember { mutableStateOf(false) }
    val diceImages by viewModel.diceImages.collectAsState()
    val scoreState by viewModel.scoreState.collectAsState()
    val isRolling by viewModel.isRolling.collectAsState()
    var showWinDialog by remember { mutableStateOf(false) }
    val balutState by viewModel.balutScoreState.collectAsState()
    val settingsManager = LocalSettingsManager.current
    val boardColor by settingsManager.getBoardColor().collectAsState(initial = "default")

    // Add BackHandler to prevent back navigation during game
    BackHandler(enabled = true) {
        if (showWinDialog) {
            return@BackHandler
        } else {
            showExitGameDialog = true
        }
    }

    // Check for game over condition which is when all categories are completed
    LaunchedEffect(balutState) {
        showWinDialog = balutState.currentRound >= BalutScoreState.categories.size
    }

    DisposableEffect(Unit) {
        viewModel.resumeShakeDetection()
        onDispose {
            viewModel.pauseShakeDetection()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Balut") },
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
                        diceImages.take(2).forEach { diceImage ->
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
                        diceImages.drop(2).take(2).forEach { diceImage ->
                            DiceRollAnimation(
                                isRolling = isRolling,
                                diceImage = diceImage,
                                modifier = Modifier.size(100.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Category and Rolls Display
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
                            text = "Category: ${balutState.currentCategory}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Rolls Left: ${balutState.rollsLeft}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Roll Button
                Button(
                    onClick = {
                        viewModel.rollDice(GameBoard.BALUT.modeName)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.primary),
                        contentColor = colorResource(id = R.color.on_primary),
                        disabledContainerColor = colorResource(id = R.color.outline),
                        disabledContentColor = colorResource(id = R.color.on_surface_variant)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isRolling && balutState.rollsLeft > 0
                ) {
                    Text(if (balutState.rollsLeft > 0) "Roll (${balutState.rollsLeft} left)" else "Next Category")
                }

                DiceResultImage(
                    gameMode = GameBoard.BALUT.modeName,
                    diceResult = scoreState.resultMessage
                )
            }
        }

        // Win Dialog
        if (showWinDialog) {
            AlertDialog(
                onDismissRequest = { navController.popBackStack() },
                title = { Text("Game Complete!") },
                text = { Text("Congratulations! You've completed all categories of Balut!") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetGame()
                            showWinDialog = false
                        }, colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.primary),
                            contentColor = colorResource(id = R.color.on_primary),
                            disabledContainerColor = colorResource(id = R.color.outline),
                            disabledContentColor = colorResource(id = R.color.on_surface_variant)
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
                            contentColor = colorResource(id = R.color.on_primary),
                            disabledContainerColor = colorResource(id = R.color.outline),
                            disabledContentColor = colorResource(id = R.color.on_surface_variant)
                        )
                    ) {
                        Text("Exit")
                    }
                }
            )
            ConfettiAnimation()
        }


        // Add Exit Game Dialog
        if (showExitGameDialog) {
            AlertDialog(containerColor = colorResource(id = R.color.surface),
                titleContentColor = colorResource(id = R.color.on_surface),
                textContentColor = colorResource(id = R.color.on_surface),
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
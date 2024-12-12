package com.mayor.kavi.ui.screens.boards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mayor.kavi.ui.Routes
import com.mayor.kavi.ui.components.DiceRollAnimation
import com.mayor.kavi.ui.viewmodel.ConfettiAnimation
import com.mayor.kavi.ui.viewmodel.DiceViewModel
import com.mayor.kavi.ui.viewmodel.GameBoard
import com.mayor.kavi.util.DiceResultImage
import kotlinx.coroutines.*
import com.mayor.kavi.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardFourScreen(
    viewModel: DiceViewModel = hiltViewModel(),
    navController: NavController
) {
    var showExitGameDialog by remember { mutableStateOf(false) }
    val scoreState by viewModel.scoreState.collectAsState()
    val chicagoState by viewModel.chicagoScoreState.collectAsState()
    val diceImages by viewModel.diceImages.collectAsState()
    var isRolling by remember { mutableStateOf(false) }
    var showWinDialog by remember { mutableStateOf(false) }

    BackHandler {
        showExitGameDialog = true
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chicago") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dice display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                diceImages.take(2).forEach { diceImage ->
                    DiceRollAnimation(
                        isRolling = isRolling,
                        diceImage = diceImage,
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
                        text = "Round ${chicagoState.currentRound}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Target Number: ${chicagoState.currentRound}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = scoreState.resultMessage,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Divider(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        color = colorResource(id = R.color.on_secondary_container)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total Score",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "${chicagoState.totalScore}",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Round Score",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "${chicagoState.roundScore}",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    isRolling = true
                    viewModel.rollDice(GameBoard.CHICAGO.modeName)
                    MainScope().launch {
                        delay(2000)
                        isRolling = false
                    }
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
                enabled = !isRolling
            ) {
                Text("Roll")
            }
            DiceResultImage(
                gameMode = GameBoard.CHICAGO.modeName,
                diceResult = scoreState.resultMessage
            )
        }
    }

    // Win Dialog
    if (showWinDialog) {
        AlertDialog(
            onDismissRequest = { navController.popBackStack() },
            title = { Text("Game Complete!") },
            text = { Text("Congratulations! You've completed all rounds of Chicago!") },
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

    if (showExitGameDialog) {
        AlertDialog(
            containerColor = colorResource(id = R.color.surface),
            titleContentColor = colorResource(id = R.color.on_surface),
            textContentColor = colorResource(id = R.color.on_surface),
            onDismissRequest = { showExitGameDialog = false },
            title = { Text("Exit Game") },
            text = { Text("Are you sure you want to exit? Your progress will be lost.") },
            confirmButton = {
                TextButton(
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
                TextButton(
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
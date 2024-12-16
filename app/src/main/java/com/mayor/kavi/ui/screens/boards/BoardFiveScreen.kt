// BoardFiveScreen.kt
package com.mayor.kavi.ui.screens.boards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mayor.kavi.ui.Routes
import com.mayor.kavi.ui.viewmodel.*
import com.mayor.kavi.R
import com.mayor.kavi.data.games.*
import com.mayor.kavi.data.manager.LocalSettingsManager
import com.mayor.kavi.ui.components.DiceRollAnimation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardFiveScreen(
    viewModel: DiceViewModel = hiltViewModel(),
    navController: NavController
) {
    var showExitGameDialog by remember { mutableStateOf(false) }
    val diceImages by viewModel.diceImages.collectAsState()
    val isRolling by viewModel.isRolling.collectAsState()
    var showWinDialog by remember { mutableStateOf(false) }
    val balutState by viewModel.balutScoreState.collectAsState()
    val settingsManager = LocalSettingsManager.current
    val boardColor by settingsManager.getBoardColor().collectAsState(initial = "default")
    val heldDice by viewModel.heldDice.collectAsState()

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

    Box(modifier = Modifier.fillMaxSize()) {
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
                    .then(
                        when (val color = BoardColors.getColor(boardColor)) {
                            is Color -> Modifier.background(color = color)
                            is Brush -> Modifier.background(brush = color)
                            else -> Modifier.background(color = BoardColors.getColor("default") as Color)
                        }
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Dice display with hold functionality
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        diceImages.take(3).forEachIndexed { index, diceImage ->
                            Box(
                                modifier = Modifier
                                    .clickable(
                                        enabled = !isRolling && balutState.rollsLeft > 0,
                                        onClick = { viewModel.toggleDiceHold(index) }
                                    )
                                    .background(
                                        color = if (heldDice.contains(index)) colorResource(id = R.color.scrim).copy(
                                            alpha = 0.2f
                                        ) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp) // Apply rounded corners to the background too
                                    )
                                    .padding(4.dp)
                            ) {
                                DiceRollAnimation(
                                    isRolling = isRolling && !heldDice.contains(index),
                                    diceImage = diceImage,
                                    modifier = Modifier.size(100.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        diceImages.drop(3).take(2).forEachIndexed { index, diceImage ->
                            val index = index + 3
                            Box(
                                modifier = Modifier
                                    .clickable(
                                        enabled = !isRolling && balutState.rollsLeft > 0,
                                        onClick = { viewModel.toggleDiceHold(index) }
                                    )
                                    .background(
                                        color = if (heldDice.contains(index)) colorResource(id = R.color.scrim).copy(
                                            alpha = 0.2f
                                        ) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp) // Apply rounded corners to the background too
                                    )
                                    .padding(4.dp)
                            ) {
                                DiceRollAnimation(
                                    isRolling = isRolling && !heldDice.contains(index),
                                    diceImage = diceImage,
                                    modifier = Modifier.size(100.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

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
                            text = "Round: ${balutState.currentRound}/${balutState.maxRounds}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Rolls Left: ${balutState.rollsLeft}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Score categories
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(BalutScoreState.categories) { category ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable(
                                    enabled = category == balutState.currentCategory && !isRolling,
                                    onClick = { viewModel.scoreCurrentDice() }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    category == balutState.currentCategory -> colorResource(id = R.color.primary)
                                    balutState.categoryScores.containsKey(category) -> colorResource(
                                        id = R.color.surface_variant
                                    )

                                    else -> colorResource(id = R.color.surface)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = category)
                                Text(text = balutState.categoryScores[category]?.toString() ?: "-")
                            }
                        }
                    }
                }

                // Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { viewModel.rollDice(GameBoard.BALUT.modeName) },
                        enabled = !isRolling && balutState.rollsLeft > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.primary),
                            contentColor = colorResource(id = R.color.on_primary)
                        )
                    ) {
                        Text(if (balutState.rollsLeft > 0) "Roll (${balutState.rollsLeft} left)" else "Next Category")
                    }
                    Button(
                        onClick = { viewModel.scoreCurrentDice() },
                        enabled = balutState.rollsLeft > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.primary),
                            contentColor = colorResource(id = R.color.on_primary)
                        )
                    ) {
                        Text("Score")
                    }
                }
            }
        }

        // Win Dialog
        if (showWinDialog) {
            AlertDialog(
                onDismissRequest = { navController.popBackStack() },
                title = { Text("Congratulations!") },
                text = { Text("You've completed all categories of Balut!") },
                confirmButton = {
                    Button(
                        onClick = {
                            showWinDialog = false
                            viewModel.resetGame()
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
                            navController.navigate(Routes.Boards.route) {
                                popUpTo(Routes.Boards.route) { inclusive = true }
                            }
                            showWinDialog = false
                            viewModel.resetGame()
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
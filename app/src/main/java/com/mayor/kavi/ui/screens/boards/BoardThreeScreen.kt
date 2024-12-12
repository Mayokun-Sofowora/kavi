package com.mayor.kavi.ui.screens.boards

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.*
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mayor.kavi.R
import com.mayor.kavi.ui.Routes
import com.mayor.kavi.ui.components.DiceRollAnimation
import com.mayor.kavi.ui.viewmodel.*
import com.mayor.kavi.util.DiceResultImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardThreeScreen(
    viewModel: DiceViewModel = hiltViewModel(),
    navController: NavController
) {
    var showExitGameDialog by remember { mutableStateOf(false) }
    val scoreState by viewModel.scoreState.collectAsState()
    val diceImages by viewModel.diceImages.collectAsState()
    var isRolling by remember { mutableStateOf(false) }
    var showWinDialog by remember { mutableStateOf(false) }
    val mexicoState by viewModel.mexicoScoreState.collectAsState()
    val shakeEnabled by viewModel.shakeEnabled.observeAsState(initial = false)

    BackHandler(enabled = true) {
        if (showWinDialog) {
            return@BackHandler
        } else {
            showExitGameDialog = true
        }
    }
    // Game initialization and cleanup (similar to BoardOneScreen)
    LaunchedEffect(Unit) {
        viewModel.setSelectedBoard(GameBoard.MEXICO.modeName)
        if (shakeEnabled) {
            viewModel.startShakeDetection()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopShakeDetection()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Mexico") },
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
                            text = "Mexico Dice",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (mexicoState.isFirstRound) "First Round" else "Round ${mexicoState.currentRound}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = scoreState.resultMessage,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Divider(
                            modifier = Modifier.fillMaxWidth(0.8f),
                            color = colorResource(id = R.color.on_tertiary_container)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Lives",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "${mexicoState.lives}",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Current Score",
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

                // Roll button
                Button(
                    onClick = {
                        isRolling = true
                        viewModel.rollDice(GameBoard.MEXICO.modeName)
                        kotlinx.coroutines.MainScope().launch {
                            kotlinx.coroutines.delay(2000)
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
                    gameMode = GameBoard.MEXICO.modeName,
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
                    onDismissRequest = { showWinDialog = false },
                    title = { Text("¡México!") },
                    text = { Text("You rolled a ${scoreState.resultMessage}!") },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.resetGame()
                            showWinDialog = false
                        }, colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.primary),
                            contentColor = colorResource(id = R.color.on_primary)
                        )) {
                            Text("Play Again")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            viewModel.resetGame()
                            navController.navigate(Routes.Boards.route) {
                                popUpTo(Routes.Boards.route) { inclusive = true }
                            }
                            showWinDialog = false
                        },colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.primary),
                            contentColor = colorResource(id = R.color.on_primary)
                        ),
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
                containerColor = colorResource(id = R.color.surface),
                titleContentColor = colorResource(id = R.color.on_surface),
                textContentColor = colorResource(id = R.color.on_surface),
                onDismissRequest = { showExitGameDialog = false },
                title = { Text("Exit Game?") },
                text = { Text("Are you sure you want to exit? Your progress will be lost.") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.resetGame()
                        navController.navigate(Routes.Boards.route) {
                            popUpTo(Routes.Boards.route) { inclusive = true }
                        }
                    },colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.primary),
                        contentColor = colorResource(id = R.color.on_primary)
                    )) {
                        Text("Exit")
                    }
                },
                dismissButton = {
                    Button(onClick = { showExitGameDialog = false }, colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.primary),
                        contentColor = colorResource(id = R.color.on_primary)
                    )) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
package com.mayor.kavi.ui.screens.boards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mayor.kavi.data.manager.games.MyGameManager
import com.mayor.kavi.data.models.GameScoreState
import com.mayor.kavi.ui.components.DiceDisplay
import com.mayor.kavi.ui.viewmodel.GameViewModel
import com.mayor.kavi.util.GameBoard
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.colorResource
import androidx.navigation.NavController
import com.mayor.kavi.R
import com.mayor.kavi.data.manager.SettingsManager.Companion.LocalSettingsManager
import com.mayor.kavi.ui.Routes
import com.mayor.kavi.util.BoardColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardFourScreen(
    viewModel: GameViewModel = hiltViewModel(),
    navController: NavController,
    onBack: () -> Unit
) {
    val gameState = viewModel.gameState.collectAsState()
    val isRolling by viewModel.isRolling.collectAsState()
    val diceImages by viewModel.diceImages.collectAsState()
    var gameName by remember { mutableStateOf("") }
    var diceCount by remember { mutableStateOf(MyGameManager.DEFAULT_DICE.toString()) }
    var newPlayerName by remember { mutableStateOf("") }
    var selectedPlayerIndex by remember { mutableIntStateOf(0) }
    var scoreInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val settingsManager = LocalSettingsManager.current
    val boardColor by settingsManager.getBoardColor().collectAsState(initial = "default")

    // Initialize game state when the screen is first loaded
    LaunchedEffect(Unit) {
        viewModel.setSelectedBoard(GameBoard.CUSTOM.modeName)
        viewModel.resetGame()
    }

    DisposableEffect(Unit) {
        viewModel.resumeShakeDetection()
        onDispose {
            viewModel.pauseShakeDetection()
        }
    }

    // Safe cast with early return if wrong game type
    val customState = (gameState.value as? GameScoreState.CustomScoreState) ?: run {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customState.gameName.ifEmpty { "Custom Dice Board" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(id = R.color.primary_container),
                    titleContentColor = colorResource(id = R.color.on_primary_container)
                ),
                actions = {
                    IconButton(onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            val gameData = buildString {
                                append("Game: ${customState.gameName}\n")
                                append("Players:\n")
                                customState.playerNames.forEach { (index, name) ->
                                    append("$name: ${customState.playerScores[index] ?: 0}\n")
                                    customState.scoreHistory[index]?.forEach { note ->
                                        append("  • $note\n")
                                    }
                                }
                            }
                            putExtra(Intent.EXTRA_TEXT, gameData)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Game"))
                    }) {
                        Icon(Icons.Default.Share, "Share")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(
                    when (val color = BoardColors.getColor(boardColor)) {
                        is Color -> Modifier.background(color = color)
                        is Brush -> Modifier.background(brush = color)
                        else -> Modifier.background(color = BoardColors.getColor("default") as Color)
                    }
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Game Settings Section
            item {
                GameSettingsCard(gameName, onGameNameChange = {
                    gameName = it
                    viewModel.setGameName(it)
                }, diceCount, onDiceCountChange = { dice ->
                    if (true) {
                        diceCount = dice.toString()
                        dice.let { count -> viewModel.setDiceCount(count) }
                    }
                })
            }

            // Dice Display
            item {
                DiceCard(isRolling = isRolling, diceImages = diceImages, viewModel = viewModel)
            }

            // Player Management
            item {
                PlayerManagementCard(customState,
                    newPlayerName,
                    onNewPlayerNameChange = { newPlayerName = it },
                    selectedPlayerIndex,
                    onSelectedPlayerChange = { selectedPlayerIndex = it },
                    onAddPlayer = { viewModel.addPlayer() },
                    onUpdatePlayer = {
                        viewModel.updatePlayerName(
                            selectedPlayerIndex,
                            newPlayerName
                        ); newPlayerName = ""
                    }
                )
            }

            // Score Management
            item {
                ScoreManagementCard(
                    scoreInput,
                    onScoreInputChange = { newScore ->
                        if (newScore.isEmpty() || newScore.toIntOrNull() != null) {
                            scoreInput = newScore
                        }
                    },
                    noteInput,
                    onNoteInputChange = { noteInput = it },
                    onAddScore = {
                        scoreInput.toIntOrNull()?.let { score ->
                            viewModel.addScore(selectedPlayerIndex, score)
                            scoreInput = ""
                        }
                    },
                    onAddNote = {
                        if (noteInput.isNotEmpty()) {
                            viewModel.addNote(selectedPlayerIndex, noteInput)
                            noteInput = ""
                        }
                    }
                )
            }

            // Score History
            item {
                ScoreHistoryCard(customState, selectedPlayerIndex)
            }

            // Action Buttons
            item {
                ActionButtonsCard(
                    onResetScores = { viewModel.resetScores() })
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun GameSettingsCard(
    gameName: String, onGameNameChange: (String) -> Unit,
    diceCount: String, onDiceCountChange: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.surface_variant),
            contentColor = colorResource(id = R.color.on_surface_variant)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Game Settings", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = gameName,
                onValueChange = onGameNameChange,
                label = { Text("Game Name") },
                modifier = Modifier.fillMaxWidth()
            )

            DiceCountSelector(
                label = { Text("Dice Count") },
                diceCount = diceCount.toIntOrNull() ?: 0,
                onDiceCountChange = onDiceCountChange,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun DiceCountSelector(
    label: @Composable (() -> Unit)? = null,
    diceCount: Int,
    onDiceCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minDice: Int = 1,
    maxDice: Int = 6
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.sizeIn(minWidth = 200.dp).padding(16.dp).fillMaxWidth()
    ) {
        // Label
        label?.let {
            it()
            Spacer(modifier = Modifier.width(16.dp))
        }

        // Decrement Button
        Button(
            onClick = { if (diceCount > minDice) onDiceCountChange(diceCount - 1) },
            enabled = diceCount > minDice,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.white),
                contentColor = colorResource(id = R.color.black)
            )
        ) {
            Text("-", style = MaterialTheme.typography.titleLarge)
        }

        // Dice Count Display
        Text(
            text = diceCount.toString(),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Increment Button
        Button(
            onClick = { if (diceCount < maxDice) onDiceCountChange(diceCount + 1) },
            enabled = diceCount < maxDice,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.white),
                contentColor = colorResource(id = R.color.black)
            )
        ) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun DiceCard(isRolling: Boolean, diceImages: List<Int>, viewModel: GameViewModel) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DiceDisplay(
                diceImages = diceImages,
                isRolling = isRolling,
                heldDice = emptySet(),
                isMyTurn = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.rollDice() },
                enabled = !isRolling,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.primary),
                    contentColor = colorResource(id = R.color.on_primary)
                )
            ) {
                Text("Roll Dice")
            }
        }
    }
}

@Composable
private fun PlayerManagementCard(
    customState: GameScoreState.CustomScoreState,
    newPlayerName: String,
    onNewPlayerNameChange: (String) -> Unit,
    selectedPlayerIndex: Int,
    onSelectedPlayerChange: (Int) -> Unit,
    onAddPlayer: () -> Unit,
    onUpdatePlayer: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.surface_variant),
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Players", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = onAddPlayer,
                    enabled = customState.playerCount < MyGameManager.MAX_PLAYERS,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.primary),
                        contentColor = colorResource(id = R.color.on_primary)
                    )
                ) {
                    Text("Add Player")
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(customState.playerNames.toList()) { (index, name) ->
                    FilterChip(
                        selected = selectedPlayerIndex == index,
                        onClick = { onSelectedPlayerChange(index) },
                        label = { Text(name) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = colorResource(id = R.color.surface_variant),
                            labelColor = colorResource(id = R.color.on_surface_variant),
                            selectedContainerColor = colorResource(id = R.color.primary),
                            selectedLabelColor = colorResource(id = R.color.on_primary)
                        )
                    )
                }
            }

            // Selected Player Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newPlayerName,
                    onValueChange = onNewPlayerNameChange,
                    label = { Text("Rename Player") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = onUpdatePlayer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.primary),
                        contentColor = colorResource(id = R.color.on_primary)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update Name")
                }
            }
        }
    }
}

@Composable
private fun ScoreManagementCard(
    scoreInput: String,
    onScoreInputChange: (String) -> Unit,
    noteInput: String,
    onNoteInputChange: (String) -> Unit,
    onAddScore: () -> Unit,
    onAddNote: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.surface_variant),
            contentColor = colorResource(id = R.color.on_surface_variant)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Score Management", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = scoreInput,
                    onValueChange = onScoreInputChange,
                    label = { Text("Score") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onAddScore,
                    enabled = scoreInput.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.primary),
                        contentColor = colorResource(id = R.color.on_primary)
                    )
                ) {
                    Text("Add")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = onNoteInputChange,
                    label = { Text("Note") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onAddNote,
                    enabled = noteInput.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.primary),
                        contentColor = colorResource(id = R.color.on_primary)
                    ),
                ) {
                    Text("Add")
                }
            }
        }
    }
}

@Composable
private fun ScoreHistoryCard(
    customState: GameScoreState.CustomScoreState,
    selectedPlayerIndex: Int
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.surface_variant),
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Score History", style = MaterialTheme.typography.titleMedium)
            customState.scoreHistory[selectedPlayerIndex]?.forEach { note ->
                Text(note, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ActionButtonsCard(onResetScores: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onResetScores,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.primary),
                contentColor = colorResource(id = R.color.on_primary)
            )
        ) {
            Text("Reset All")
        }
    }
}

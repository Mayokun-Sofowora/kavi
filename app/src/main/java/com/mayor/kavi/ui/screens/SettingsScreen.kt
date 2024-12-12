package com.mayor.kavi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.*
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mayor.kavi.ui.viewmodel.*
import com.mayor.kavi.R
import com.mayor.kavi.data.SettingsDataStoreManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DiceViewModel = hiltViewModel(),
    settingsManager: SettingsDataStoreManager,
    navController: NavController
) {
    val shakeEnabled by viewModel.shakeEnabled.observeAsState(initial = false)
    val soundEnabled by settingsManager.getSoundEnabled()
        .collectAsState(initial = true)
    val selectedDiceType by settingsManager.getDiceType()
        .collectAsState(initial = "default")
    var selectedBoard by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Back"
                        )
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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Choose your dice board",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            // Board Selection Cards
            val boards = listOf(
                Triple(GameBoard.PIG.modeName, "Suitable for single player", R.drawable.dice_1),
                Triple(GameBoard.GREED.modeName, "Suitable for multi-player", R.drawable.dice_2),
                Triple(GameBoard.MEXICO.modeName, "Suitable for multi-player", R.drawable.dice_3),
                Triple(GameBoard.CHICAGO.modeName, "Suitable for multi-player", R.drawable.dice_4),
                Triple(GameBoard.BALUT.modeName, "Suitable for multi-player", R.drawable.dice_5)
            )

            boards.forEach { (boardName, description, image) ->
                BoardSelectionCard(
                    title = boardName,
                    description = description,
                    imageRes = image,
                    isSelected = selectedBoard == boardName,
                    backgroundColor = when (boardName) {
                        GameBoard.PIG.modeName -> Color(0xFFF9DCC4)
                        GameBoard.GREED.modeName -> Color(0xFFFEC89A)
                        GameBoard.MEXICO.modeName -> Color(0xFFFCD5CE)
                        GameBoard.CHICAGO.modeName -> Color(0xFFFFB5A7)
                        else -> Color(0xFFFFB5A7)
                    },
                    onSelect = {
                        selectedBoard = boardName
                        viewModel.setSelectedBoard(boardName)
                    }
                )
            }

            // Settings Cards
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.surface_variant)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Shake Detection Setting
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable Shake to Roll",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = shakeEnabled,
                            onCheckedChange = { viewModel.setShakeEnabled(it) }
                        )
                    }

                    // Sound Setting
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enable Sound",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = {
                                viewModel.viewModelScope.launch {
                                    settingsManager.setSoundEnabled(it)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Play Button
            Button(
                onClick = {
                    selectedBoard?.let { board ->
                        when (board) {
                            GameBoard.PIG.modeName -> navController.navigate("BoardOne")
                            GameBoard.GREED.modeName -> navController.navigate("BoardTwo")
                            GameBoard.MEXICO.modeName -> navController.navigate("BoardThree")
                            GameBoard.CHICAGO.modeName -> navController.navigate("BoardFour")
                            GameBoard.BALUT.modeName -> navController.navigate("BoardFive")
                        }
                    }
                },
                enabled = selectedBoard != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.primary),
                    contentColor = colorResource(id = R.color.on_primary)
                ),
                shape = RoundedCornerShape(32.dp)
            ) {
                Text("Play")
            }
        }
    }
}

@Composable
private fun BoardSelectionCard(
    title: String,
    description: String,
    imageRes: Int,
    isSelected: Boolean,
    backgroundColor: Color,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(120.dp)
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = colorResource(id = R.color.primary),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
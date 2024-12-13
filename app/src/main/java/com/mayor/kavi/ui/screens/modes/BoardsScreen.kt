package com.mayor.kavi.ui.screens.modes

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mayor.kavi.ui.viewmodel.*
import com.mayor.kavi.R
import com.mayor.kavi.data.games.GameBoard
import com.mayor.kavi.ui.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardsScreen(
    viewModel: DiceViewModel,
    navController: NavHostController
) {
    var selectedGameMode by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Choose Your Board",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Back",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colorResource(id = R.color.primary_container),
                    titleContentColor = colorResource(id = R.color.on_primary_container)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Board Selection Cards
            val boards = listOf(
                Triple(GameBoard.PIG.modeName, "Single player dice game", R.drawable.pig),
                Triple(GameBoard.GREED.modeName, "Race to 10,000 points", R.drawable.greed),
                Triple(GameBoard.MEXICO.modeName, "Lowest score wins", R.drawable.mexico),
                Triple(GameBoard.CHICAGO.modeName, "11 rounds of strategy", R.drawable.chicago),
                Triple(GameBoard.BALUT.modeName, "Yahtzee-style scoring", R.drawable.balut)
            )

            boards.forEach { (boardName, description, image) ->
                BoardSelectionCard(
                    title = boardName,
                    description = description,
                    imageRes = image,
                    isSelected = selectedGameMode == boardName,
                    backgroundColor = when (boardName) {
                        GameBoard.PIG.modeName -> Color(0xFFFCD5CE)
                        GameBoard.GREED.modeName -> Color(0xFFFEC89A)
                        GameBoard.MEXICO.modeName -> Color(0xFFE5E5E5)
                        GameBoard.CHICAGO.modeName -> Color(0xFFF9DCC4)
                        GameBoard.BALUT.modeName -> Color(0xFFFFB5A7)
                        else -> Color(0xFFFFB5A7)
                    },
                    onSelect = {
                        selectedGameMode = boardName
                        viewModel.setSelectedBoard(boardName)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Play Button
            Button(
                onClick = {
                    selectedGameMode?.let {
                        viewModel.setSelectedBoard(it)
                        navController.navigate(Routes.PlayMode.route)
                    }
                },
                enabled = selectedGameMode != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.primary),
                    contentColor = colorResource(id = R.color.on_primary)
                )
            ) {
                Text(
                    text = "Play",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
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
        shape = MaterialTheme.shapes.medium
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
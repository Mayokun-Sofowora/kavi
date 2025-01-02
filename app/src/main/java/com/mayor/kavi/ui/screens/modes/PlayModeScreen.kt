package com.mayor.kavi.ui.screens.modes

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.mayor.kavi.R
import com.mayor.kavi.data.models.*
import com.mayor.kavi.ui.Screen
import com.mayor.kavi.ui.viewmodel.*
import com.mayor.kavi.util.*
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayModeScreen(
    navController: NavController,
    appViewModel: AppViewModel,
    gameViewModel: GameViewModel
) {
    val userProfileState by appViewModel.userProfileState.collectAsState()
    val selectedBoard by gameViewModel.selectedBoard.collectAsState()

    LaunchedEffect(selectedBoard) {
        if (selectedBoard.isEmpty()) {
            gameViewModel.setSelectedBoard(GameBoard.GREED.modeName)
        }
    }

    LaunchedEffect(userProfileState) {
        Timber.tag("PlayModeScreen").d("UserProfileState: $userProfileState")
        when (userProfileState) {
            is Result.Success -> Timber.tag("PlayModeScreen")
                .d("Profile data: ${(userProfileState as Result.Success<UserProfile>).data}")

            is Result.Error -> Timber.tag("PlayModeScreen")
                .d("Error: ${(userProfileState as Result.Error).exception}")

            is Result.Loading -> Timber.tag("PlayModeScreen").d("Loading...")
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Select Play Mode",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Background with overlay
            Image(
                painter = painterResource(id = R.drawable.background2),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(id = R.color.scrim).copy(alpha = 0.75f))
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Welcome Message
                when (userProfileState) {
                    is Result.Success -> {
                        val profile = (userProfileState as Result.Success).data
                        Text(
                            text = "Welcome, ${profile.name}!",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    offset = Offset(2f, 2f),
                                    blurRadius = 3f
                                )
                            ),
                            textAlign = TextAlign.Center
                        )
                    }

                    is Result.Loading -> {
                        // Show loading state with preserved data if available
                        val preservedProfile = (userProfileState as Result.Loading).data
                        Text(
                            text = "Welcome, ${preservedProfile?.name}!",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    offset = Offset(2f, 2f),
                                    blurRadius = 3f
                                )
                            ),
                            textAlign = TextAlign.Center
                        )
                    }

                    is Result.Error -> {
                        Text(
                            text = "Welcome, Guest!",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    offset = Offset(2f, 2f),
                                    blurRadius = 3f
                                )
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Select your opponent:",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center
                )

                // Game Mode Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ElevatedButton(
                        onClick = {
                            gameViewModel.setPlayMode(PlayMode.Multiplayer)
                            Timber.tag("PlayModeScreen").d("Multiplayer button clicked")
                            navigateToBoard(gameViewModel, navController)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = colorResource(id = R.color.primary_container),
                            contentColor = colorResource(id = R.color.on_primary_container)
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(
                            defaultElevation = 6.dp
                        )
                    ) {
                        Text(
                            "Play Multiplayer",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    ElevatedButton(
                        onClick = {
                            gameViewModel.setPlayMode(PlayMode.SinglePlayer)
                            Timber.tag("PlayModeScreen").d("SinglePlayer button clicked")
                            navigateToBoard(gameViewModel, navController)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = colorResource(id = R.color.tertiary_container),
                            contentColor = colorResource(id = R.color.on_tertiary_container)
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(
                            defaultElevation = 6.dp
                        )
                    ) {
                        Text(
                            "Play Single Player",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun navigateToBoard(gameViewModel: GameViewModel, navController: NavController) {
    if (gameViewModel.playMode.value is PlayMode.Multiplayer) {
        Timber.tag("Navigation").d("Navigating to Multiplayer Lobby")
        navController.navigateToLobby()  // Using extension function
    } else if (gameViewModel.playMode.value is PlayMode.SinglePlayer) {
        Timber.tag("Navigation").d("Navigating to Single Player Board")
        navController.navigateToBoard(Screen.Board.Two) // Using extension function
    } else {
        Timber.tag("Navigation").d("Unknown GameMode")
    }
}

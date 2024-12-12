package com.mayor.kavi.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.mayor.kavi.R
import com.mayor.kavi.data.UserProfile
import com.mayor.kavi.util.Result
import com.mayor.kavi.ui.viewmodel.*
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayModeScreen(
    navController: NavController,
    appViewModel: AppViewModel,
    diceViewModel: DiceViewModel
) {
    val userProfileState by appViewModel.userProfileState.collectAsState()
    val selectedBoard by diceViewModel.selectedBoard.observeAsState(initial = "")

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
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    is Result.Error -> {
                        Text(
                            text = "Welcome, Guest!",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
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
                            appViewModel.setPlayMode("vs_player")
                            navigateToBoard(selectedBoard, navController)
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
                            "Play Vs Player",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    ElevatedButton(
                        onClick = {
                            appViewModel.setPlayMode("vs_ai")
                            navigateToBoard(selectedBoard, navController)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = colorResource(id = R.color.secondary_container),
                            contentColor = colorResource(id = R.color.on_secondary_container)
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(
                            defaultElevation = 6.dp
                        )
                    ) {
                        Text(
                            "Play Vs AI",
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

private fun navigateToBoard(selectedBoard: String, navController: NavController) {
    when (selectedBoard) {
        "Pig" -> navController.navigate("boardOne")
        "Greed / 10000" -> navController.navigate("boardTwo")
        "Mexico" -> navController.navigate("boardThree")
        "Chicago" -> navController.navigate("boardFour")
        "Balut" -> navController.navigate("boardFive")
        else -> navController.popBackStack()
    }
}

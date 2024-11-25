package com.mayor.kavi.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.mayor.kavi.R
import com.mayor.kavi.data.models.GameType
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchmakingScreen(navController: NavController) {
    var isSearching by remember { mutableStateOf(false) }
    var playerFound by remember { mutableStateOf(false) }
    val matchType = GameType.LOCAL_MULTIPLAYER

    // Cancel the search
    fun cancelSearch() {
        isSearching = false
        playerFound = false
    }

    // Matchmaking logic based on game type
    if (isSearching) {
        LaunchedEffect(isSearching) {
            when (matchType) {
                GameType.COMPUTER_AI -> {
                    navController.navigate("gameScene")
                }

                GameType.LOCAL_MULTIPLAYER -> {
                    // Simulate local search (nearby players, Bluetooth, etc.)
                    delay(3000)
                    playerFound = true // Simulate match found
                    delay(2000)
                    navController.navigate("gameScene")
                }
//                GameType.ONLINE_MULTIPLAYER -> {
//                    // Implement online matchmaking in future improvements
//                }
                else -> {
                    playerFound = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxHeight()) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("classicMode") }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Display Image Above "Find A Match"
                    Image(
                        painter = painterResource(id = R.drawable.hero),
                        contentDescription = "Matchmaking Illustration",
                        modifier = Modifier
                            .size(200.dp)
                            .padding(bottom = 16.dp)
                    )
                    // Show matchmaking card
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        border = CardDefaults.outlinedCardBorder(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                        modifier = Modifier
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .padding(horizontal = 30.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(30.dp)
                        ) {
                            Text(
                                text = "Find A Match",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            if (!isSearching) {
                                // Show "Search" Button
                                Button(
                                    onClick = { isSearching = true },
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(horizontal = 40.dp)
                                ) {
                                    Text("Search", style = MaterialTheme.typography.bodyLarge)
                                }
                            } else {
                                if (!playerFound) {
                                    CircularProgressIndicator(strokeWidth = 5.dp)
                                    Spacer(modifier = Modifier.height(30.dp))
                                    Text(
                                        text = "Searching for a match...",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                    Spacer(modifier = Modifier.height(40.dp))

                                    Button(
                                        onClick = { cancelSearch() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .padding(horizontal = 30.dp, vertical = 12.dp)
                                    ) {
                                        Text("Cancel", style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                            // If match is found
                            if (playerFound) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Match Found",
                                    modifier = Modifier.size(50.dp),
                                    tint = Color.Green
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "Match Found!",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Green
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Preparing game...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Compete and enjoy your gaming experience!",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MatchmakingScreenPreview() {
    val navController = rememberNavController()
    MatchmakingScreen(navController = navController)
}

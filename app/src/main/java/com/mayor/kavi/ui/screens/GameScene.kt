package com.mayor.kavi.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.mayor.kavi.ui.viewmodel.UserViewModel
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScene(navController: NavController, viewModel: UserViewModel) {
//    val
    // Collect the userName as state
    val userName by viewModel.userName.collectAsState("guest")

    // States for dice values and lock statuses
    val diceValues = remember { mutableStateListOf(1, 1, 1, 1, 1) }
    val lockedDice = remember { mutableStateListOf(false, false, false, false, false) }


    // Function to roll the dice
    fun rollDice() {
        for (i in diceValues.indices) {
            if (!lockedDice[i]) { // Only roll unlocked dice
                diceValues[i] = Random.nextInt(1, 7) // Random value between 1 and 6
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("Settings") }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Score Area
                ScoreArea()

                // Dice Play Area
                DicePlayArea(diceValues, lockedDice, ::rollDice, navController)
            }
        }
    )
}

@Composable
fun ScoreArea() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Score Area: Add educational scoring rules here.",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
    }
}

@Composable
fun DicePlayArea(
    diceValues: List<Int>,
    lockedDice: MutableList<Boolean>,
    rollDice: () -> Unit,  // Pass function reference
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Display five dice with interactivity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            diceValues.forEachIndexed { index, value ->
                Dice(
                    value = value,
                    isLocked = lockedDice[index],
                    onToggleLock = { lockedDice[index] = !lockedDice[index] }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Roll Button
        Button(onClick = { rollDice() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Roll Dice", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // End Game Button
        Button(
            onClick = {
                navController.popBackStack("classicMode", false)
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("End Game", fontSize = 18.sp, color = Color.White)
        }
    }
}

@Composable
fun Dice(value: Int, isLocked: Boolean, onToggleLock: () -> Unit) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .background(if (isLocked) Color.Gray else Color.White, shape = CircleShape)
            .border(2.dp, Color.Black, shape = CircleShape)
            .clickable { onToggleLock() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (isLocked) Color.White else Color.Black
        )
    }
}

//@Preview(showBackground = true)
//@Composable
//fun GameScenePreview() {
//    val navController = rememberNavController()
//    GameScene(navController = navController)
//}

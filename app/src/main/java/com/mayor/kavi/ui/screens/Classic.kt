package com.mayor.kavi.ui.screens

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.mayor.kavi.R
import com.mayor.kavi.data.dao.UserEntity
import com.mayor.kavi.data.models.GameType
import com.mayor.kavi.ui.theme.AdlamFont
import com.mayor.kavi.ui.viewmodel.UserViewModel
import kotlin.random.Random
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassicMode(navController: NavController, viewModel: UserViewModel) {
//    val
    var userName by remember { mutableStateOf(TextFieldValue(viewModel.userName.value)) }
    val selectedType by viewModel.selectedMode.collectAsState()

    // Getting the current context so we can use it later
    val context = LocalContext.current

    // Function to generate a random player name if not entered
    fun getPlayerName(): String {
        return if (userName.text.isEmpty()) {
            "guest ${Random.nextInt(1000)}"  // Generate random name
        } else {
            userName.text
        }
    }

    // Ensure matchmaking only for valid modes
    fun onSelectType(type: GameType) {
        viewModel.setGameType(type)
        val name = getPlayerName()
        viewModel.setUserName(name)  // Update name in ViewModel
        // Save user to database (make sure it's stored before navigation)
        val userEntity = UserEntity(
            username = name,
            preferences = "{}"
        )
        viewModel.saveUser(userEntity)
        // Navigate to the game scene
        when (type) {
            GameType.LOCAL_MULTIPLAYER -> navController.navigate("gameScene")
            GameType.COMPUTER_AI -> navController.navigate("gameScene")
        }
    }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = {
                Box(modifier = Modifier.fillMaxHeight()) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .height(100.dp)
                            .align(Alignment.CenterStart)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    navController.popBackStack("play", false) // Navigate back to the play screen
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back"
                    )
                }
            }
        )
    }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Your background image and overlay
            Image(
                painter = painterResource(id = R.drawable.background2),
                contentDescription = "Background Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.75f }
                    .background(Color.Black.copy(alpha = 0.75f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Select how you'd like to play",
                    fontSize = 14.sp,
                    color = Color.White,
                    fontFamily = AdlamFont,
                    modifier = Modifier
                        .padding(bottom = 64.dp)
                        .background(Color(0xFF73423C).copy(alpha = 0.87f))
                        .padding(8.dp)
                )

                Text(
                    "Enter your name (optional):",
                    fontFamily = AdlamFont,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Text field and icon in a row for alignment
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = userName,
                        onValueChange = {
                            userName = it
                            viewModel.setUserName(it.text) // Update ViewModel with new name
                        },
                        modifier = Modifier
                            .width(300.dp)
                            .background(Color.White, shape = MaterialTheme.shapes.medium)
                            .padding(12.dp)
                            .onKeyEvent { event ->
                                if (event.key == Key.Enter) {
                                    // Save the user when Enter is pressed
                                    val userEntity = UserEntity(username = userName.text, id = 0, preferences = "")
                                    viewModel.saveUser(userEntity)
                                    true
                                } else {
                                    false
                                }
                            },
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = AdlamFont,
                            color = Color.Black
                        )
                    )

                    IconButton(
                        onClick = {
                            // Save the user name only without navigation
                            if (userName.text.isNotBlank()) {
                                val userEntity = UserEntity(username = userName.text, id = 0, preferences = "")
                                viewModel.saveUser(userEntity)
                                Toast.makeText(context, "Username saved", Toast.LENGTH_SHORT).show()  // Show toast here
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Enter"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onSelectType(GameType.LOCAL_MULTIPLAYER) },
                    modifier = Modifier.width(170.dp),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text("Play Vs Player", color = Color.White, fontFamily = AdlamFont)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onSelectType(GameType.COMPUTER_AI) },
                    modifier = Modifier.width(170.dp),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text("Play Vs AI", color = Color.White, fontFamily = AdlamFont)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // For the "Play Online" button:
                Button(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Online Multiplayer not implemented yet",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.width(170.dp),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text("Play Online", color = Color.White, fontFamily = AdlamFont)
                }
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun ClassicModePreview() {
//    ClassicMode(navController = rememberNavController())
//}
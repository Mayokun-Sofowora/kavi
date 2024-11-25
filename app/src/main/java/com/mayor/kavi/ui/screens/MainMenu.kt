package com.mayor.kavi.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mayor.kavi.R
import com.mayor.kavi.ui.theme.*
import com.mayor.kavi.ui.viewmodel.UserViewModel

@Composable
fun MainMenu(navController: NavController, userViewModel: UserViewModel) {
//    val
    val playerId = userViewModel.userName.collectAsState(initial = null).value

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.background1),
            contentDescription = "Background Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Apply blur effect using graphicsLayer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.8f }
                .background(Color.Black.copy(alpha = 0.9f))
        )

        // Overlay with logo and buttons
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp)
                .wrapContentSize(align = Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp)
            ) {
                // Dark semi-transparent background layer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                )
                // Logo at the top
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .padding(bottom = 30.dp)
                        .fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.Center,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            // Navigation Buttons
            MenuButton(label = "Start", onClick = { navController.navigate("play") })
            MenuButton(label = "Settings", onClick = { navController.navigate("settings") })
            MenuButton(
                label = "Instructions",
                onClick = { navController.navigate("instructions/full") })

            // Ensure the playerId is not null before navigating
            MenuButton(
                label = "Statistics",
                enabled = playerId != null,
                onClick = {
                    playerId?.let {
                        navController.navigate("statistics/$it")
                    }
                }
            )
        }
    }
}

@Composable
fun MenuButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick.takeIf { enabled } ?: {},
        modifier = Modifier.padding(vertical = 10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (enabled) Color(0xFFAF423C) else Color.Gray),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 40.dp, vertical = 15.dp),
        enabled = enabled
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = AdlamFont,
                color = Color.White,
            )
        )
    }
}

//@Preview(showBackground = true)
//@Composable
//fun MainMenuPreview() {
//    val navController = rememberNavController()
//    val userViewModel = hiltViewModel<UserViewModel>() // For Preview, can mock
//    MainMenu(navController, userViewModel)
//}

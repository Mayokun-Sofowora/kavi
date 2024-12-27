package com.mayor.kavi.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mayor.kavi.R
import com.mayor.kavi.data.repository.AuthRepository
import com.mayor.kavi.ui.Routes
import com.mayor.kavi.ui.theme.*

@Composable
fun MainMenuScreen(navController: NavController) {
    var authRepository by remember { mutableStateOf<AuthRepository?>(null) }

    BackHandler(enabled = true) {
        // Prevent navigation to sign-in page
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Layer
        BackgroundImage()

        // Content Layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoSection()

            Spacer(modifier = Modifier.height(32.dp))

            MenuButtonsSection(navController, authRepository)
        }
    }
}

@Composable
private fun BackgroundImage() {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))  // Slightly less dark overlay
        )
    }
}

@Composable
private fun LogoSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.03f))
            .padding(vertical = 24.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
fun MenuButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val buttonColor = Color(0xFFAF423C)  // Custom red color
    val disabledColor = Color.Gray

    Button(
        onClick = onClick.takeIf { enabled } ?: {},
        modifier = Modifier
            .widthIn(min = 200.dp)
            .height(70.dp)  // Slightly taller
            .padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) buttonColor else disabledColor,
            contentColor = Color.White
        ),
        shape = MaterialTheme.shapes.medium,
        enabled = enabled,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp,
            disabledElevation = 0.dp
        ),
        border = BorderStroke(
            width = 2.dp,
            color = if (enabled)
                Color.White.copy(alpha = 0.2f)
            else
                Color.White.copy(alpha = 0.1f)
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = AdlamFont,
                color = Color.White
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun MenuButtonsSection(
    navController: NavController,
    authRepository: AuthRepository?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)  // Reduced spacing
    ) {
        MenuButton(
            label = "Start",
            onClick = { navController.navigate(Routes.Start.route) }
        )
        MenuButton(
            label = "Settings",
            onClick = { navController.navigate(Routes.Settings.route) }
        )
        MenuButton(
            label = "Instructions",
            onClick = { navController.navigate(Routes.Instructions.route) }
        )
        MenuButton(
            label = "Statistics",
            onClick = { navController.navigate(Routes.Statistics.route) }
        )

        Spacer(modifier = Modifier.height(16.dp))  // Increased spacing before logout

        MenuButton(
            label = "Logout",
            onClick = {
                authRepository?.signOut()
                navController.navigate(Routes.SignOut.route) {
                    popUpTo(Routes.MainMenu.route) { inclusive = true }
                }
            }
        )
    }
}
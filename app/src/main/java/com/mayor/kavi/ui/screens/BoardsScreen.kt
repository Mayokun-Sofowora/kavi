package com.mayor.kavi.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mayor.kavi.ui.viewmodel.DiceViewModel
import com.mayor.kavi.R
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Kavi Logo",
                modifier = Modifier
                    .size(120.dp)
                    .padding(vertical = 16.dp)
            )

            // Game Mode Selection List
            val gameModes = listOf(
                "Pig" to R.drawable.pig,
                "Greed / 10000" to R.drawable.greed,
                "Mexico" to R.drawable.mexico,
                "Chicago" to R.drawable.chicago,
                "Balut" to R.drawable.balut
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                gameModes.forEach { (gameMode, iconRes) ->
                    GameModeItem(
                        text = gameMode,
                        imageResourceId = iconRes,
                        isSelected = selectedGameMode == gameMode,
                        onClick = { selectedGameMode = gameMode }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Play Button
            ElevatedButton(
                onClick = {
                    selectedGameMode?.let {
                        viewModel.setSelectedBoard(it)
                        navController.navigate(Routes.PlayMode.route)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = selectedGameMode != null,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = colorResource(id = R.color.primary),
                    contentColor = colorResource(id = R.color.on_primary),
                    disabledContainerColor = colorResource(id = R.color.outline),
                    disabledContentColor = colorResource(id = R.color.on_surface_variant)
                ),
                elevation = ButtonDefaults.elevatedButtonElevation(
                    defaultElevation = 6.dp
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
fun GameModeItem(
    text: String,
    imageResourceId: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
            .background(
                if (isSelected) colorResource(id = R.color.primary).copy(alpha = 0.3f)
                else Color.Transparent,
                shape = MaterialTheme.shapes.medium
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) colorResource(id = R.color.primary).copy(alpha = 0.6f)
                else colorResource(id = R.color.outline).copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) colorResource(id = R.color.primary).copy(alpha = 0.8f)
            else colorResource(id = R.color.on_background).copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.weight(1f))

        Image(
            painter = painterResource(id = imageResourceId),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )

        if (isSelected) {
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = colorResource(id = R.color.primary),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

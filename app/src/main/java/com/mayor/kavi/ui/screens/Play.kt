// file: PlayScreen.kt
package com.mayor.kavi.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.mayor.kavi.R
import com.mayor.kavi.ui.theme.AdlamFont
import com.mayor.kavi.data.models.GameMode
import com.mayor.kavi.data.repository.fakes.FakeUserRepository
import com.mayor.kavi.ui.viewmodel.PlayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(navController: NavController, viewModel: PlayViewModel) {
    val selectedMode = viewModel.selectedMode.collectAsState().value

    val onModeSelected: (GameMode) -> Unit = { mode ->
        viewModel.selectMode(mode.name)
        when (mode) {
            GameMode.VIRTUAL -> navController.navigate("arMode")
            GameMode.CLASSIC -> navController.navigate("classicMode")
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
                actions = {
                    IconButton(onClick = { navController.navigate("instructions/singlePage") }) {
                        Icon(Icons.Default.Info, contentDescription = "Info")
                    }
                }
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(80.dp))

                Text(
                    "GAME MODE",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AdlamFont
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(80.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ModeSelectionButton(
                        label = "VIRTUAL",
                        imageRes = R.drawable.virtual,
                        onClick = { onModeSelected(GameMode.VIRTUAL) }
                    )
                    Spacer(modifier = Modifier.width(100.dp))
                    ModeSelectionButton(
                        label = "CLASSIC",
                        imageRes = R.drawable.classic,
                        onClick = { onModeSelected(GameMode.CLASSIC) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    "Choose between AR mode for virtual gameplay or classic mode to play the traditional way.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

@Composable
fun ModeSelectionButton(label: String, imageRes: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "$label Mode",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PlayScreenPreview() {
    PlayScreen(
        navController = rememberNavController(),
        viewModel = PlayViewModel(
            userRepository = FakeUserRepository()  // Replace with actual repository if needed
        )
    )
}

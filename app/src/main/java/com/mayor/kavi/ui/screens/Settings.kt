package com.mayor.kavi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.mayor.kavi.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, settingsViewModel: SettingsViewModel) {
    // Collect settings from the ViewModel
    val soundEnabled by settingsViewModel.isSoundEnabled.collectAsState()
    val notificationsEnabled by settingsViewModel.isNotificationsEnabled.collectAsState()
    val theme by settingsViewModel.selectedTheme.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFDBB5).copy(alpha = 0.7f),
                            Color(0xFF6C4E31).copy(alpha = 0.9f)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {

                // Sound toggle
                SettingsOption(
                    label = "Enable Sound",
                    isChecked = soundEnabled,
                    onCheckedChange = { settingsViewModel.toggleSound() })

                // Notifications toggle
                SettingsOption(
                    label = "Enable Notifications",
                    isChecked = notificationsEnabled,
                    onCheckedChange = { settingsViewModel.toggleNotifications() })

                Spacer(modifier = Modifier.height(16.dp))

                // Theme selection
                ThemeSelector(
                    selectedTheme = theme,
                    onThemeChanged = { settingsViewModel.changeTheme(it) })

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = { navController.popBackStack() }) {
                    Text("Back")
                }
            }
        }
    }
}

@Composable
fun SettingsOption(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = isChecked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ThemeSelector(selectedTheme: String, onThemeChanged: (String) -> Unit) {
    val themes = listOf("Light", "Dark", "System Default")
    Column {
        Text(
            "Select Theme:",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
        themes.forEach { theme ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = theme == selectedTheme,
                    onClick = { onThemeChanged(theme) }
                )
                Text(theme)
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun SettingsScreenPreview() {
//    // For preview, create a fake SettingsViewModel
//    val fakeViewModel = FakeSettingsViewModel() // Implement a fake or mock ViewModel
//    SettingsScreen(navController = rememberNavController(), settingsViewModel = fakeViewModel)
//}
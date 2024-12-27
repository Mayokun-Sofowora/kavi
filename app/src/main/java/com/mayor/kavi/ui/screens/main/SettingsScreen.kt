package com.mayor.kavi.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.*
import androidx.navigation.NavController
import com.mayor.kavi.ui.viewmodel.*
import com.mayor.kavi.R
import com.mayor.kavi.util.BoardColors
import com.mayor.kavi.data.manager.*
import com.mayor.kavi.ui.Routes
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GameViewModel = hiltViewModel(),
    navController: NavController
) {
    // Handle back button press
    BackHandler {
        navController.navigateUp()
    }

    val scope = rememberCoroutineScope()
    val settingsManager = SettingsManager.LocalSettingsManager.current
    val vibrationEnabled by settingsManager.getVibrationEnabled().collectAsState(initial = true)
    val shakeEnabled by settingsManager.getShakeEnabled().collectAsState(initial = false)
    val boardColor by settingsManager.getBoardColor().collectAsState(initial = "default")

    LaunchedEffect(vibrationEnabled) {
        viewModel.setVibrationEnabled(vibrationEnabled)
    }

    // Cleanup when leaving settings
    DisposableEffect(Unit) {
        viewModel.pauseShakeDetection()
        onDispose {
            viewModel.resumeShakeDetection()
            viewModel.setVibrationEnabled(vibrationEnabled)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(id = R.color.primary_container),
                    titleContentColor = colorResource(id = R.color.on_primary_container)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Game Controls Card
            SettingsCard(
                title = "Game Controls",
                content = {
                    // Shake Detection Setting
                    SettingsSwitch(
                        title = "Shake to Roll",
                        description = "Roll dice by shaking your device",
                        checked = shakeEnabled,
                        onCheckedChange = {
                            scope.launch {
                                settingsManager.setShakeEnabled(it)
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Vibration Setting
                    SettingsSwitch(
                        title = "Vibration Effects",
                        description = "Haptic feedback during gameplay",
                        checked = vibrationEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                settingsManager.setVibrationEnabled(enabled)
                            }
                        }
                    )
                }
            )

            // Game Rules Card
            SettingsCard(
                title = "Game Rules",
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Not sure what to play? check out the quick board rules",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Button(
                            onClick = { navController.navigate(Routes.InstructionsShort.route + "/4") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorResource(id = R.color.secondary)
                            )
                        ) {
                            Text("Quick Board Rules")
                        }
                    }
                }
            )

            // Appearance Card
            SettingsCard(
                title = "Appearance",
                content = {
                    Column {
                        Text(
                            "Background Color",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            val availableColors = BoardColors.getAvailableColors()
                            items(
                                count = availableColors.size,
                                key = { index -> availableColors[index] }
                            ) { index ->
                                val color = availableColors[index]
                                ColorOption(
                                    color = color,
                                    isSelected = color == boardColor,
                                    onSelect = {
                                        scope.launch {
                                            settingsManager.setBoardColor(color)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.surface_variant)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ColorOption(
    color: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(
                when (val backgroundColor = BoardColors.getColor(color)) {
                    is Color -> Modifier.background(color = backgroundColor)
                    is Brush -> Modifier.background(brush = backgroundColor)
                    else -> Modifier.background(color = BoardColors.getColor("default") as Color)
                }
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(onClick = onSelect),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
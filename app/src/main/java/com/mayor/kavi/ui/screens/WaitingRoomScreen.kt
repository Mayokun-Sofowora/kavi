package com.mayor.kavi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mayor.kavi.data.GameSession
import com.mayor.kavi.ui.viewmodel.MultiplayerViewModel
import com.mayor.kavi.util.ErrorContent
import com.mayor.kavi.util.Result

@Composable
fun WaitingRoomScreen(
    multiplayerViewModel: MultiplayerViewModel,
    onGameStart: () -> Unit,
    onCancel: () -> Unit
) {
    val gameSession by multiplayerViewModel.gameSession.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (gameSession) {
                is Result.Success -> {
                    val session = (gameSession as Result.Success<GameSession>).data
                    if (session.gameState["status"] == "active") {
                        LaunchedEffect(Unit) {
                            onGameStart()
                        }
                    } else {
                        WaitingContent(session)
                    }
                }

                is Result.Loading -> {
                    CircularProgressIndicator()
                }

                is Result.Error -> {
                    ErrorContent(
                        message = (gameSession as Result.Error).exception?.message
                            ?: "Unknown error",
                        onRetry = { /* Implement retry logic */ }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun WaitingContent(session: GameSession) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Waiting for opponent...",
            style = MaterialTheme.typography.headlineSmall
        )

        LinearProgressIndicator(
            modifier = Modifier.width(200.dp)
        )

        Text(
            "Game ID: ${session.id}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
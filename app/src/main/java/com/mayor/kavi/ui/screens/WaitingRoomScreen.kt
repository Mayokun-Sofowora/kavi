package com.mayor.kavi.ui.screens

import androidx.compose.runtime.*
import com.mayor.kavi.ui.viewmodel.MultiplayerViewModel
import com.mayor.kavi.util.Result

@Composable
fun WaitingRoomScreen(
    multiplayerViewModel: MultiplayerViewModel,
    onGameStart: () -> Unit
) {
    val gameSession by multiplayerViewModel.gameSession.collectAsState()

//    when (gameSession) {
//        is Result.Success -> {
//            if (gameSession.data.gameState["status"] == "active") {
//                onGameStart()
//            } else {
//                // Show waiting UI
//            }
//        }
//        // Handle other states
//    }
//    else -> //// Handle other states
}
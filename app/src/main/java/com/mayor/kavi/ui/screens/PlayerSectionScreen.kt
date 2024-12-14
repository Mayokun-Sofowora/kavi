package com.mayor.kavi.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import com.mayor.kavi.ui.viewmodel.MultiplayerViewModel

@Composable
fun PlayerSelectionScreen(
    multiplayerViewModel: MultiplayerViewModel,
    onPlayerSelected: (String) -> Unit
) {
    val opponents by multiplayerViewModel.opponents.collectAsState()

    LazyColumn {
//        items(opponents) { player ->
//            PlayerCard(
//                player = player,
//                onClick = { onPlayerSelected(player.uid) }
//            )
//        }
    }
}
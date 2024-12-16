//package com.mayor.kavi.ui.screens
//
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.*
//import androidx.compose.ui.unit.dp
//import com.mayor.kavi.data.UserProfile
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PlayerSelectionScreen(
//    onPlayerSelected: (String) -> Unit,
//    onBack: () -> Unit
//) {
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        TopAppBar(
//            title = { Text("Select Opponent") },
//            navigationIcon = {
//                IconButton(onClick = onBack) {
//                    Icon(Icons.Default.ArrowBack, "Back")
//                }
//            }
//        )
//
//        if (opponents.isEmpty()) {
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) {
//                CircularProgressIndicator()
//            }
//        } else {
//            LazyColumn(
//                modifier = Modifier.fillMaxSize(),
//                verticalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                items(opponents) { player ->
//                    PlayerCard(
//                        player = player,
//                        onClick = { onPlayerSelected(player.id) }
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun PlayerCard(
//    player: UserProfile,
//    onClick: () -> Unit
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable(onClick = onClick)
//            .padding(8.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//    ) {
//        Row(
//            modifier = Modifier
//                .padding(16.dp)
//                .fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Column {
//                Text(
//                    text = player.name,
//                    style = MaterialTheme.typography.titleMedium
//                )
//                Text(
//                    text = "Recent games: ${player.recentScores.size}",
//                    style = MaterialTheme.typography.bodyMedium
//                )
//            }
//            Icon(
//                imageVector = Icons.Default.ChevronRight,
//                contentDescription = "Select player"
//            )
//        }
//    }
//}

package com.mayor.kavi.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mayor.kavi.util.ConfettiAnimation

@Composable
fun GameEndDialog(
    message: String,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    if (message.contains("Win")) ConfettiAnimation(modifier = Modifier.fillMaxSize())
    AlertDialog(
        onDismissRequest = { },
        title = {
            if (message.contains("Win")) Text("Congratulations") else Text("Game Over")
        },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onPlayAgain) {
                Text("Play Again")
            }
        },
        dismissButton = {
            TextButton(onClick = onExit) {
                Text("Exit")
            }
        }
    )
}
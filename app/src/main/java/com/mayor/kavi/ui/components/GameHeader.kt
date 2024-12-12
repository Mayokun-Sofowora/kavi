package com.mayor.kavi.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mayor.kavi.R

@Composable
fun GameHeader(
    currentPlayer: String,
    player1Score: Int,
    player2Score: Int,
    player2Name: String = "AI",
    isAIGame: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.surface_variant)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player 1 (User)
            PlayerScore(
                name = "You",
                score = player1Score,
                isCurrentTurn = currentPlayer == "player1",
                alignment = Alignment.Start
            )

            // VS Indicator
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.primary)
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "VS",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorResource(id = R.color.on_primary),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // Player 2 (AI or Opponent)
            PlayerScore(
                name = player2Name,
                score = player2Score,
                isCurrentTurn = currentPlayer == "player2",
                alignment = Alignment.End
            )
        }
    }
}

@Composable
private fun PlayerScore(
    name: String,
    score: Int,
    isCurrentTurn: Boolean,
    alignment: Alignment.Horizontal
) {
    Column(
        horizontalAlignment = when(alignment) {
            Alignment.Start -> Alignment.Start
            else -> Alignment.End
        },
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = if (isCurrentTurn)
                colorResource(id = R.color.primary)
            else
                colorResource(id = R.color.on_surface_variant)
        )
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = colorResource(id = R.color.on_surface_variant)
        )
        if (isCurrentTurn) {
            LinearProgressIndicator(
                modifier = Modifier
                    .width(60.dp)
                    .padding(top = 4.dp),
                color = colorResource(id = R.color.primary)
            )
        }
    }
}
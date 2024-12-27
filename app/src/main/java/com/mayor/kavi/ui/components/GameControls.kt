package com.mayor.kavi.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.*
import com.mayor.kavi.R

/**
 * Composable to display the game controls on the board screen.
 */
@Composable
fun GameControls(
    onRoll: () -> Unit,
    onBank: (() -> Unit)? = null,
    onSelectedCategory: (() -> Unit)? = null,
    onEndTurn: (() -> Unit)? = null,
    isRolling: Boolean = false,
    canReroll: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onRoll,
            enabled = !isRolling && canReroll,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.primary),
                contentColor = colorResource(id = R.color.on_primary)
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text("Roll")
        }

        if (onBank != null) {
            Button(
                onClick = onBank,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.primary),
                    contentColor = colorResource(id = R.color.on_primary)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Bank")
            }
        }

        if (onEndTurn != null) {
            Button(
                onClick = onEndTurn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.primary),
                    contentColor = colorResource(id = R.color.on_primary)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("End Turn")
            }
        }

        if (onSelectedCategory != null) {
            Button(
                onClick = onSelectedCategory,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.primary),
                    contentColor = colorResource(id = R.color.on_primary)
                )
            ) {
                Text("Score")
            }
        }
    }
}

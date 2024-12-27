package com.mayor.kavi.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mayor.kavi.R

/**
 * Composable to display the dice on the board screen.
 */
@Composable
fun DiceDisplay(
    diceImages: List<Int>,
    isRolling: Boolean,
    heldDice: Set<Int>,
    isMyTurn: Boolean = true,
    onDiceHold: ((Int) -> Unit)? = null,
    diceSize: Dp = 100.dp,
    arrangement: DiceArrangement = DiceArrangement.GRID
) {
    when (arrangement) {
        DiceArrangement.GRID -> DiceGrid(
            diceImages = diceImages,
            isRolling = isRolling,
            heldDice = heldDice,
            isMyTurn = isMyTurn,
            onDiceHold = onDiceHold,
            diceSize = diceSize
        )
        DiceArrangement.ROW -> DiceRow(
            diceImages = diceImages,
            isRolling = isRolling,
            heldDice = heldDice,
            isMyTurn = isMyTurn,
            onDiceHold = onDiceHold,
            diceSize = diceSize
        )
    }
}

enum class DiceArrangement {
    GRID, ROW
}

@Composable
private fun DiceGrid(
    diceImages: List<Int>,
    isRolling: Boolean,
    heldDice: Set<Int>,
    isMyTurn: Boolean,
    onDiceHold: ((Int) -> Unit)?,
    diceSize: Dp
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // First row (3 dice)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            diceImages.take(3).forEachIndexed { index, diceImage ->
                DiceItem(
                    isRolling = isRolling,
                    isHeld = heldDice.contains(index),
                    diceImage = diceImage,
                    isMyTurn = isMyTurn,
                    onDiceClick = { onDiceHold?.invoke(index) },
                    size = diceSize
                )
            }
        }

        // Second row (3 dice)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            diceImages.drop(3).take(3).forEachIndexed { index, diceImage ->
                DiceItem(
                    isRolling = isRolling,
                    isHeld = heldDice.contains(index + 3),
                    diceImage = diceImage,
                    isMyTurn = isMyTurn,
                    onDiceClick = { onDiceHold?.invoke(index + 3) },
                    size = diceSize
                )
            }
        }
    }
}

@Composable
private fun DiceRow(
    diceImages: List<Int>,
    isRolling: Boolean,
    heldDice: Set<Int>,
    isMyTurn: Boolean,
    onDiceHold: ((Int) -> Unit)?,
    diceSize: Dp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        diceImages.forEachIndexed { index, diceImage ->
            DiceItem(
                isRolling = isRolling,
                isHeld = heldDice.contains(index),
                diceImage = diceImage,
                isMyTurn = isMyTurn,
                onDiceClick = { onDiceHold?.invoke(index) },
                size = diceSize
            )
        }
    }
}

@Composable
fun DiceItem(
    isRolling: Boolean,
    isHeld: Boolean,
    diceImage: Int,
    isMyTurn: Boolean,
    onDiceClick: () -> Unit,
    size: Dp = 100.dp
) {
    Box(
        modifier = Modifier
            .clickable(
                enabled = !isRolling && isMyTurn,
                onClick = onDiceClick
            )
            .background(
                color = if (isHeld)
                    colorResource(id = R.color.scrim).copy(alpha = 0.2f)
                else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(4.dp)
    ) {
        DiceRollAnimation(
            isRolling = isRolling && !isHeld,
            diceImage = diceImage,
            modifier = Modifier.size(size)
        )
    }
}
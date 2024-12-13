package com.mayor.kavi.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun DiceRollAnimation(
    isRolling: Boolean,
    diceImage: Int,
    modifier: Modifier = Modifier
) {
    var currentRotation by remember { mutableFloatStateOf(0f) }
    val rotation = remember { Animatable(currentRotation) }

    // Keep track of the image we should display
    var displayedImage by remember(diceImage) { mutableIntStateOf(diceImage) }
    val numberOfSpins = 3

    LaunchedEffect(isRolling) {
        if (isRolling) {
            // Keep showing current image during roll
            displayedImage = diceImage

            // Reset rotation
            currentRotation = 0f
            rotation.snapTo(0f)

            // Animate rolling
            val totalRotation = numberOfSpins * 360f + 360f // 4 full spins + one extra for reset
            rotation.animateTo(
                targetValue = totalRotation,
                animationSpec = tween(
                    durationMillis = 2000,
                    easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)  // Custom easing curve
                )
            ) {
                currentRotation = value
            }

            // Ensure final position is at 0 degrees
            rotation.snapTo(0f)

            displayedImage = diceImage
        }
    }

    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = displayedImage),
            contentDescription = "Rolling Dice",
            modifier = Modifier
                .size(150.dp)
                .rotate(rotation.value % 360f)
                .graphicsLayer {
                    // Add slight scaling during rotation for more dynamic feel
                    val scale = 1f - (rotation.value % 360f / 360f) * 0.1f
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}

package com.mayor.kavi.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mayor.kavi.R
import kotlinx.coroutines.delay

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
    val numberOfSpins = 4

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
                    easing = FastOutSlowInEasing
                )
            )

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
        )
    }
}

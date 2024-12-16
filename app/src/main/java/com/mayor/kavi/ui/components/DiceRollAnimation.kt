package com.mayor.kavi.ui.components

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

@Composable
fun DiceRollAnimation(
    isRolling: Boolean,
    diceImage: Int,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dice animation")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "dice rotation"
    )

    Image(
        painter = painterResource(id = diceImage),
        contentDescription = "Dice Image",
        modifier = modifier
            .rotate(if (isRolling) rotation else 0f),
        contentScale = ContentScale.Fit
    )
}
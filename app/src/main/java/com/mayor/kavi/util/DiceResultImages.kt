package com.mayor.kavi.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mayor.kavi.R
import com.mayor.kavi.data.games.GameBoard

@Composable
fun DiceResultImage(
    gameMode: String?,
    diceResult: String?,
    isHoldable: Boolean = false,
    index: Int = 0,
    isHeld: Boolean = false,
    onHoldToggle: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val drawableId = when (gameMode) {
        GameBoard.PIG.modeName -> getPigDrawable(diceResult)
        GameBoard.GREED.modeName -> getGreedDrawable(diceResult)
        GameBoard.MEXICO.modeName -> getMexicoDrawable(diceResult)
        GameBoard.CHICAGO.modeName -> getChicagoDrawable(diceResult)
        GameBoard.BALUT.modeName -> getBalutDrawable(diceResult)
        else -> R.drawable.ic_no_result
    }

    if (isHoldable && onHoldToggle != null) {
        Box(
            modifier = modifier
                .clickable { onHoldToggle(index) }
                .border(
                    width = if (isHeld) 2.dp else 0.dp,
                    color = if (isHeld) colorResource(id = R.color.buttonColor) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(4.dp)
        ) {
            Image(
                painter = painterResource(id = drawableId),
                contentDescription = "Dice $index",
                modifier = Modifier.size(64.dp)
            )
        }
    } else {
        Image(
            painter = painterResource(id = drawableId),
            contentDescription = "Game Result",
            modifier = modifier.size(64.dp)
        )
    }
}

private fun getPigDrawable(result: String?): Int {
    return when {
        result?.contains("Turn ended") == true -> R.drawable.ic_pig_out
        result == "1" -> R.drawable.ic_pig_out
        result == "0" -> R.drawable.ic_pig_out
        result == "100" -> R.drawable.ic_pig_win
        else -> R.drawable.ic_no_result
    }
}

private fun getGreedDrawable(result: String?): Int {
    return when {
        result?.contains("Three Pairs") == true -> R.drawable.ic_three_pears
        result?.contains("Straight") == true -> R.drawable.arrow_right_long
        result?.contains("Six of a Kind") == true -> R.drawable.ic_kind_six
        result?.contains("Five of a Kind") == true -> R.drawable.ic_kind_five
        result == "No Score" -> R.drawable.ic_no_result
        else -> R.drawable.ic_no_result
    }
}

private fun getMexicoDrawable(result: String?): Int {
    return when {
        result?.contains("¡México!") == true -> R.drawable.mexico
        result?.contains("Double 1s") == true -> R.drawable.ic_group_11
        result?.contains("Double 2s") == true -> R.drawable.ic_group_22
        result?.contains("Double 3s") == true -> R.drawable.ic_group_33
        result?.contains("Double 4s") == true -> R.drawable.ic_group_44
        result?.contains("Double 5s") == true -> R.drawable.ic_group_55
        result?.contains("Double 6s") == true -> R.drawable.ic_group_66
        else -> R.drawable.ic_no_result
    }
}

private fun getChicagoDrawable(result: String?): Int {
    return when {
        result?.contains("2 points") == true -> R.drawable.ic_group_2
        result?.contains("3 points") == true -> R.drawable.ic_group_3
        result?.contains("4 points") == true -> R.drawable.ic_group_4
        result?.contains("5 points") == true -> R.drawable.ic_group_5
        result?.contains("6 points") == true -> R.drawable.ic_group_6
        result?.contains("7 points") == true -> R.drawable.ic_group_7
        result?.contains("8 points") == true -> R.drawable.ic_group_8
        result?.contains("9 points") == true -> R.drawable.ic_group_9
        result?.contains("10 points") == true -> R.drawable.ic_group_10
        result?.contains("11 points") == true -> R.drawable.ic_group_11
        result?.contains("12 points") == true -> R.drawable.ic_group_12
        else -> R.drawable.ic_no_result
    }
}

private fun getBalutDrawable(result: String?): Int {
    if (result == null) return R.drawable.ic_no_result

    return when {
        result.contains("Aces") == true -> R.drawable.ic_group_1
        result.contains("Twos") == true -> R.drawable.ic_group_2
        result.contains("Threes") == true -> R.drawable.ic_group_3
        result.contains("Fours") == true -> R.drawable.ic_group_4
        result.contains("Fives") == true -> R.drawable.ic_group_5
        result.contains("Sixes") == true -> R.drawable.ic_group_6
        result.contains("Straight") == true -> R.drawable.arrow_right_long
        result.contains("Full House") == true -> R.drawable.ic_balut_house
        result.contains("Five of a Kind") == true -> R.drawable.ic_kind_five
        result.contains("Four of a Kind") == true -> R.drawable.ic_kind_four
        result.contains("Choice") == true -> R.drawable.ic_choice
        result.contains("Balut") == true -> R.drawable.ic_balut_balut
        else -> R.drawable.ic_no_result
    }
}

package com.mayor.kavi.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mayor.kavi.R
import com.mayor.kavi.ui.viewmodel.GameBoard

@Composable
fun DiceResultImage(gameMode: String?, diceResult: String?) {
    val drawableId = when (gameMode) {
        GameBoard.PIG.modeName -> getPigDrawable(diceResult)
        GameBoard.GREED.modeName -> getGreedDrawable(diceResult)
        GameBoard.MEXICO.modeName -> getMexicoDrawable(diceResult)
        GameBoard.CHICAGO.modeName -> getChicagoDrawable(diceResult)
        GameBoard.BALUT.modeName -> getBalutDrawable(diceResult)
        else -> R.drawable.ic_no_result
    }

    Image(
        painter = painterResource(id = drawableId),
        contentDescription = "Game Result",
        modifier = Modifier.size(64.dp)
    )
}

private fun getPigDrawable(result: String?): Int {
    return when (result) {
        "0" -> R.drawable.ic_pig_out // Pig out happens when rolling 1 during a round
        "100" -> R.drawable.ic_pig_win // Winning score
        else -> R.drawable.ic_no_result
    }
}

private fun getGreedDrawable(result: String?): Int {
    return when {
        result?.contains("Three Pairs") == true -> R.drawable.ic_three_pairs
        result?.contains("Straight") == true -> R.drawable.ic_straight
        result?.contains("Six of a Kind") == true -> R.drawable.ic_kind_six
        result?.contains("Five of a Kind") == true -> R.drawable.ic_kind_five
        result?.contains("Four of a Kind") == true -> R.drawable.ic_kind_four
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
    return when (result) {
        "1-1" -> R.drawable.ic_group_2
        "1-2" -> R.drawable.ic_group_3
        "2-2" -> R.drawable.ic_group_4
        "2-3" -> R.drawable.ic_group_5
        "3-3" -> R.drawable.ic_group_6
        "3-4" -> R.drawable.ic_group_7
        "4-4" -> R.drawable.ic_group_8
        "4-5" -> R.drawable.ic_group_9
        "5-5" -> R.drawable.ic_group_10
        "5-6" -> R.drawable.ic_group_11
        "6-6" -> R.drawable.ic_group_12
        else -> R.drawable.ic_no_result
    }
}

private fun getBalutDrawable(result: String?): Int {
    return when (result) {
        "fours" -> R.drawable.ic_balut_fours
        "fives" -> R.drawable.ic_balut_fives
        "sixes" -> R.drawable.ic_balut_sixes
        "straight" -> R.drawable.ic_balut_straight
        "house" -> R.drawable.ic_balut_house
        "choice" -> R.drawable.ic_balut_choice
        "balut" -> R.drawable.ic_balut_balut
        else -> R.drawable.ic_no_result
    }
}

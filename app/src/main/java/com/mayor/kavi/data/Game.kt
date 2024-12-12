package com.mayor.kavi.data

data class Game(
    val score: Int,
    val scoreModifier: List<Int>,
    val isShaking: Boolean,
    val dice: List<String>,
    val diceNumber: List<Int>,
    val diceSideNumber: List<Int>,
    val backgroundColor: List<String>
)

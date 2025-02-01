package com.mayor.kavi.data.models.enums

/**
 * Represents the different game modes available.
 *
 * @property modeName The name of the game mode.
 */
enum class GameBoard(val modeName: String) {
    PIG("Pig"),
    GREED("Greed"),
    BALUT("Balut"),
    CUSTOM("Custom")
}
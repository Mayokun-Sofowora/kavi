package com.mayor.kavi.data.games

import androidx.compose.ui.graphics.Color

enum class GameBoard(val modeName: String) {
    PIG("Pig"),
    GREED("Greed"),
    MEXICO("Mexico"),
    CHICAGO("Chicago"),
    BALUT("Balut")
}

object BoardColors {
    val colorMap = mapOf(
        "default" to Color(0xFFD3D3D3),  // Dark gray
        "blue" to Color(0xFF90CAF9),     // Material Blue 200
        "green" to Color(0xFFA5D6A7),    // Material Green 200
        "red" to Color(0xFFEF9A9A),      // Material Red 200
        "orange" to Color(0xFFFFCC80),    //  Material Orange 200
        "night" to Color(0xFF212121)      // Dark gray
    )

    fun getColor(colorName: String): Color {
        return colorMap[colorName] ?: colorMap["default"]!!
    }
}
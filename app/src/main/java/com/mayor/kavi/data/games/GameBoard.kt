package com.mayor.kavi.data.games

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.mayor.kavi.ui.viewmodel.ScoreState

enum class GameBoard(val modeName: String) {
    PIG("Pig"),
    GREED("Greed"),
    MEXICO("Mexico"),
    CHICAGO("Chicago"),
    BALUT("Balut")
}

object BoardColors {
    // Solid colors
    private val colorMap = mapOf(
        "default" to Color(0xFFD3D3D3),  // Dark gray
        "blue" to Color(0xFF90CAF9),     // Material Blue 200
        "green" to Color(0xFFA5D6A7),    // Material Green 200
        "red" to Color(0xFFEF9A9A),      // Material Red 200
        "orange" to Color(0xFFFFCC80),   // Material Orange 200
        "night" to Color(0xFF212121)     // Dark gray
    )

    // Color blend/mixture definitions
    private val blendedColors = mapOf(
        "bluePurple" to Color(0xFF8A2BE2),  // Blend of blue and purple
        "seafoam" to Color(0xFF98FF98),      // Blend of green and white
        "coral" to Color(0xFFFF7F50),        // Blend of orange and pink
        "lavender" to Color(0xFFE6E6FA),     // Blend of light blue and pink
        "turquoise" to Color(0xFF40E0D0)     // Blend of blue and green
    )

    // Gradient definitions
    private val gradientMap = mapOf(
        "sunset" to Brush.horizontalGradient(
            colors = listOf(Color(0xFFFF8C00), Color(0xFFFF0080))
        ),
        "ocean" to Brush.verticalGradient(
            colors = listOf(Color(0xFF00B4DB), Color(0xFF0083B0))
        ),
        "forest" to Brush.verticalGradient(
            colors = listOf(Color(0xFF56AB2F), Color(0xFFA8E063))
        ),
        "twilight" to Brush.linearGradient(
            colors = listOf(Color(0xFF2C3E50), Color(0xFF3498DB))
        ),
        "fire" to Brush.horizontalGradient(
            colors = listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))
        ),
        "beach" to Brush.horizontalGradient(
            colors = listOf(Color(0xFFFCEEB5), Color(0xFF4BC0C8))

        ),
        "sunrise" to Brush.verticalGradient(
            colors = listOf(Color(0xFFFF5722), Color(0xFFFFEB3B))
        )
    )

    fun getColor(colorName: String): Any {
        // First check if it's a gradient
        gradientMap[colorName]?.let { return it }

        // Then check if it's a blended color
        blendedColors[colorName]?.let { return it }

        // Finally check solid colors, with default fallback
        return colorMap[colorName] ?: colorMap["default"]!!
    }

    // Helper method to get available color names for settings
    fun getAvailableColors(): List<String> {
        return (colorMap.keys + blendedColors.keys + gradientMap.keys).distinct()
    }
}


data class GameCondition(
    val getScore: (state: Any) -> Int,
    val checkWin: (state: Any) -> Boolean,
    val winMessage: String
)

object GameRules {
    val gameConditions = mapOf(
        GameBoard.PIG.modeName to GameCondition(
            getScore = { state ->
                val scoreState = state as ScoreState
                scoreState.overallScore + scoreState.currentTurnScore
            },
            checkWin = { state ->
                val scoreState = state as ScoreState
                (scoreState.overallScore + scoreState.currentTurnScore) >= 100
            },
            winMessage = "Congratulations! You've reached 100 points!"
        ),

        GameBoard.GREED.modeName to GameCondition(
            getScore = { state ->
                val greedState = state as GreedScoreState
                greedState.currentScore + greedState.turnScore
            },
            checkWin = { state ->
                val greedState = state as GreedScoreState
                (greedState.currentScore + greedState.turnScore) >= 10000
            },
            winMessage = "Incredible! You've reached 10,000 points!"
        ),

        GameBoard.MEXICO.modeName to GameCondition(
            getScore = { state -> (state as MexicoScoreState).roundScores.sum() },
            checkWin = { state ->
                val mexicoState = state as MexicoScoreState
                mexicoState.isGameOver || mexicoState.lives <= 0
            },
            winMessage = "Game Over! You've survived Mexico!"
        ),

        GameBoard.CHICAGO.modeName to GameCondition(
            getScore = { state -> (state as ChicagoScoreState).totalScore },
            checkWin = { state ->
                val chicagoState = state as ChicagoScoreState
                chicagoState.currentRound >= 12 || chicagoState.totalScore >= 77
            },
            winMessage = "You've completed all rounds of Chicago!"
        ),

        GameBoard.BALUT.modeName to GameCondition(
            getScore = { state -> (state as BalutScoreState).categoryScores.values.sum() },
            checkWin = { state ->
                (state as BalutScoreState).currentRound >= BalutScoreState.categories.size
            },
            winMessage = "You've completed all categories of Balut!"
        )
    )

    fun getGameScore(board: String, state: Any): Int {
        return gameConditions[board]?.getScore?.invoke(state) ?: 0
    }

    fun isGameWon(board: String, state: Any): Boolean {
        return gameConditions[board]?.checkWin?.invoke(state) == true
    }

    fun getWinMessage(board: String): String {
        return gameConditions[board]?.winMessage ?: "Congratulations!"
    }
}
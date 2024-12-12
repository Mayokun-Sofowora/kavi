data class MexicoState(
    val currentPlayer: Int = 1,
    val players: List<PlayerState> = listOf(
        PlayerState(1, 3), // Start with 3 lives
        PlayerState(2, 3)
    ),
    val diceValues: List<Int> = listOf(0, 0),
    val roundScore: Int = 0,
    val isGameOver: Boolean = false
)

data class PlayerState(
    val id: Int,
    val lives: Int
)

class MexicoGame {
    fun rollDice(): List<Int> = List(2) { (1..6).random() }

    fun calculateScore(dice: List<Int>): Int {
        val sorted = dice.sortedDescending()
        return when {
            dice.contains(2) && dice.contains(1) -> 21 // Mexico
            sorted[0] == sorted[1] -> sorted[0] * 100 // Doubles
            else -> sorted[0] * 10 + sorted[1]
        }
    }

    fun updateGameState(currentState: MexicoState, newScore: Int): MexicoState {
        val lowestScore = newScore
        val currentPlayer = currentState.currentPlayer
        val nextPlayer = if (currentPlayer == 1) 2 else 1
        
        // Last player gets one more roll if current player gets Mexico
        if (newScore == 21) {
            return currentState.copy(
                roundScore = newScore,
                currentPlayer = nextPlayer
            )
        }

        // Update lives if necessary
        val updatedPlayers = if (currentState.roundScore > 0 && newScore < currentState.roundScore) {
            currentState.players.map { player ->
                if (player.id == currentPlayer) {
                    player.copy(lives = player.lives - 1)
                } else player
            }
        } else currentState.players

        // Check for game over
        val isGameOver = updatedPlayers.any { it.lives <= 0 }

        return currentState.copy(
            players = updatedPlayers,
            currentPlayer = nextPlayer,
            roundScore = if (currentState.currentPlayer == 1) newScore else 0,
            isGameOver = isGameOver
        )
    }
} 
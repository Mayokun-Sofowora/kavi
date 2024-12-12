data class ChicagoState(
    val currentRound: Int = 1,
    val targetNumber: Int = 2,
    val score: Int = 0,
    val roundScores: MutableMap<Int, Int> = mutableMapOf(),
    val isGameOver: Boolean = false
)

class ChicagoGame {
    private val rounds = (2..12).toList()

    fun rollDice(): List<Int> = List(2) { (1..6).random() }

    fun calculateScore(dice: List<Int>, targetNumber: Int): Int {
        val sum = dice.sum()
        return if (sum == targetNumber) 10 else 0
    }

    fun updateGameState(currentState: ChicagoState, diceRoll: List<Int>): ChicagoState {
        val roundScore = calculateScore(diceRoll, currentState.targetNumber)
        val updatedScores = currentState.roundScores.toMutableMap().apply {
            put(currentState.currentRound, roundScore)
        }
        
        val nextRound = currentState.currentRound + 1
        val isGameOver = nextRound > 11

        return currentState.copy(
            currentRound = nextRound,
            targetNumber = if (isGameOver) currentState.targetNumber else rounds[nextRound - 1],
            score = currentState.score + roundScore,
            roundScores = updatedScores,
            isGameOver = isGameOver
        )
    }
} 
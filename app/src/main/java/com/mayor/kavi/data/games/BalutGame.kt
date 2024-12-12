data class BalutState(
    val currentCategory: String = "Ones",
    val categoryIndex: Int = 0,
    val rollsLeft: Int = 3,
    val diceValues: List<Int> = List(5) { 0 },
    val scores: MutableMap<String, Int> = mutableMapOf(),
    val isGameOver: Boolean = false
)

class BalutGame {
    private val categories = listOf("Ones", "Twos", "Threes", "Fours", "Fives", "Sixes", "Straight", "Full House", "Four of a Kind", "Choice", "Balut")

    fun rollDice(keptDice: List<Int>): List<Int> {
        return List(5) { index ->
            if (keptDice.getOrNull(index) != null) keptDice[index]
            else (1..6).random()
        }
    }

    fun calculateScore(dice: List<Int>, category: String): Int {
        return when (category) {
            "Ones" -> dice.count { it == 1 } * 1
            "Twos" -> dice.count { it == 2 } * 2
            "Threes" -> dice.count { it == 3 } * 3
            "Fours" -> dice.count { it == 4 } * 4
            "Fives" -> dice.count { it == 5 } * 5
            "Sixes" -> dice.count { it == 6 } * 6
            "Straight" -> if (dice.sorted() == listOf(1,2,3,4,5) || dice.sorted() == listOf(2,3,4,5,6)) 30 else 0
            "Full House" -> if (hasFullHouse(dice)) 25 else 0
            "Four of a Kind" -> calculateFourOfAKind(dice)
            "Choice" -> dice.sum()
            "Balut" -> if (dice.distinct().size == 1) 50 else 0
            else -> 0
        }
    }

    private fun hasFullHouse(dice: List<Int>): Boolean {
        val grouped = dice.groupBy { it }
        return grouped.size == 2 && grouped.values.any { it.size == 3 }
    }

    private fun calculateFourOfAKind(dice: List<Int>): Int {
        val grouped = dice.groupBy { it }
        return grouped.entries.find { it.value.size >= 4 }?.key?.times(4) ?: 0
    }

    fun updateGameState(currentState: BalutState, newDice: List<Int>): BalutState {
        val rollsLeft = currentState.rollsLeft - 1
        
        if (rollsLeft < 0) {
            val nextCategoryIndex = currentState.categoryIndex + 1
            val isGameOver = nextCategoryIndex >= categories.size
            
            return currentState.copy(
                categoryIndex = nextCategoryIndex,
                currentCategory = if (isGameOver) currentState.currentCategory else categories[nextCategoryIndex],
                rollsLeft = 3,
                isGameOver = isGameOver
            )
        }

        return currentState.copy(
            diceValues = newDice,
            rollsLeft = rollsLeft
        )
    }
} 
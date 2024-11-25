package com.mayor.kavi.data.models

import java.time.LocalDateTime
import kotlin.random.Random

enum class Dice(val value: Int) {
    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6);

    companion object {
        // Converts a list of integers to corresponding dice values
        fun from(integers: List<Int>): List<Dice> {
            return integers.map { num -> Dice.entries[num - 1] }
        }
    }
}

//data class DicePool(
//    val poolId: Long,
//    val gameId: Long,
//    var content: List<Dice>,
//    var onHold: List<Boolean>,
//    val createdAt: LocalDateTime,
//    var updatedAt: LocalDateTime
//) {
//    companion object {
//        private val random = Random(System.currentTimeMillis())
//
//        /**
//         * Initializes an empty DicePool with a given size.
//         */
//        fun createEmpty(poolId: Long, gameId: Long, size: Int): DicePool {
//            return DicePool(
//                poolId = poolId,
//                gameId = gameId,
//                content = List(size) { Dice.ONE },
//                onHold = List(size) { false },
//                createdAt = LocalDateTime.now(),
//                updatedAt = LocalDateTime.now()
//            )
//        }
//    }
//
//    /**
//     * Rolls the dice, keeping those that are on hold.
//     */
//    fun roll(): List<Dice> {
//        content = content.mapIndexed { index, currentDice ->
//            if (onHold[index]) currentDice else Dice.entries[random.nextInt(6)]
//        }
//        updatedAt = LocalDateTime.now()
//        return content
//    }
//
//    /**
//     * Toggles the hold state of a dice at a given index.
//     */
//    fun toggleHold(index: Int): Boolean {
//        require(index in content.indices) { "Index out of bounds: $index" }
//        onHold = onHold.mapIndexed { i, hold -> if (i == index) !hold else hold }
//        updatedAt = LocalDateTime.now()
//        return onHold[index]
//    }
//
//    /**
//     * Clears the pool by resetting the dice and hold states.
//     */
//    fun clear() {
//        content = List(content.size) { Dice.ONE }
//        onHold = List(content.size) { false }
//        updatedAt = LocalDateTime.now()
//    }
//}

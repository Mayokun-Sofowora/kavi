package com.mayor.kavi.data.models

enum class ScoreType(val scoreCalculator: (List<Dice>) -> Int) {
    ONES({ dices -> dices.count { it == Dice.ONE } * Dice.ONE.value }),
    TWOS({ dices -> dices.count { it == Dice.TWO } * Dice.TWO.value }),
    THREES({ dices -> dices.count { it == Dice.THREE } * Dice.THREE.value }),
    FOURS({ dices -> dices.count { it == Dice.FOUR } * Dice.FOUR.value }),
    FIVES({ dices -> dices.count { it == Dice.FIVE } * Dice.FIVE.value }),
    SIXES({ dices -> dices.count { it == Dice.SIX } * Dice.SIX.value }),

    THREE_OF_A_KIND({ dices -> if (hasDiceGroup(dices, 3)) dices.sumOf { it.value } else 0 }),
    FOUR_OF_A_KIND({ dices -> if (hasDiceGroup(dices, 4)) dices.sumOf { it.value } else 0 }),
    FULL_HOUSE({ dices -> if (checkForFullHouse(dices)) 25 else 0 }),
    SMALL_STRAIGHT({ dices -> if (checkForStraights(dices, smallStraightPattern)) 30 else 0 }),
    LARGE_STRAIGHT({ dices -> if (checkForStraights(dices, largeStraightPattern)) 40 else 0 }),
    FIVE_OF_A_KIND({ dices -> if (hasDiceGroup(dices, 5)) 50 else 0 }),
    CHANCE({ dices -> dices.sumOf { it.value } });

    companion object {
        private fun hasDiceGroup(dices: List<Dice>, count: Int): Boolean {
            val group = groupDiceByValue(dices)
            return group.values.any { it >= count }
        }

        private fun checkForFullHouse(dices: List<Dice>): Boolean {
            val group = groupDiceByValue(dices)
            return group.values.contains(2) && group.values.contains(3)
        }

        private fun checkForStraights(dices: List<Dice>, patternList: List<List<Dice>>): Boolean {
            val diceSet = dices.toSet()
            return patternList.any { pattern -> pattern.toSet().all { diceSet.contains(it) } }
        }

        private fun groupDiceByValue(dices: List<Dice>): Map<Dice, Int> {
            return dices.groupingBy { it }.eachCount()
        }

        private val smallStraightPattern = listOf(
            listOf(Dice.ONE, Dice.TWO, Dice.THREE, Dice.FOUR),
            listOf(Dice.TWO, Dice.THREE, Dice.FOUR, Dice.FIVE),
            listOf(Dice.THREE, Dice.FOUR, Dice.FIVE, Dice.SIX)
        )

        private val largeStraightPattern = listOf(
            listOf(Dice.ONE, Dice.TWO, Dice.THREE, Dice.FOUR, Dice.FIVE),
            listOf(Dice.TWO, Dice.THREE, Dice.FOUR, Dice.FIVE, Dice.SIX)
        )
    }
}

// ScoreCalculator.kt
package com.mayor.kavi.util

import com.mayor.kavi.data.models.GameScoreState
import com.mayor.kavi.ui.viewmodel.GameViewModel.Companion.AI_PLAYER_ID

/**
 * ScoreCalculator is responsible for calculating scores based on dice rolls.
 */
object ScoreCalculator {
    fun calculateGreedScore(dice: List<Int>): Pair<Int, Set<Int>> {
        var score = 0
        val scoringDice = mutableSetOf<Int>()
        val diceFrequency = dice.groupBy { it }

        // Check for special combinations first
        when {
            dice.sorted() == listOf(1, 2, 3, 4, 5, 6) -> {
                score = 1500
                scoringDice.addAll(dice.indices)
                return Pair(score, scoringDice)
            }

            diceFrequency.any { it.value.size == 6 } -> {
                score = 3000
                scoringDice.addAll(dice.indices)
                return Pair(score, scoringDice)
            }

            diceFrequency.any { it.value.size == 5 } -> {
                score = 2000
                scoringDice.addAll(dice.indices)
                return Pair(score, scoringDice)
            }

            diceFrequency.count { it.value.size == 2 } == 3 -> {
                score = 1500
                scoringDice.addAll(dice.indices)
                return Pair(score, scoringDice)
            }
        }

        // Process remaining combinations
        for ((number, occurrences) in diceFrequency) {
            when {
                occurrences.size >= 3 -> {
                    score += if (number == 1) 1000 else number * 100
                    scoringDice.addAll(occurrences.take(3).map { dice.indexOf(it) })

                    // Score remaining 1s and 5s
                    val remaining = occurrences.size - 3
                    if (number == 1 || number == 5) {
                        score += remaining * (GameScoreState.GreedScoreState.SCORING_VALUES[number]
                            ?: 0)
                        scoringDice.addAll(occurrences.takeLast(remaining).map { dice.indexOf(it) })
                    }
                }

                number == 1 || number == 5 -> {
                    score += occurrences.size * (GameScoreState.GreedScoreState.SCORING_VALUES[number]
                        ?: 0)
                    scoringDice.addAll(occurrences.map { dice.indexOf(it) })
                }
            }
        }

        return Pair(score, scoringDice)
    }

    fun calculateBalutScore(dice: List<Int>): Pair<Int, Set<Int>> {
        val grouped = dice.groupBy { it }

        // Check for Five of a Kind (all dice showing same number)
        if (grouped.values.any { it.size == 5 }) {
            grouped.entries.first { it.value.size == 5 }.key
            return Pair(50, dice.mapIndexed { index, d -> index }.toSet())
        }

        // Check for Four of a Kind
        grouped.entries.find { it.value.size >= 4 }?.let { entry ->
            val scoringDice = dice.mapIndexedNotNull { index, d ->
                if (d == entry.key) index else null
            }.take(4).toSet()
            return Pair(40, scoringDice)
        }

        // Check for Full House
        if (grouped.size == 2 && grouped.values.any { it.size == 3 }) {
            return Pair(35, dice.mapIndexed { index, _ -> index }.toSet())
        }

        // Check for Straight (1-2-3-4-5 or 2-3-4-5-6)
        val sorted = dice.sorted()
        if (sorted.windowed(2).all { (a, b) -> b - a == 1 }) {
            return Pair(30, dice.mapIndexed { index, _ -> index }.toSet())
        }

        // No scoring combination
        return Pair(0, emptySet())
    }

}

object GameMessages {
    private fun getPlayerName(playerIndex: Int): String = when (playerIndex) {
        AI_PLAYER_ID.hashCode() -> "AI"
        else -> "You"
    }

    fun buildPigScoreMessage(die: Int, score: Int, playerIndex: Int): String = when {
        die == 1 -> "${getPlayerName(playerIndex)}: Rolled 1 - " +
                (if (playerIndex == AI_PLAYER_ID.hashCode()) "Your turn!" else "AI's Turn!")

        score >= 100 -> "${getPlayerName(playerIndex)} win${
            if (playerIndex == AI_PLAYER_ID.hashCode())
                "s" else ""
        } with $score points!"

        else -> "${getPlayerName(playerIndex)}: Rolled $die - Turn score: $score"
    }

//    fun buildGreedScoreMessage(
//        dice: List<Int>,
//        score: Int,
//        turnScore: Int,
//        playerIndex: Int,
//        roundHistory: Map<Int, List<Int>>
//    ): String {
//        if (score == 0 || score == 1500) return "${getPlayerName(playerIndex)} busts! No scoring dice."
//
//        val message = buildString {
//            append("${getPlayerName(playerIndex)} rolled: ${dice.joinToString()}\n")
//
//            when {
//                // Special combinations
//                dice.sorted() == listOf(1, 2, 3, 4, 5, 6) -> append("Straight! +1000")
//                dice.groupBy { it }.any { it.value.size == 6 } -> {
//                    val num = dice[0]
//                    val fiveOfKind = 2000 * num
//                    append("Six of a Kind! +${fiveOfKind * 2}")
//                }
//
//                dice.groupBy { it }.any { it.value.size == 5 } -> {
//                    val num = dice.groupBy { it }.entries.first { it.value.size == 5 }.key
//                    val fourOfKind = 1000 * num
//                    append("Five of a Kind! +${fourOfKind * 2}")
//                }
//
//                dice.groupBy { it }.any { it.value.size == 4 } -> {
//                    val num = dice.groupBy { it }.entries.first { it.value.size == 4 }.key
//                    val threeOfKind = if (num == 1) 1000 else num * 100
//                    append("Four of a Kind! +${threeOfKind * 2}")
//                }
//
//                dice.groupBy { it }
//                    .count { it.value.size == 2 } == 3 -> append("Three Pairs! +1000")
//
//                else -> {
//                    // Handle three of a kind and single scoring dice
//                    dice.groupBy { it }.forEach { (num, group) ->
//                        when (group.size) {
//                            3 -> appendLine("Three ${num}s: +${if (num == 1) 1000 else num * 100}")
//                            in 1..2 -> if (num in setOf(1, 5)) {
//                                appendLine("${group.size} × $num: +${group.size * (if (num == 1) 100 else 50)}")
//                            }
//                        }
//                    }
//                }
//            }
//
//            appendLine("\nTurn Score: $turnScore")
//            val isOnBoard = (roundHistory[playerIndex] ?: emptyList()).any { it >= 800 }
//            if (!isOnBoard && turnScore < 800) {
//                appendLine("Need ${800 - turnScore} more points to get on board")
//            }
//        }
//        return message.trim()
//    }

    fun buildGreedScoreMessage(
        dice: List<Int>,
        score: Int,
        turnScore: Int,
        playerIndex: Int,
        roundHistory: Map<Int, List<Int>>
    ): String {
        if (score == 0) return "${getPlayerName(playerIndex)} busts! No scoring dice."

        return buildString {
            append("${getPlayerName(playerIndex)} rolled: ${dice.joinToString()}\n")

            when {
                // Straight
                dice.toSet() == setOf(1, 2, 3, 4, 5, 6) -> appendLine("Straight! +1000")

                // Six of a Kind
                dice.groupBy { it }.any { it.value.size == 6 } -> {
                    val num = dice[0]
                    appendLine("Six of a Kind! +${num * 400}")
                }

                // Five of a Kind
                dice.groupBy { it }.any { it.value.size == 5 } -> {
                    val num = dice.groupBy { it }.entries.first { it.value.size == 5 }.key
                    appendLine("Five of a Kind! +${num * 200}")
                }

                // Four of a Kind
                dice.groupBy { it }.any { it.value.size == 4 } -> {
                    val num = dice.groupBy { it }.entries.first { it.value.size == 4 }.key
                    appendLine("Four of a Kind! +${num * 100}")
                }

                // Three Pairs
                dice.groupBy { it }.count { it.value.size == 2 } == 3 -> appendLine("Three Pairs! +1000")

                else -> {
                    // Handle Three of a Kind and single scoring dice
                    dice.groupBy { it }.forEach { (num, group) ->
                        when (group.size) {
                            3 -> appendLine("Three ${num}s: +${if (num == 1) 1000 else num * 100}")
                            in 1..2 -> if (num in setOf(1, 5)) {
                                appendLine("${group.size} × $num: +${group.size * (if (num == 1) 100 else 50)}")
                            }
                        }
                    }
                }
            }

            appendLine("\nTurn Score: $turnScore")

            val isOnBoard = (roundHistory[playerIndex] ?: emptyList()).any { it >= 800 }
            if (!isOnBoard && turnScore < 800) {
                appendLine("Need ${800 - turnScore} more points to get on board")
            }
        }.trim()
    }


    fun buildBalutCategoryMessage(
        dice: List<Int>,
        category: String,
        score: Int,
        playerIndex: Int,
        isGameOver: Boolean = false,
        totalScore: Int = 0
    ): String = when {
        isGameOver -> buildBalutEndGameMessage(totalScore, playerIndex)
        else -> "${getPlayerName(playerIndex)} - $category: ${dice.joinToString()} = $score points"
    }

    private fun buildBalutEndGameMessage(totalScore: Int, playerIndex: Int): String {
        val playerName = getPlayerName(playerIndex)
        return if (playerIndex == AI_PLAYER_ID.hashCode()) {
            "$playerName wins with $totalScore points! Better luck next time!"
        } else {
            "$playerName win with $totalScore points!"
        }
    }

}

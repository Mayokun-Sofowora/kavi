package com.mayor.kavi.ui.components

import com.mayor.kavi.ui.viewmodel.GameBoard
import kotlin.random.Random

class GameAI {
    fun makeDecision(
        gameBoard: String,
        currentScore: Int,
        diceResults: List<Int>,
        opponentScore: Int = 0
    ): AIDecision {
        return when (gameBoard) {
            GameBoard.PIG.modeName -> decidePigMove(
                currentScore,
                diceResults.first(),
                opponentScore
            )

            GameBoard.GREED.modeName -> decideGreedMove(currentScore, diceResults, opponentScore)
            GameBoard.MEXICO.modeName -> decideMexicoMove(diceResults)
            GameBoard.CHICAGO.modeName -> decideChicagoMove(currentScore, diceResults)
            GameBoard.BALUT.modeName -> decideBalutMove(diceResults)
            else -> AIDecision.Roll
        }
    }

    sealed class AIDecision {
        object Roll : AIDecision()
        object Bank : AIDecision()
        data class SelectDice(val indices: List<Int>) : AIDecision()
    }

    private fun decidePigMove(currentScore: Int, lastRoll: Int, opponentScore: Int): AIDecision {
        return when {
            lastRoll == 1 -> AIDecision.Bank
            currentScore >= 100 -> AIDecision.Bank
            opponentScore >= 80 ->
                when {  // More aggressive when opponent is close to winning
                    currentScore < 20 -> AIDecision.Roll
                    currentScore >= 20 -> AIDecision.Bank
                    else -> AIDecision.Roll
                }

            currentScore >= 20 && (lastRoll <= 3 || Random.nextFloat() < 0.7f) -> AIDecision.Bank
            currentScore >= 25 -> AIDecision.Bank
            else -> AIDecision.Roll
        }
    }

    private fun decideGreedMove(
        currentScore: Int,
        diceResults: List<Int>,
        opponentScore: Int
    ): AIDecision {
        val currentRollScore = calculateGreedScore(diceResults)
        return when {
            currentRollScore == 0 -> AIDecision.Bank
            opponentScore >= 8000 -> AIDecision.Roll // Very aggressive when opponent is close
            currentScore + currentRollScore >= 10000 -> AIDecision.Bank
            currentScore >= 7500 -> when {
                currentRollScore >= 500 -> AIDecision.Bank
                else -> AIDecision.Roll
            }

            currentScore >= 5000 -> when {
                currentRollScore >= 1000 -> AIDecision.Bank
                else -> AIDecision.Roll
            }

            else -> when {
                currentRollScore >= 2000 -> AIDecision.Bank
                else -> AIDecision.Roll
            }
        }
    }

    private fun decideMexicoMove(diceResults: List<Int>): AIDecision {
        val score = calculateMexicoScore(diceResults)
        return when {
            score == 21 -> AIDecision.Bank // Mexico - always bank
            score >= 65 -> AIDecision.Bank // High doubles
            score >= 53 && Random.nextFloat() < 0.8f -> AIDecision.Bank // Good score
            score <= 31 -> AIDecision.Roll // Low score, try again
            else -> if (Random.nextFloat() < 0.6f) AIDecision.Bank else AIDecision.Roll
        }
    }

    private fun decideChicagoMove(currentScore: Int, diceResults: List<Int>): AIDecision {
        val targetNumber = (currentScore % 11) + 2 // Chicago targets 2-12 in sequence
        val sum = diceResults.sum()
        return when {
            sum == targetNumber -> AIDecision.Bank
            diceResults.size >= 2 && Random.nextFloat() < 0.7f -> AIDecision.Bank
            else -> AIDecision.Roll
        }
    }

    private fun decideBalutMove(diceResults: List<Int>): AIDecision {
        val grouped = diceResults.groupBy { it }
        return when {
            grouped.size == 1 -> AIDecision.Bank // Balut
            grouped.any { it.value.size >= 4 } -> AIDecision.Bank // Four of a kind
            grouped.size == 2 && grouped.any { it.value.size == 3 } -> AIDecision.Bank // Full house
            diceResults.sorted() == (1..5).toList() || diceResults.sorted() == (2..6).toList() -> AIDecision.Bank // Straight
            else -> {
                val keepIndices = findBestDiceToKeep(diceResults)
                AIDecision.SelectDice(keepIndices)
            }
        }
    }

    private fun findBestDiceToKeep(dice: List<Int>): List<Int> {
        val grouped = dice.groupingBy { it }.eachCount()
        return when {
            grouped.any { it.value >= 3 } -> dice.mapIndexedNotNull { index, value ->
                if (value == grouped.maxByOrNull { it.value }?.key) index else null
            }

            grouped.size >= 4 -> dice.mapIndexedNotNull { index, value ->
                if (value in 1..6) index else null
            }

            else -> emptyList()
        }
    }

    private fun calculateGreedScore(dice: List<Int>): Int {
        // Implement Greed scoring rules
        return 0 // Placeholder
    }

    private fun calculateMexicoScore(dice: List<Int>): Int {
        if (dice.size != 2) return 0
        return when {
            dice.toSet() == setOf(2, 1) -> 21
            dice[0] == dice[1] -> dice[0] * 11
            else -> maxOf(dice[0], dice[1]) * 10 + minOf(dice[0], dice[1])
        }
    }
}

package com.mayor.kavi.data.manager.games

import com.mayor.kavi.data.models.GameScoreState.CustomScoreState
import java.util.UUID
import javax.inject.Inject

class MyGameManager @Inject constructor(
) {
    companion object {
        const val MAX_DICE = 6  // Maximum number of dice that can be rolled
        const val DEFAULT_DICE = 6  // Default number of dice for a new game
        const val MIN_PLAYERS = 2  // Minimum number of players
        const val MAX_PLAYERS = 6  // Maximum number of players
    }

    fun initializeGame(gameId: String? = null): CustomScoreState {
        val id = gameId ?: UUID.randomUUID().toString()
        return CustomScoreState(
            gameId = id,
            playerScores = (0 until MIN_PLAYERS).associateWith { 0 },
            currentPlayerIndex = 0,
            message = "Welcome to Custom Dice Scorer! Roll dice and save your scores.",
            diceCount = DEFAULT_DICE,
            scoreHistory = (0 until MIN_PLAYERS).associateWith { emptyList<String>() },
            gameName = "Custom Dice Game",
            playerNames = (0 until MIN_PLAYERS).associateWith { "Player ${it + 1}" }
        )
    }

    fun handleTurn(currentState: CustomScoreState, diceResults: List<Int>): CustomScoreState {
        // Calculate total of dice
        val score = diceResults.sum()

        // Create message showing dice rolls and total
        val message = buildString {
            append("Rolled: ${diceResults.joinToString()}")
            append("\nTotal: $score")
        }

        // Don't automatically switch turns or update scores - let the user control this
        return currentState.copy(message = message)
    }

    fun addPlayer(currentState: CustomScoreState): CustomScoreState {
        if (currentState.playerCount >= MAX_PLAYERS) {
            return currentState.copy(message = "Maximum number of players reached (${MAX_PLAYERS})")
        }

        val newPlayerIndex = currentState.playerCount
        val updatedScores = currentState.playerScores + (newPlayerIndex to 0)
        val updatedHistory = currentState.scoreHistory + (newPlayerIndex to emptyList())
        val updatedNames =
            currentState.playerNames + (newPlayerIndex to "Player ${newPlayerIndex + 1}")

        return currentState.copy(
            playerScores = updatedScores,
            scoreHistory = updatedHistory,
            playerNames = updatedNames,
            playerCount = currentState.playerCount + 1,
            message = "Added Player ${newPlayerIndex + 1}"
        )
    }

    fun updatePlayerName(
        currentState: CustomScoreState,
        playerIndex: Int,
        name: String
    ): CustomScoreState {
        if (playerIndex >= currentState.playerCount) {
            return currentState.copy(message = "Invalid player index")
        }

        val updatedNames = currentState.playerNames + (playerIndex to name)
        return currentState.copy(
            playerNames = updatedNames,
            message = "Updated player name to $name"
        )
    }

    fun addScore(currentState: CustomScoreState, playerIndex: Int, score: Int): CustomScoreState {
        val currentScore = currentState.playerScores[playerIndex] ?: 0
        val updatedScores = currentState.playerScores + (playerIndex to currentScore + score)

        // Add score to history
        val playerHistory = currentState.scoreHistory[playerIndex] ?: emptyList()
        val updatedHistory =
            currentState.scoreHistory + (playerIndex to playerHistory + "Score: $score")

        return currentState.copy(
            playerScores = updatedScores,
            scoreHistory = updatedHistory,
            message = "Added score $score for ${currentState.playerNames[playerIndex]}"
        )
    }

    fun addNote(currentState: CustomScoreState, playerIndex: Int, note: String): CustomScoreState {
        val playerHistory = currentState.scoreHistory[playerIndex] ?: emptyList()
        val updatedHistory = currentState.scoreHistory + (playerIndex to playerHistory + note)
        return currentState.copy(
            scoreHistory = updatedHistory,
            message = "Added note for ${currentState.playerNames[playerIndex]}"
        )
    }

    fun setDiceCount(currentState: CustomScoreState, count: Int): CustomScoreState {
        val newCount = count.coerceIn(1, MAX_DICE)
        return currentState.copy(
            diceCount = newCount,
            message = "Number of dice set to $newCount"
        )
    }

    fun setGameName(currentState: CustomScoreState, name: String): CustomScoreState {
        return currentState.copy(
            gameName = name,
            message = "Game name set to: $name"
        )
    }

    fun resetScores(currentState: CustomScoreState): CustomScoreState {
        return initializeGame(currentState.gameId).copy(
            message = "Reset board!"
        )
    }

}
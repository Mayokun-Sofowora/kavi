package com.mayor.kavi.data.repository.fakes

import com.mayor.kavi.data.models.Game
import com.mayor.kavi.data.models.GameMode
import com.mayor.kavi.data.models.GameType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class FakeGameRepositoryTest {

    private lateinit var gameRepository: FakeGameRepository

    @Before
    fun setUp() {
        gameRepository = FakeGameRepository()
    }

    @Test
    fun `test saveGame and getAllGames`() = runBlocking {
        val game = Game(
            id = 1L,
            players = listOf("player1", "player2"),
            score = mapOf("player1" to 10, "player2" to 15),
            timestamp = System.currentTimeMillis(),
            gameMode = GameMode.CLASSIC,
            gameType = GameType.LOCAL_MULTIPLAYER,
            currentPlayerIndex = 0,
            diceValues = listOf(1, 2, 3, 4, 5),
            lockedDice = listOf(false, false, true, false, true),
            remainingRolls = 3,
            imageRecognitionMode = false,
            recognizedDiceValues = listOf(1, 2, 3),
            isPlayerWinner = {playerName -> true},
            isPlayersTurn = { playerName -> false }
        )
        gameRepository.saveGame(game)

        val games = gameRepository.getAllGames().first()
        assertEquals(1, games.size)
        assertEquals(game.id, games[0].id)
    }

    @Test
    fun `test deleteGame`() = runBlocking {
        val game = Game(
            id = 1L,
            players = listOf("player1", "player2"),
            score = mapOf("player1" to 10, "player2" to 15),
            timestamp = System.currentTimeMillis(),
            gameMode = GameMode.CLASSIC,
            gameType = GameType.LOCAL_MULTIPLAYER,
            currentPlayerIndex = 0,
            diceValues = listOf(1, 2, 3, 4, 5),
            lockedDice = listOf(false, false, true, false, true),
            remainingRolls = 3,
            imageRecognitionMode = false,
            recognizedDiceValues = listOf(1, 2, 3),
            isPlayerWinner = {playerName -> true},
            isPlayersTurn = { playerName -> false }
        )
        gameRepository.saveGame(game)
        gameRepository.deleteGame(1L)

        val games = gameRepository.getAllGames().first()
        assertEquals(0, games.size) // Ensure the game is deleted
    }

    @Test
    fun `test updateGame`() = runBlocking {
        val game = Game(
            id = 1L,
            players = listOf("player1", "player2"),
            score = mapOf("player1" to 10, "player2" to 15),
            timestamp = System.currentTimeMillis(),
            gameMode = GameMode.CLASSIC,
            gameType = GameType.LOCAL_MULTIPLAYER,
            currentPlayerIndex = 0,
            diceValues = listOf(1, 2, 3, 4, 5),
            lockedDice = listOf(false, false, true, false, true),
            remainingRolls = 3,
            imageRecognitionMode = false,
            recognizedDiceValues = listOf(1, 2, 3),
            isPlayerWinner = {playerName -> true},
            isPlayersTurn = { playerName -> false }
        )
        gameRepository.saveGame(game)

        val updatedGame = game.copy(score = mapOf("player1" to 20, "player2" to 15))
        gameRepository.saveGame(updatedGame)

        val games = gameRepository.getAllGames().first()
        val updatedGameFromRepo = games.find { it.id == 1L }

        assertEquals(20, updatedGameFromRepo?.score?.get("player1")) // Ensure score is updated
        assertEquals(15, updatedGameFromRepo?.score?.get("player2"))
    }

    @Test
    fun `test getGameById`() = runBlocking {
        val game = Game(
            id = 1L,
            players = listOf("player1", "player2"),
            score = mapOf("player1" to 10, "player2" to 15),
            timestamp = System.currentTimeMillis(),
            gameMode = GameMode.CLASSIC,
            gameType = GameType.LOCAL_MULTIPLAYER,
            currentPlayerIndex = 0,
            diceValues = listOf(1, 2, 3, 4, 5),
            lockedDice = listOf(false, false, true, false, true),
            remainingRolls = 3,
            imageRecognitionMode = false,
            recognizedDiceValues = listOf(1, 2, 3),
            isPlayerWinner = {playerName -> true},
            isPlayersTurn = { playerName -> false }
        )
        gameRepository.saveGame(game)

        val retrievedGame = gameRepository.getGameById(1L)
        assertEquals(game.id, retrievedGame?.id) // Ensure the game is retrieved by ID
    }

    // New Tests for Filtering

    @Test
    fun `test getGamesByMode`() = runBlocking {
        val game1 = Game(
            id = 1L,
            players = listOf("player1", "player2"),
            score = mapOf("player1" to 10, "player2" to 15),
            timestamp = System.currentTimeMillis(),
            gameMode = GameMode.CLASSIC,
            gameType = GameType.LOCAL_MULTIPLAYER,
            currentPlayerIndex = 0,
            diceValues = listOf(1, 2, 3, 4, 5),
            lockedDice = listOf(false, false, true, false, true),
            remainingRolls = 3,
            imageRecognitionMode = false,
            recognizedDiceValues = listOf(1, 2, 3),
            isPlayerWinner = {playerName -> true},
            isPlayersTurn = { playerName -> false }
        )
        val game2 = Game(
            id = 2L,
            players = listOf("player3", "player4"),
            score = mapOf("player3" to 20, "player4" to 25),
            timestamp = System.currentTimeMillis() + 1000,
            gameMode = GameMode.VIRTUAL,
            gameType = GameType.COMPUTER_AI,
            currentPlayerIndex = 1,
            diceValues = listOf(5, 4, 3, 2, 1),
            lockedDice = listOf(true, true, false, false, true),
            remainingRolls = 2,
            imageRecognitionMode = true,
            recognizedDiceValues = listOf(1, 2, 3),
            isPlayerWinner = {playerName -> true},
            isPlayersTurn = { playerName -> false }
        )
        gameRepository.saveGame(game1)
        gameRepository.saveGame(game2)

        val classicGames = gameRepository.getGamesByMode(GameMode.CLASSIC).first()
        assertEquals(1, classicGames.size) // Only 1 game should be returned
        assertEquals(game1.id, classicGames[0].id) // The game should match the mode
    }

    @Test
    fun `test getGamesByType`() = runBlocking {
        val game1 = Game(
            id = 1L,
            players = listOf("player1", "player2"),
            score = mapOf("player1" to 10, "player2" to 15),
            timestamp = System.currentTimeMillis(),
            gameMode = GameMode.CLASSIC,
            gameType = GameType.LOCAL_MULTIPLAYER,
            currentPlayerIndex = 0,
            diceValues = listOf(1, 2, 3, 4, 5),
            lockedDice = listOf(false, false, true, false, true),
            remainingRolls = 3,
            imageRecognitionMode = false,
            recognizedDiceValues = listOf(1, 2, 3),
            isPlayerWinner = {playerName -> true},
            isPlayersTurn = { playerName -> false }
        )
        val game2 = Game(
            id = 2L,
            players = listOf("player3", "player4"),
            score = mapOf("player3" to 20, "player4" to 25),
            timestamp = System.currentTimeMillis() + 1000,
            gameMode = GameMode.VIRTUAL,
            gameType = GameType.COMPUTER_AI,
            currentPlayerIndex = 1,
            diceValues = listOf(5, 4, 3, 2, 1),
            lockedDice = listOf(true, true, false, false, true),
            remainingRolls = 2,
            imageRecognitionMode = true,
            recognizedDiceValues = listOf(1, 2, 3),
            isPlayerWinner = {playerName -> true},
            isPlayersTurn = { playerName -> false }
        )
        gameRepository.saveGame(game1)
        gameRepository.saveGame(game2)

        val onlineGames = gameRepository.getGamesByType(GameType.COMPUTER_AI).first()
        assertEquals(1, onlineGames.size) // Only 1 game should be returned
        assertEquals(game2.id, onlineGames[0].id) // The game should match the type
    }

    @Test
    fun `test getGamesByPlayer`() = runBlocking {
        val game1 = Game(
            id = 1L,
            players = listOf("player1", "player2"),
            score = mapOf("player1" to 10, "player2" to 15),
            timestamp = System.currentTimeMillis(),
            gameMode = GameMode.CLASSIC,
            gameType = GameType.LOCAL_MULTIPLAYER,
            currentPlayerIndex = 0,
            diceValues = listOf(1, 2, 3, 4, 5),
            lockedDice = listOf(false, false, true, false, true),
            remainingRolls = 3,
            imageRecognitionMode = false,
            recognizedDiceValues = listOf(1, 2, 3),
            isPlayerWinner = {playerName -> true},
            isPlayersTurn = { playerName -> false }
        )
        val game2 = Game(
            id = 2L,
            players = listOf("player3", "player4"),
            score = mapOf("player3" to 20, "player4" to 25),
            timestamp = System.currentTimeMillis() + 1000,
            gameMode = GameMode.VIRTUAL,
            gameType = GameType.COMPUTER_AI,
            currentPlayerIndex = 1,
            diceValues = listOf(5, 4, 3, 2, 1),
            lockedDice = listOf(true, true, false, false, true),
            remainingRolls = 2,
            imageRecognitionMode = true,
            recognizedDiceValues = listOf(1, 2, 3),
            isPlayerWinner = {playerName -> true},
            isPlayersTurn = { playerName -> false }
        )
        gameRepository.saveGame(game1)
        gameRepository.saveGame(game2)

        val player2Games = gameRepository.getGamesByPlayer("player2").first()
        assertEquals(2, player2Games.size) // Both games should include player2
    }

    @Test
    fun `test getGamesByTimestampRange`() = runBlocking {
        val game1 = Game(
            id = 1L,
            players = listOf("player1", "player2"),
            score = mapOf("player1" to 10, "player2" to 15),
            timestamp = System.currentTimeMillis(),
            gameMode = GameMode.CLASSIC,
            gameType = GameType.LOCAL_MULTIPLAYER,
            currentPlayerIndex = 0,
            diceValues = listOf(1, 2, 3, 4, 5),
            lockedDice = listOf(false, false, true, false, true),
            remainingRolls = 3,
            imageRecognitionMode = false,
            recognizedDiceValues = listOf(1, 2, 3),
            isPlayerWinner = {playerName -> true},
            isPlayersTurn = { playerName -> false }
        )
        val game2 = Game(
            id = 2L,
            players = listOf("player3", "player4"),
            score = mapOf("player3" to 20, "player4" to 25),
            timestamp = System.currentTimeMillis() + 1000,
            gameMode = GameMode.VIRTUAL,
            gameType = GameType.COMPUTER_AI,
            currentPlayerIndex = 1,
            diceValues = listOf(5, 4, 3, 2, 1),
            lockedDice = listOf(true, true, false, false, true),
            remainingRolls = 2,
            imageRecognitionMode = true,
            recognizedDiceValues = listOf(1, 2, 3),
            isPlayerWinner = {playerName -> true},
            isPlayersTurn = { playerName -> false }
        )
        gameRepository.saveGame(game1)
        gameRepository.saveGame(game2)

        val startTime = System.currentTimeMillis()
        val endTime = System.currentTimeMillis() + 1500
        val gamesInRange = gameRepository.getGamesByTimestampRange(startTime, endTime).first()
        assertEquals(2, gamesInRange.size) // Both games should be within the timestamp range
    }
}

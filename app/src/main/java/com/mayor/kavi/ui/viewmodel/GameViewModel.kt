package com.mayor.kavi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mayor.kavi.data.repository.*
import com.mayor.kavi.data.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/***
 * Purpose: Manage the game state across screens like the classic mode, dice rolls, scores, and end game logic.
 * Responsibilities:
 * Store dice values, locked states, and current game scores.
 * Handle game logic (e.g., scoring rules, resetting state).
 * Communicate with the repository to save game statistics or achievements.
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val leaderboardRepository: LeaderboardRepository,
    private val achievementRepository: AchievementRepository // New
) : ViewModel() {

    // Additional UI States
    private val _achievements = MutableStateFlow<AchievementsUiState>(AchievementsUiState.Initial)
    val achievements: StateFlow<AchievementsUiState> = _achievements.asStateFlow()

    private val _leaderboard = MutableStateFlow<LeaderboardUiState>(LeaderboardUiState.Initial)
    val leaderboard: StateFlow<LeaderboardUiState> = _leaderboard.asStateFlow()

    private val _statistics = MutableStateFlow<StatisticsUiState>(StatisticsUiState.Initial)
    val statistics: StateFlow<StatisticsUiState> = _statistics.asStateFlow()

    // State to hold list of games
    private val _games = MutableStateFlow<GamesUiState>(GamesUiState.Initial)
    val games: StateFlow<GamesUiState> = _games.asStateFlow()

    // State to hold game details
    private val _selectedGame = MutableStateFlow<GameUiState>(GameUiState.Initial)
    val selectedGame: StateFlow<GameUiState> = _selectedGame.asStateFlow()


    // Add to existing game state updates
    private fun updateGameState(newGame: Game) {
        viewModelScope.launch {
            try {
                gameRepository.saveGame(newGame)
                _selectedGame.value = GameUiState.Success(newGame)

                // Check for achievements after game state updates
                checkAchievements(newGame)
                // Update statistics
                updateStatistics(newGame)
            } catch (e: Exception) {
                _selectedGame.value = GameUiState.Error(e.message ?: "Failed to update game state")
            }
        }
    }

    // Achievement handling
    private fun checkAchievements(game: Game) {
        viewModelScope.launch {
            try {
                val newAchievements = achievementRepository.checkNewAchievements(game)
                if (newAchievements.isNotEmpty()) {
                    // Update UI state with new achievements
                    _achievements.value = AchievementsUiState.NewUnlocked(newAchievements)
                }
            } catch (e: Exception) {
                _achievements.value =
                    AchievementsUiState.Error(e.message ?: "Achievement check failed")
            }
        }
    }

    fun fetchAchievements() {
        viewModelScope.launch {
            _achievements.value = AchievementsUiState.Loading
            try {
                achievementRepository.getAllAchievements()
                    .collect { achievementsList ->
                        _achievements.value = AchievementsUiState.Success(achievementsList)
                    }
            } catch (e: Exception) {
                _achievements.value =
                    AchievementsUiState.Error(e.message ?: "Failed to fetch achievements")
            }
        }
    }

    // Statistics handling
    private fun updateStatistics(game: Game) {
        viewModelScope.launch {
            try {
                gameRepository.updatePlayerStats(game)
                fetchStatistics(game.players)
            } catch (e: Exception) {
                _statistics.value =
                    StatisticsUiState.Error(e.message ?: "Failed to update statistics")
            }
        }
    }

    fun fetchStatistics(playerName: List<String>) {
        viewModelScope.launch {
            _statistics.value = StatisticsUiState.Loading
            try {
                val stats = gameRepository.getPlayerStats(playerName).toList()
                val statsMap =
                    stats.associateBy { it.playerId } // Assuming PlayerStats has a playerName property
                _statistics.value = StatisticsUiState.Success(statsMap)
            } catch (e: Exception) {
                _statistics.value =
                    StatisticsUiState.Error(e.message ?: "Failed to fetch statistics")
            }
        }
    }


    // Enhanced leaderboard handling
    fun fetchLeaderboard() {
        viewModelScope.launch {
            _leaderboard.value = LeaderboardUiState.Loading
            try {
                leaderboardRepository.getLeaderboard()
                    .collect { entries ->
                        _leaderboard.value = LeaderboardUiState.Success(entries)
                    }
            } catch (e: Exception) {
                _leaderboard.value =
                    LeaderboardUiState.Error(e.message ?: "Failed to fetch leaderboard")
            }
        }
    }

    // Additional UI States
    sealed class AchievementsUiState {
        object Initial : AchievementsUiState()
        object Loading : AchievementsUiState()
        data class Success(val achievements: List<Achievement>) : AchievementsUiState()
        data class NewUnlocked(val newAchievements: List<Achievement>) : AchievementsUiState()
        data class Error(val message: String) : AchievementsUiState()
    }

    sealed class LeaderboardUiState {
        object Initial : LeaderboardUiState()
        object Loading : LeaderboardUiState()
        data class Success(val entries: List<LeaderboardEntry>) : LeaderboardUiState()
        data class Error(val message: String) : LeaderboardUiState()
    }

    sealed class StatisticsUiState {
        object Initial : StatisticsUiState()
        object Loading : StatisticsUiState()
        data class Success(val statistics: Map<String, PlayerStats>) : StatisticsUiState()
        data class Error(val message: String) : StatisticsUiState()
    }

    // Fetch all games
    fun fetchAllGames() {
        viewModelScope.launch {
            _games.value = GamesUiState.Loading
            try {
                gameRepository.getAllGames()
                    .catch { e ->
                        _games.value = GamesUiState.Error(e.localizedMessage ?: "Unknown error")
                    }
                    .collect { gamesList -> _games.value = GamesUiState.Success(gamesList) }
            } catch (e: Exception) {
                _games.value = GamesUiState.Error(e.localizedMessage ?: "Fetch failed")
            }
        }
    }

    // Save a new game
    fun saveGame(game: Game) {
        viewModelScope.launch {
            try {
                gameRepository.saveGame(game)
                // Optionally refresh games list or update UI
            } catch (e: Exception) {
                _selectedGame.value = GameUiState.Error(e.localizedMessage ?: "Save failed")
            }
        }
    }

    // Delete a game
    fun deleteGame(gameId: Long) {
        viewModelScope.launch {
            try {
                gameRepository.deleteGame(gameId)
                // Remove the game from local state
                val currentState = _games.value
                if (currentState is GamesUiState.Success) {
                    val updatedList = currentState.games.filter { it.id != gameId }
                    _games.value = GamesUiState.Success(updatedList)
                }
            } catch (e: Exception) {
                _games.value = GamesUiState.Error(e.localizedMessage ?: "Delete failed")
            }
        }
    }

    // Fetch a specific game by its ID
    fun fetchGameById(gameId: Long) {
        viewModelScope.launch {
            _selectedGame.value = GameUiState.Loading
            try {
                val game = gameRepository.getGameById(gameId)
                if (game != null) {
                    _selectedGame.value = GameUiState.Success(game)
                } else {
                    _selectedGame.value = GameUiState.Error("Game not found")
                }
            } catch (e: Exception) {
                _selectedGame.value = GameUiState.Error(e.localizedMessage ?: "Fetch failed")
            }
        }
    }

    // Start a new game
    fun startGame(players: List<String>, mode: GameMode) {
        val newGame = Game(
            id = 0L,
            players = players,
            score = players.associateWith { 0 },
            timestamp = System.currentTimeMillis(),
            gameMode = mode,
            gameType = GameType.LOCAL_MULTIPLAYER,
            currentPlayerIndex = 0,
            diceValues = List(5) { (1..6).random() },
            lockedDice = emptyList(),
            remainingRolls = 3,
            imageRecognitionMode = false,
            recognizedDiceValues = null,
            isPlayerWinner = ({ playerName -> false }),
            isPlayersTurn = ({ playerName -> false })
        )

        saveGame(newGame)
    }

    // Roll dice
    fun rollDice(): List<Int> {
        return (1..6).map { (1..6).random() }
    }

    // Lock a dice by index
    fun lockDice(index: Int, locked: Boolean) {
        // Assuming diceValues and lockedDice state management exists
    }

    // Calculate score based on scoring category
    fun calculateScore(scoringCategory: String): Int {
        // Implement scoring logic here
        return 0 // Placeholder
    }

    // Switch current player
    fun switchPlayer(currentPlayerIndex: Int, players: List<String>): Int {
        return if (currentPlayerIndex < players.size - 1) {
            currentPlayerIndex + 1
        } else {
            0 // Wrap around to first player if last player
        }
    }

    // Check if game is complete
    fun checkGameEnd(players: List<String>): Boolean {
        return players.size <= 1 // Example logic
    }

    // Sealed classes for state management
    sealed class GamesUiState {
        object Initial : GamesUiState()
        object Loading : GamesUiState()
        data class Success(val games: List<Game>) : GamesUiState()
        data class Error(val message: String) : GamesUiState()
    }

    sealed class GameUiState {
        object Initial : GameUiState()
        object Loading : GameUiState()
        data class Success(val game: Game) : GameUiState()
        data class Error(val message: String) : GameUiState()
    }

    // New functions for filtering games

    // Get games by mode
    fun getGamesByMode(mode: GameMode) {
        viewModelScope.launch {
            _games.value = GamesUiState.Loading
            try {
                gameRepository.getGamesByMode(mode)
                    .catch { e ->
                        _games.value =
                            GamesUiState.Error(e.localizedMessage ?: "Error fetching games by mode")
                    }
                    .collect { gamesList ->
                        _games.value = GamesUiState.Success(gamesList)
                    }
            } catch (e: Exception) {
                _games.value =
                    GamesUiState.Error(e.localizedMessage ?: "Error fetching games by mode")
            }
        }
    }

    // Get games by type
    fun getGamesByType(type: GameType) {
        viewModelScope.launch {
            _games.value = GamesUiState.Loading
            try {
                gameRepository.getGamesByType(type)
                    .catch { e ->
                        _games.value =
                            GamesUiState.Error(e.localizedMessage ?: "Error fetching games by type")
                    }
                    .collect { gamesList ->
                        _games.value = GamesUiState.Success(gamesList)
                    }
            } catch (e: Exception) {
                _games.value =
                    GamesUiState.Error(e.localizedMessage ?: "Error fetching games by type")
            }
        }
    }

    // Get games by player
    fun getGamesByPlayer(playerName: String) {
        viewModelScope.launch {
            _games.value = GamesUiState.Loading
            try {
                gameRepository.getGamesByPlayer(playerName)
                    .catch { e ->
                        _games.value = GamesUiState.Error(
                            e.localizedMessage ?: "Error fetching games by player"
                        )
                    }
                    .collect { gamesList ->
                        _games.value = GamesUiState.Success(gamesList)
                    }
            } catch (e: Exception) {
                _games.value =
                    GamesUiState.Error(e.localizedMessage ?: "Error fetching games by player")
            }
        }
    }

    // Get games by timestamp range
    fun getGamesByTimestampRange(startTime: Long, endTime: Long) {
        viewModelScope.launch {
            _games.value = GamesUiState.Loading
            try {
                gameRepository.getGamesByTimestampRange(startTime, endTime)
                    .catch { e ->
                        _games.value = GamesUiState.Error(
                            e.localizedMessage ?: "Error fetching games by timestamp range"
                        )
                    }
                    .collect { gamesList ->
                        _games.value = GamesUiState.Success(gamesList)
                    }
            } catch (e: Exception) {
                _games.value = GamesUiState.Error(
                    e.localizedMessage ?: "Error fetching games by timestamp range"
                )
            }
        }
    }
}

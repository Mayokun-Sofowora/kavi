package com.mayor.kavi.ui.viewmodel

import android.content.Context
import android.os.*
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.mayor.kavi.data.models.*
import com.mayor.kavi.data.manager.*
import com.mayor.kavi.data.manager.games.*
import com.mayor.kavi.data.repository.UserRepository
import com.mayor.kavi.data.repository.LeaderboardRepository
import com.mayor.kavi.util.*
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Primary ViewModel for managing game state and user interactions across all dice game variants.
 *
 * This ViewModel handles:
 * - Game state management for all variants (Pig, Greed, Balut, Custom)
 * - Dice rolling and management
 * - Score tracking and statistics
 * - AI opponent interactions
 * - User settings and preferences
 * - Device interactions (vibration, shake detection)
 *
 * @property context Application context for system services
 * @property pigGameManager Manager for Pig game variant
 * @property greedGameManager Manager for Greed game variant
 * @property balutGameManager Manager for Balut game variant
 * @property myGameManager Manager for custom game variants
 * @property statisticsManager Tracks game statistics and player behavior
 * @property networkConnection Monitors network connectivity
 * @property shakeDetectionManager Handles shake-to-roll functionality
 * @property diceManager Manages dice state and animations
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pigGameManager: PigGameManager,
    private val greedGameManager: GreedGameManager,
    private val balutGameManager: BalutGameManager,
    private val myGameManager: MyGameManager,
    private val statisticsManager: StatisticsManager,
    private val networkConnection: NetworkConnection,
    private val shakeDetectionManager: ShakeDetectionManager,
    private val diceManager: DiceManager,
    private val userRepository: UserRepository,
    private val leaderboardRepository: LeaderboardRepository
) : ViewModel() {
    companion object {
        /** Identifier for the AI opponent */
        const val AI_PLAYER_ID = "AI_OPPONENT"
    }

    private val diceDataStore = DataStoreManager.getInstance(context)

    // Core States
    private val _gameState = MutableStateFlow<GameScoreState>(GameScoreState.PigScoreState())

    /** Current game state, varies by game variant */
    val gameState: StateFlow<GameScoreState> = _gameState.asStateFlow()
    private val _selectedBoard = MutableStateFlow("")

    /** Currently selected game board/variant */
    val selectedBoard: StateFlow<String> = _selectedBoard
    private val _heldDice = MutableStateFlow<Set<Int>>(emptySet())

    // UI States
    private val _showWinDialog = MutableStateFlow(false)

    // Settings & Board Selection
    private val _vibrationEnabled = MutableStateFlow(true)

    /** Whether vibration feedback is enabled */
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _boardColor = MutableStateFlow("")

    // Game States
    private val _isLoading = MutableStateFlow(false)
    private val _isNetworkConnected = MutableStateFlow(false)
    private val shakeFlow = MutableSharedFlow<Unit>()

    // Delegate to DiceManager
    val diceImages = diceManager.diceImages
    val isRolling = diceManager.isRolling
    val heldDice = diceManager.heldDice

    init {
        viewModelScope.launch {
            launch {
                networkConnection.isConnected.collect { isConnected ->
                    _isNetworkConnected.value = isConnected
                }
            }
            launch {
                diceDataStore.getVibrationEnabled()
                    .collect { enabled -> _vibrationEnabled.value = enabled }
            }
        }
        setSelectedBoard(GameBoard.PIG.modeName)
        getBoardColor()
        resetGame()
    }

    /**
     * Sets the number of dice for custom game variants.
     *
     * Updates both the game state and dice manager configuration.
     *
     * @param count Number of dice to use (will be constrained to valid range)
     */
    fun setDiceCount(count: Int) {
        val customState = (_gameState.value as? GameScoreState.CustomScoreState) ?: return
        _gameState.value = myGameManager.setDiceCount(customState, count)
        diceManager.setDiceCount(count)
    }

    fun addPlayer() {
        val customState = (_gameState.value as? GameScoreState.CustomScoreState) ?: return
        _gameState.value = myGameManager.addPlayer(customState)
    }

    fun updatePlayerName(playerIndex: Int, name: String) {
        val customState = (_gameState.value as? GameScoreState.CustomScoreState) ?: return
        _gameState.value = myGameManager.updatePlayerName(customState, playerIndex, name)
    }

    fun addScore(playerIndex: Int, score: Int) {
        val customState = (_gameState.value as? GameScoreState.CustomScoreState) ?: return
        _gameState.value = myGameManager.addScore(customState, playerIndex, score)
    }

    fun addNote(playerIndex: Int, note: String) {
        val customState = (_gameState.value as? GameScoreState.CustomScoreState) ?: return
        _gameState.value = myGameManager.addNote(customState, playerIndex, note)
    }

    /**
     * Processes a dice roll action.
     *
     * This method:
     * 1. Validates if rolling is allowed
     * 2. Triggers dice animation
     * 3. Provides haptic feedback if enabled
     * 4. Updates game state based on results
     */
    fun rollDice() {
        if (isRolling.value || !isRollAllowed.value) return
        viewModelScope.launch {
            _isLoading.value = true
                    val results = diceManager.rollDiceForBoard(_selectedBoard.value)
                    // Only provide haptic feedback if vibration is enabled
                    if (vibrationEnabled.value) {
                        provideHapticFeedback()
                    }
                    // Process game state after rolling
                    val newState = processGameState(results)
                    _gameState.value = newState
                }

    }

    /**
     * Toggles whether a specific die is held.
     *
     * Used in games where players can choose which dice to keep
     * between rolls (Greed, Balut).
     *
     * @param index Index of the die to toggle
     */
    fun toggleDiceHold(index: Int) {
        diceManager.toggleHold(index, _selectedBoard.value)
    }

    /**
     * Determines if the AI should bank its current score.
     *
     * Decision is based on:
     * - Current turn score
     * - AI's total score
     * - Player's total score
     * - Game variant specific rules
     *
     * @param currentTurnScore Points accumulated in current turn
     * @param aiTotalScore AI's total score
     * @param playerTotalScore Player's total score
     * @return true if AI should bank, false to continue rolling
     */
    fun shouldAIBank(currentTurnScore: Int, aiTotalScore: Int, playerTotalScore: Int): Boolean {
        return statisticsManager.shouldAIBank(
            currentTurnScore = currentTurnScore,
            aiTotalScore = aiTotalScore,
            playerTotalScore = playerTotalScore
        )
    }

    fun setGameName(name: String) {
        val customState = (_gameState.value as? GameScoreState.CustomScoreState) ?: return
        _gameState.value = myGameManager.setGameName(customState, name)
    }

    fun endPigTurn() {
        val pigState = (_gameState.value as? GameScoreState.PigScoreState) ?: return
        if (pigState.isGameOver) {
            val playerScore = pigState.playerScores[0] ?: 0
            val aiScore = pigState.playerScores[AI_PLAYER_ID.hashCode()] ?: 0
            handleGameEnd(playerScore, playerScore > aiScore)
        }
        viewModelScope.launch {
            val currentState = _gameState.value as? GameScoreState.PigScoreState ?: return@launch
            val newState = pigGameManager.bankScore(currentState)
            _gameState.value = newState
            if (newState.isGameOver) {
                _showWinDialog.value = true
                val playerScore = newState.playerScores[0] ?: 0
                val aiScore = newState.playerScores[AI_PLAYER_ID.hashCode()] ?: 0
                handleGameEnd(playerScore, playerScore > aiScore)
            }
        }
    }

    fun endGreedTurn() {
        val greedState = (_gameState.value as? GameScoreState.GreedScoreState) ?: return
        if (greedState.isGameOver) {
            val playerScore = greedState.playerScores[0] ?: 0
            val aiScore = greedState.playerScores[AI_PLAYER_ID.hashCode()] ?: 0
            handleGameEnd(playerScore, playerScore > aiScore)
        }
        val currentState = (_gameState.value as? GameScoreState.GreedScoreState) ?: return
        val newState = greedGameManager.bankScore(currentState)
        _gameState.value = newState
        if (newState.isGameOver) {
                _showWinDialog.value = true
            val playerScore = newState.playerScores[0] ?: 0
            val aiScore = newState.playerScores[AI_PLAYER_ID.hashCode()] ?: 0
            handleGameEnd(playerScore, playerScore > aiScore)
        }
    }

    fun endBalutTurn(category: String) {
        val balutState = (_gameState.value as? GameScoreState.BalutScoreState) ?: return
        if (balutState.isGameOver) {
            val playerScore = balutState.playerScores[0]?.values?.sum() ?: 0
            val aiScore = balutState.playerScores[AI_PLAYER_ID.hashCode()]?.values?.sum() ?: 0
            handleGameEnd(playerScore, playerScore > aiScore)
        }
        val currentState = (_gameState.value as? GameScoreState.BalutScoreState) ?: return
        val dice = getCurrentRolls()
        val newState = balutGameManager.scoreCategory(currentState, dice, category)
        
        _gameState.value = newState
        if (newState.isGameOver) {
            _showWinDialog.value = true
            val playerScore = newState.playerScores[0]?.values?.sum() ?: 0
            val aiScore = newState.playerScores[AI_PLAYER_ID.hashCode()]?.values?.sum() ?: 0
            handleGameEnd(playerScore, playerScore > aiScore)
        }
    }

    fun resetHeldDice() {
        diceManager.resetHeldDice()
    }

    fun resetScores() {
        val customState = (_gameState.value as? GameScoreState.CustomScoreState) ?: return
        _gameState.value = myGameManager.resetScores(customState)
    }

    fun resetGame() {
        viewModelScope.launch {
            _gameState.value = when (GameBoard.valueOf(_selectedBoard.value.uppercase())) {
                GameBoard.PIG -> pigGameManager.initializeGame()
                GameBoard.GREED -> greedGameManager.initializeGame()
                GameBoard.BALUT -> balutGameManager.initializeGame()
                GameBoard.CUSTOM -> myGameManager.initializeGame()
            }
            diceManager.resetGame()
            _showWinDialog.value = false
            _heldDice.value = emptySet()
        }
    }

    /**
     * Manages shake detection for dice rolling.
     *
     * Enables shake-to-roll functionality when:
     * - Rolling is currently allowed
     * - No roll is in progress
     */
    @OptIn(FlowPreview::class)
    fun resumeShakeDetection() {
        shakeDetectionManager.setOnShakeListener {
            viewModelScope.launch {
                shakeFlow.emit(Unit)
            }
        }
        shakeDetectionManager.startListening()

        viewModelScope.launch {
            shakeFlow
                .debounce(300) // Debounce for 300ms
                .collect {
                    if (!isRolling.value && isRollAllowed.value) {
                        rollDice()
                    }
                }
        }
    }

    fun pauseShakeDetection() {
        shakeDetectionManager.clearOnShakeListener()
        shakeDetectionManager.stopListening()
    }

    fun setVibrationEnabled(enabled: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        diceDataStore.setVibrationEnabled(enabled)
    }

    fun setSelectedBoard(board: String) {
        _selectedBoard.value = board
        resetGame()
    }

    fun chooseAICategory(diceResults: List<Int>): String {
        val currentState = (_gameState.value as? GameScoreState.BalutScoreState) ?: run {
            Timber.e("Invalid game state for AI category selection")
            return balutGameManager.initializeGame().let {
                balutGameManager.chooseAICategory(diceResults, it)
            }
        }
        return balutGameManager.chooseAICategory(diceResults, currentState)
    }

    fun getCurrentRolls(): List<Int> {
        return diceManager.currentRolls.value
    }

    private fun getBoardColor() {
        viewModelScope.launch {
            diceDataStore.getBoardColor().collect {
                _boardColor.value = it
            }
        }
    }

    private fun provideHapticFeedback() {
        if (!_vibrationEnabled.value) return

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    val isRollAllowed: StateFlow<Boolean> = combine(
        _gameState,
        _selectedBoard,
        diceManager.isRolling
    ) { gameState, selectedBoard, isRolling ->
        when {
            isRolling -> false
            else -> true
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        true
    )

    private fun updateGameStatistics(score: Int, isWin: Boolean) {
        viewModelScope.launch {
            val gameMode = when (_gameState.value) {
                is GameScoreState.PigScoreState -> GameBoard.PIG.modeName
                is GameScoreState.GreedScoreState -> GameBoard.GREED.modeName
                is GameScoreState.BalutScoreState -> GameBoard.BALUT.modeName
                is GameScoreState.CustomScoreState -> GameBoard.CUSTOM.modeName
                else -> return@launch
            }
            statisticsManager.updateGameStatistics(gameMode, score, isWin)
        }
    }

    private fun handleGameEnd(finalScore: Int, isWin: Boolean) {
        updateGameStatistics(finalScore, isWin)
        viewModelScope.launch {
            val currentUserId = userRepository.getCurrentUserId()?.toString() ?: run {
                Timber.e("No current user ID available")
                return@launch
            }

            // Get current user profile
            val currentUser = when (val result = userRepository.getCurrentUser()) {
                is Result.Success -> result.data
                else -> {
                    Timber.e("Failed to get current user profile")
                    return@launch
                }
            }

            // Update leaderboard entry
            when (_gameState.value) {
                is GameScoreState.PigScoreState, 
                is GameScoreState.GreedScoreState,
                is GameScoreState.BalutScoreState -> {
                    try {
                        val entry = LeaderboardEntry(
                            userId = currentUserId,
                            displayName = currentUser.name,
                            score = finalScore,
                            gamesPlayed = 1,
                            gamesWon = if (isWin) 1 else 0,
                            lastUpdated = System.currentTimeMillis()
                        )
                        leaderboardRepository.updateLeaderboardEntry(entry)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to update leaderboard: ${e.message}")
                    }
                }
                is GameScoreState.CustomScoreState -> {
                    // Custom games are not tracked in leaderboards
                }
            }
        }
    }

    private suspend fun processGameState(results: List<Int>): GameScoreState {
        return when (val currentState = _gameState.value) {
            is GameScoreState.PigScoreState -> withContext(Dispatchers.Default) {
                pigGameManager.handleTurn(currentState, results.firstOrNull())
            }

            is GameScoreState.GreedScoreState -> withContext(Dispatchers.Default) {
                greedGameManager.handleTurn(results, currentState, _heldDice.value)
            }

            is GameScoreState.BalutScoreState -> withContext(Dispatchers.Default) {
                balutGameManager.handleTurn(results, currentState, _heldDice.value)
            }

            is GameScoreState.CustomScoreState -> withContext(Dispatchers.Default) {
                myGameManager.handleTurn(currentState, results)
            }

            else -> throw IllegalArgumentException("Unsupported Game State")
        }
    }
}

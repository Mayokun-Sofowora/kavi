package com.mayor.kavi.ui.viewmodel

import android.content.Context
import android.os.*
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import com.mayor.kavi.util.Result.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import com.mayor.kavi.data.models.*
import com.mayor.kavi.data.manager.*
import com.mayor.kavi.data.manager.games.*
import com.mayor.kavi.data.repository.*
import com.mayor.kavi.util.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,
    private val pigGameManager: PigGameManager,
    private val greedGameManager: GreedGameManager,
    private val balutGameManager: BalutGameManager,
    private val myGameManager: MyGameManager,
    private val gameSessionManager: GameSessionManager,
    private val statisticsManager: StatisticsManager,
    private val networkConnection: NetworkConnection,
    private val shakeDetectionManager: ShakeDetectionManager,
    private val diceManager: DiceManager
) : ViewModel() {
    companion object {
        const val AI_PLAYER_ID = "AI_OPPONENT"
    }

    private val diceDataStore = DataStoreManager.getInstance(context)
    private val gameSessionMutex = Mutex()
//    private val gameStatisticsMutex = Mutex()
//    private val gameStateMutex = Mutex()

    // Core States
    private val _gameState = MutableStateFlow<GameScoreState>(GameScoreState.PigScoreState())
    val gameState: StateFlow<GameScoreState> = _gameState.asStateFlow()
    private val _selectedBoard = MutableStateFlow("")
    val selectedBoard: StateFlow<String> = _selectedBoard
    private val _playMode = MutableStateFlow<PlayMode>(PlayMode.SinglePlayer)
    val playMode: StateFlow<PlayMode> = _playMode
    private val _heldDice = MutableStateFlow<Set<Int>>(emptySet())

    // UI States
    private val _showWinDialog = MutableStateFlow(false)
    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent

    // Settings & Board Selection
    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _boardColor = MutableStateFlow("")

    // Game Session
    private val _gameSession = MutableStateFlow(GameSession())
    val gameSession: StateFlow<GameSession> = _gameSession.asStateFlow()

    // Statistics and Profile
//    private val _gameStatistics = MutableStateFlow(GameStatistics(0, emptyMap(), emptyMap()))

    // Game States
    private val _isMyTurn = MutableStateFlow(false)
    val isMyTurn: StateFlow<Boolean> = _isMyTurn.asStateFlow()
    private val _playerInfo = MutableStateFlow<List<PlayerInfoData>>(emptyList())
    val playerInfo: StateFlow<List<PlayerInfoData>> = _playerInfo.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow("")
    private val _onlinePlayers = MutableStateFlow<List<UserProfile>>(emptyList())
    val onlinePlayers: StateFlow<List<UserProfile>> = _onlinePlayers.asStateFlow()
    private val _isNetworkConnected = MutableStateFlow(false)
    private val shakeFlow = MutableSharedFlow<Unit>()

    // Delegate to DiceManager
    val diceImages = diceManager.diceImages
    val isRolling = diceManager.isRolling
    val heldDice = diceManager.heldDice

    private val _challengeNotification =
        MutableStateFlow<GameSession?>(null) // consider using challenge notification

    private var onlinePlayersJob: Job? = null

    init {
        viewModelScope.launch {
            // Collect game session updates
            gameSessionManager.gameSessionUpdates.collect { session ->
                _gameSession.value = session
                handleGameSessionUpdate(session)
            }

            launch {
                networkConnection.isConnected.collect { isConnected ->
                    _isNetworkConnected.value = isConnected
                }
            }

            launch {
                diceDataStore.getVibrationEnabled()
                    .collect { enabled -> _vibrationEnabled.value = enabled }
            }

            launch {
                gameRepository.listenForGameInvites().collect { session ->
                    _challengeNotification.value = session
                }
            }

            launch(Dispatchers.IO) {
                if (userRepository.getCurrentUserId() != null) {
                    userRepository.setUserOnlineStatus(true)
                }
            }
        }

        setSelectedBoard(GameBoard.PIG.modeName)
        getOnlinePlayers()
        getBoardColor()
        setInitialTurn()
        resetGame()
    }

    sealed class NavigationEvent {
        data class NavigateToBoard(val sessionId: String) : NavigationEvent()
        object NavigateBack : NavigationEvent()
        object ShowExitDialog : NavigationEvent()
    }

    fun getCurrentUserId(): String? {
        return userRepository.getCurrentUserId()
    }

    fun createGameSession(opponentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = gameRepository.createGameSession(opponentId)) {
                is Success -> {
                    val session = result.data
                    _gameSession.value = session
                    setPlayMode(PlayMode.Multiplayer, session)
                    userRepository.setUserGameStatus(
                        isInGame = false,
                        isWaitingForPlayers = true,
                        gameId = session.id
                    )
                    navigateToBoard(session.id)
                }

                is Error -> {
                    Timber.e(result.exception, result.message)
                    _errorMessage.value = result.message
                }

                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun joinGameSession(sessionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = gameRepository.joinGameSession(sessionId)) {
                is Success -> {
                    val session = result.data
                    _gameSession.value = session
                    setPlayMode(PlayMode.Multiplayer, session)
                    setupMultiplayerSession()
                    userRepository.setUserGameStatus(
                        isInGame = false,
                        isWaitingForPlayers = true,
                        gameId = session.id
                    )
                    // Only navigate to board if both players are ready
                    if (session.players.all { it.isReady }) {
                        userRepository.setUserGameStatus(
                            isInGame = true,
                            isWaitingForPlayers = false,
                            gameId = session.id
                        )
                        navigateToBoard(session.id)
                    }
                }

                is Error -> {
                    Timber.e(result.exception, result.message)
                    _errorMessage.value = result.message
                }

                else -> {}
            }
            _isLoading.value = false
        }
    }

    fun endGameSession() {
        viewModelScope.launch {
            val sessionId = gameRepository.getSessionId()
            if (sessionId != null) {
                _isLoading.value = true
                val result = gameRepository.endSession(sessionId)
                when (result) {
                    is Success -> {
                        resetGame()
                        _gameSession.value = GameSession()
                        _navigationEvent.value = NavigationEvent.NavigateBack
                        userRepository.setUserGameStatus(
                            isInGame = false,
                            isWaitingForPlayers = false,
                            gameId = ""
                        )
                    }

                    is Error -> {
                        Timber.d(result.exception, result.message)
                        _errorMessage.value = result.message
                    }

                    else -> {
                        Timber.d("Unknown error occurred")
                        _errorMessage.value = "Unknown error occurred"
                    }
                }
                _isLoading.value = false
            } else {
                _errorMessage.value = "No Active Session Found!"
            }
        }
    }

    fun getOnlinePlayers() {
        viewModelScope.launch {
            gameRepository.getOnlinePlayers().collect { players ->
                _onlinePlayers.value = players
            }
        }
    }

    fun setInitialTurn() {
        _isMyTurn.value = gameSession.value.currentTurn == userRepository.getCurrentUserId()
    }

    fun setDiceCount(count: Int) {
        val customState = (_gameState.value as? GameScoreState.CustomScoreState) ?: return
        _gameState.value = myGameManager.setDiceCount(customState, count)
        // Update dice manager count
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

    fun rollDice() {
        if (isRolling.value || !isRollAllowed.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (_playMode.value == PlayMode.Multiplayer && _gameSession.value.id.isNotEmpty()) {
                    val results = diceManager.rollDiceForBoard(_selectedBoard.value)
                    // Update game session with roll results
                    val sessionId = _gameSession.value.id
                    val turnData = TurnData(
                        playerID = getCurrentUserId() ?: "",
                        diceRoll = results,
                        heldDice = heldDice.value.toList()
                    )
                    val updates = mapOf(
                        "gameState.turnData" to (_gameSession.value.gameState.turnData + turnData),
                        "gameState.lastUpdate" to System.currentTimeMillis()
                    )
                    gameRepository.getGameSessionRef(sessionId)
                        .update(updates)
                        .await()

                    // Process game state after rolling
                    val newState = processGameState(results)
                    _gameState.value = newState
                } else {
                    // Reset any lingering multiplayer session
                    if (_gameSession.value.id.isNotEmpty()) {
                        resetGameSession()
                    }
                    val results = diceManager.rollDiceForBoard(_selectedBoard.value)
                    // Only provide haptic feedback if vibration is enabled
                    if (vibrationEnabled.value) {
                        provideHapticFeedback()
                    }
                    // Process game state after rolling
                    val newState = processGameState(results)
                    _gameState.value = newState
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to roll dice: ${e.message}"
                Timber.e(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleDiceHold(index: Int) {
        diceManager.toggleHold(index, _selectedBoard.value)
    }

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

    fun navigateToBoard(sessionId: String) {
        _navigationEvent.value = NavigationEvent.NavigateToBoard(sessionId)
    }

    @OptIn(FlowPreview::class)
    fun resumeShakeDetection() {
        if (!_vibrationEnabled.value) return
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
                    if (!isRolling.value) {
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

    fun setPlayMode(playMode: PlayMode, gameSession: GameSession? = null) {
        // Only allow multiplayer for Greed game
        val finalPlayMode = when (_selectedBoard.value) {
            GameBoard.GREED.modeName -> playMode
            else -> PlayMode.SinglePlayer
        }

        _playMode.value = finalPlayMode
        if (gameSession != null && finalPlayMode == PlayMode.Multiplayer) {
            _gameSession.value = gameSession
            setupMultiplayerSession()
        } else {
            // Initialize AI opponent for single player games
            when (_selectedBoard.value) {
                GameBoard.PIG.modeName,
                GameBoard.GREED.modeName,
                GameBoard.BALUT.modeName,
                GameBoard.CUSTOM.modeName -> {
                    _gameState.value = when (val state = _gameState.value) {
                        is GameScoreState.PigScoreState -> state.copy(
                            playerCount = 2,
                            playerScores = mapOf(
                                getCurrentUserId().hashCode() to 0,
                                AI_PLAYER_ID.hashCode() to 0
                            )
                        )

                        is GameScoreState.GreedScoreState -> state.copy(
                            playerCount = 2,
                            playerScores = mapOf(
                                getCurrentUserId().hashCode() to 0,
                                AI_PLAYER_ID.hashCode() to 0
                            )
                        )

                        is GameScoreState.BalutScoreState -> state.copy(
                            playerCount = 2,
                            playerScores = mapOf(
                                getCurrentUserId().hashCode() to mutableMapOf<String, Int>(),
                                AI_PLAYER_ID.hashCode() to mutableMapOf<String, Int>()
                            )
                        )

                        is GameScoreState.CustomScoreState -> state.copy(
                            playerCount = MyGameManager.MIN_PLAYERS,
                            playerScores = (0 until MyGameManager.MIN_PLAYERS).associateWith { 0 },
                            playerNames = (0 until MyGameManager.MIN_PLAYERS).associateWith { "Player ${it + 1}" },
                            scoreHistory = (0 until MyGameManager.MIN_PLAYERS).associateWith { emptyList() }
                        )

                        else -> state
                    }
                }
            }
        }
        resetGame()
    }

    private fun setupMultiplayerSession() {
        viewModelScope.launch {
            if (_gameSession.value.id.isNotEmpty()) {
                gameRepository.listenToGameUpdates(_gameSession.value.id)
                    .collect { updatedSession ->
                        gameSessionMutex.withLock {
                            _gameSession.value = updatedSession
                            updatePlayerInfo(updatedSession)
                            setInitialTurn()

                            // Update game status when all players are ready
                            if (updatedSession.gameState.status == "active" &&
                                updatedSession.players.all { it.isReady }
                            ) {
                                userRepository.setUserGameStatus(
                                    isInGame = true,
                                    isWaitingForPlayers = false,
                                    gameId = updatedSession.id
                                )
                                _gameState.value = sessionToGameScoreState(updatedSession)
                                navigateToBoard(updatedSession.id)
                            }
                        }
                    }
            }
        }
    }

    private fun sessionToGameScoreState(session: GameSession): GameScoreState {
        val players = session.players.map { it.hashCode() }
        val scores = session.scores.mapKeys { it.key.hashCode() }

        return when (session.gameMode) {
            GameBoard.PIG.modeName -> GameScoreState.PigScoreState(
                playerScores = scores,
                playerCount = players.size
            )

            GameBoard.GREED.modeName ->
                GameScoreState.GreedScoreState(
                    playerScores = scores,
                    playerCount = players.size
                )


            GameBoard.BALUT.modeName -> {
                val balutScores = players.associate { playerId ->
                    playerId to (scores[playerId]?.let { mutableMapOf("Total" to it) }
                        ?: mutableMapOf())
                }
                GameScoreState.BalutScoreState(
                    playerScores = balutScores,
                    playerCount = players.size
                )
            }

            GameBoard.CUSTOM.modeName -> GameScoreState.CustomScoreState(
                playerScores = scores,
                playerCount = players.size,
                diceCount = MyGameManager.DEFAULT_DICE,
                scoreHistory = players.associateWith { emptyList() }
            )

            else -> throw IllegalArgumentException("Unsupported game mode: ${session.gameMode}")
        }
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

    private suspend fun handleGameSessionUpdate(session: GameSession) {
        gameSessionMutex.withLock {
            try {
                // Update game state
                val gameState = when (session.gameMode) {
                    GameBoard.GREED.modeName -> {
                        // Get the last roll from the most recent turn data
                        val lastRoll =
                            session.gameState.turnData.lastOrNull()?.diceRoll ?: emptyList()

                        GameScoreState.GreedScoreState(
                            playerScores = session.gameState.scores.mapKeys { it.key.hashCode() },
                            currentPlayerIndex = session.gameState.currentPlayerId.hashCode(),
                            turnScore = 0,
                            message = "",
                            lastRoll = lastRoll,
                            isGameOver = session.gameState.status == "completed"
                        )
                    }

                    else -> throw IllegalArgumentException("Unsupported game mode: ${session.gameMode}")
                }
                _gameState.value = gameState

                // Update player info
                updatePlayerInfo(session)

                // Update turn status
                _isMyTurn.value = session.gameState.currentPlayerId == getCurrentUserId()

                // Handle game completion
                if (session.gameState.status == "completed") {
                    _showWinDialog.value = true
                    val playerScore = session.gameState.scores[getCurrentUserId()] ?: 0
                    val isWinner = session.gameState.scores.all { (id, score) ->
                        id == getCurrentUserId() || score <= playerScore
                    }
                    handleGameEnd(playerScore, isWinner)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error handling game session update")
                _errorMessage.value = "Error updating game state: ${e.message}"
            }
        }
    }

    private suspend fun updatePlayerInfo(session: GameSession) {
        val players = session.players.map { player ->
            val userProfile = userRepository.getUserById(player.id).dataOrNull
            userProfile?.toPlayerInfoData(
                score = session.scores[player.id] ?: 0,
                isCurrentTurn = session.gameState.currentPlayerId == player.id
            ) ?: PlayerInfoData(
                id = player.id,
                name = userProfile?.name ?: "Unknown Player",
                score = session.scores[player.id] ?: 0,
                isCurrentTurn = session.gameState.currentPlayerId == player.id,
                isReady = player.isReady
            )
        }
        _playerInfo.value = players
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

    fun UserProfile.toPlayerInfoData(score: Int = 0, isCurrentTurn: Boolean = false) =
        PlayerInfoData(
            id = id,
            name = name,
            score = score,
            isCurrentTurn = isCurrentTurn,
            isReady = isWaitingForPlayers
        )

    fun chooseAICategory(diceResults: List<Int>): String {
        val currentState = (_gameState.value as? GameScoreState.BalutScoreState) ?: return "Choice"
        return balutGameManager.chooseAICategory(diceResults, currentState)
    }

    fun getCurrentRolls(): List<Int> {
        return diceManager.currentRolls.value
    }

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
    }

    fun setUserOnlineStatus(isOnline: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setUserOnlineStatus(isOnline)
        }
    }

    fun startListeningForOnlinePlayers() {
        onlinePlayersJob?.cancel() // Cancel any existing job
        onlinePlayersJob = viewModelScope.launch {
            userRepository.listenForOnlinePlayers()
                .collect { players ->
                    _onlinePlayers.value = players.filter { it.id != getCurrentUserId() }
                }
        }
    }

    fun stopListeningForOnlinePlayers() {
        onlinePlayersJob?.cancel()
        onlinePlayersJob = null
        _onlinePlayers.value = emptyList()
    }

    fun resetGameSession() {
        viewModelScope.launch {
            val currentSession = _gameSession.value
            if (currentSession.id.isNotEmpty()) {
                try {
                    // Update user status
                    userRepository.setUserGameStatus(
                        isInGame = false,
                        isWaitingForPlayers = false,
                        gameId = ""
                    )
                    // Reset session state
                    _gameSession.value = GameSession()
                    _playerInfo.value = emptyList()
                    _isMyTurn.value = false
                    // Reset game mode if needed
                    if (_playMode.value == PlayMode.Multiplayer) {
                        _playMode.value = PlayMode.SinglePlayer
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to reset game session")
                }
            }
        }
    }

    fun onBackPressed() {
        _navigationEvent.value = null // Clear previous event
        _navigationEvent.value = if (_gameSession.value.id.isNotEmpty()) {
            NavigationEvent.ShowExitDialog
        } else {
            NavigationEvent.NavigateBack
        }
    }

    fun startMultiplayerGame(sessionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Update game state to active
                val updates = mapOf(
                    "gameState.status" to "active",
                    "isGameStarted" to true
                )

                gameRepository.getGameSessionRef(sessionId)
                    .update(updates)
                    .await()

                // Update user status
                userRepository.setUserGameStatus(
                    isInGame = true,
                    isWaitingForPlayers = false,
                    gameId = sessionId
                )

                // Navigate to game board
                navigateToBoard(sessionId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to start multiplayer game")
                _errorMessage.value = "Failed to start game: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

}

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
    private val gameStatisticsMutex = Mutex()
    private val gameStateMutex = Mutex()

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
    private val _gameStatistics = MutableStateFlow(GameStatistics(0, emptyMap(), emptyMap()))

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
    private val _isReady = MutableStateFlow(false)
    private val shakeFlow = MutableSharedFlow<Unit>()

    // Delegate to DiceManager
    val diceImages = diceManager.diceImages
    val isRolling = diceManager.isRolling
    val heldDice = diceManager.heldDice

    private val _challengeNotification = MutableStateFlow<GameSession?>(null)
    val challengeNotification: StateFlow<GameSession?> = _challengeNotification.asStateFlow()

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

    fun getOnlinePlayers() {
        viewModelScope.launch {
            gameRepository.getOnlinePlayers().collect { players ->
                _onlinePlayers.value = players
            }
        }
    }

    fun setPlayerReady(sessionId: String, ready: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = gameRepository.setPlayerReady(sessionId, ready)) {
                is Success -> {
                    _isReady.value = ready
                    // If all players are ready, start the game
                    val session = gameRepository.getGameSession(sessionId).dataOrNull
                    if (session?.players?.all { it.isReady } == true) {
                        setupMultiplayerSession()
                    }
                }
                is Error -> {
                    Timber.e(result.exception, "Failed to set player ready status")
                }
                else -> {}
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
                if (_playMode.value == PlayMode.Multiplayer) {
//                    handleMultiplayerRoll(_gameState.value as GameScoreState.GreedScoreState)
                } else {
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
            diceManager.resetGame()
            if (newState.isGameOver) {
                _showWinDialog.value = true
                recordGameStatistics(newState)
            }
        }
    }

    fun endGreedTurn() {
        viewModelScope.launch {
            val currentState = _gameState.value as? GameScoreState.GreedScoreState ?: return@launch
            val updatedState = greedGameManager.bankScore(currentState)
            _gameState.value = updatedState
            if (_playMode.value == PlayMode.Multiplayer) {
                val totalScore =
                    updatedState.playerScores[userRepository.getCurrentUserId().hashCode()] ?: 0
                gameSessionManager.handleGameAction(
                    gameSession.value,
                    GameSessionManager.GameAction.BankScore(
                        totalScore = totalScore,
                        GameBoard.GREED.modeName
                    )
                )
            }
            _heldDice.value = emptySet()
            diceManager.resetGame()
            if (updatedState.isGameOver) {
                _showWinDialog.value = true
                recordGameStatistics(updatedState)
            }
        }
    }

    fun endBalutTurn(category: String) {
        val currentState = (_gameState.value as? GameScoreState.BalutScoreState) ?: return
        val dice = getCurrentRolls()
        val newState = balutGameManager.scoreCategory(currentState, dice, category)
        
        _gameState.value = newState
        if (newState.isGameOver) {
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
                                updatedSession.players.all { it.isReady }) {
                                userRepository.setUserGameStatus(
                                    isInGame = true,
                                    isWaitingForPlayers = false,
                                    gameId = updatedSession.id
                                )
                                _gameState.value = sessionToGameScoreState(updatedSession)
                                if (updatedSession.isGameStarted) {
                                    navigateToBoard(updatedSession.id)
                                }
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

            GameBoard.GREED.modeName -> GameScoreState.GreedScoreState(
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

    private fun recordGameStatistics(finalState: GameScoreState) {
        viewModelScope.launch {
            gameStatisticsMutex.withLock {
                val currentUserId = userRepository.getCurrentUserId() ?: return@launch
                val (gameMode, finalScore, isWin) = when (finalState) {
                    is GameScoreState.PigScoreState -> Triple(
                        GameBoard.PIG.modeName,
                        finalState.playerScores[currentUserId.hashCode()] ?: 0,
                        (finalState.playerScores[currentUserId.hashCode()] ?: 0) >= 100
                    )

                    is GameScoreState.GreedScoreState -> Triple(
                        GameBoard.GREED.modeName,
                        finalState.playerScores[currentUserId.hashCode()] ?: 0,
                        (finalState.playerScores[currentUserId.hashCode()] ?: 0) >= 10000
                    )

                    is GameScoreState.BalutScoreState -> Triple(
                        GameBoard.BALUT.modeName,
                        finalState.playerScores[currentUserId.hashCode()]?.values?.sum() ?: 0,
                        (finalState.playerScores[currentUserId.hashCode()]?.values?.sum()
                            ?: 0) >= 1000
                    )

                    is GameScoreState.CustomScoreState -> Triple(
                        GameBoard.CUSTOM.modeName,
                        finalState.playerScores[currentUserId.hashCode()] ?: 0,
                        (finalState.playerScores[currentUserId.hashCode()] ?: 0) >= 2000
                    )

                    else -> return@launch
                }
                _gameStatistics.value = _gameStatistics.value.copy(
                    gamesPlayed = _gameStatistics.value.gamesPlayed + 1,
                    highScores = _gameStatistics.value.highScores + (gameMode to finalScore),
                    winRates = _gameStatistics.value.winRates + (gameMode to WinRate(
                        wins = if (isWin) 1 else 0,
                        total = 1
                    ))
                )
            }
        }
    }

    private fun getCurrentTurnScore(state: GameScoreState) = when (state) {
        is GameScoreState.PigScoreState -> state.currentTurnScore
        is GameScoreState.GreedScoreState -> state.turnScore
        else -> 0
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

    private suspend fun handleMultiplayerState(results: List<Int>): GameScoreState {
        return when (val session = _gameSession.value) {
            GameSession() -> throw IllegalStateException("No active game session")
            else -> {
                val gameAction = GameSessionManager.GameAction.Roll(
                    diceResults = results,
                    heldDice = _heldDice.value,
                    turnScore = getCurrentTurnScore(_gameState.value),
                    gameMode = session.gameMode ?: GameBoard.GREED.modeName
                )
                gameSessionManager.handleGameAction(session, gameAction)
                // Update game state based on session
                sessionToGameScoreState(session)
            }
        }
    }

    private suspend fun processGameState(results: List<Int>): GameScoreState {
        return gameStateMutex.withLock {
            when (val currentState = _gameState.value) {
                is GameScoreState.PigScoreState -> withContext(Dispatchers.Default) {
                    pigGameManager.handleTurn(currentState, results.first())
                }

                is GameScoreState.GreedScoreState -> {
                    when (_playMode.value) {
                        PlayMode.Multiplayer -> handleMultiplayerState(results)
                        PlayMode.SinglePlayer -> greedGameManager.handleTurn(results, currentState)
                    }
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

    private suspend fun handleGameSessionUpdate(session: GameSession) {
        gameSessionMutex.withLock {
            val gameState = try {
                sessionToGameScoreState(session)
            } catch (e: Exception) {
                Timber.e("Error handling game session state. Defaulting state to Pig State: ${e.message}")
                GameScoreState.PigScoreState()
            }
            _gameState.value = gameState
            if (session.gameState.status == "completed") {
                _showWinDialog.value = true
                recordGameStatistics(gameState)
            }
        }
    }

    private suspend fun updatePlayerInfo(session: GameSession) {
        val players = session.players
        if (players.isEmpty()) return

        _playerInfo.value = players.map { player ->
            PlayerInfoData(
                id = player.id,
                name = player.name.ifEmpty { "Player ${player.id.take(4)}" },
                score = session.scores[player.id] ?: 0,
                isCurrentTurn = session.currentTurn == player.id,
                isReady = player.isReady
            )
        }
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

    fun chooseAICategory(diceResults: List<Int>): String {
        val currentState = (_gameState.value as? GameScoreState.BalutScoreState) ?: return "Choice"
        return balutGameManager.chooseAICategory(diceResults, currentState)
    }

    fun getCurrentRolls(): List<Int> {
        return diceManager.currentRolls.value
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.IO) {
            setUserOnlineStatus(false)
        }
        shakeDetectionManager.clearOnShakeListener()
        networkConnection.removeObserver { isConnected ->
            _isNetworkConnected.value = isConnected
        }
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

    // Update this in your game end handlers
    private fun handleGameEnd(finalScore: Int, isWin: Boolean) {
        updateGameStatistics(finalScore, isWin)
    }

    fun setUserOnlineStatus(isOnline: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setUserOnlineStatus(isOnline)
        }
    }

    fun startListeningForOnlinePlayers() {
        viewModelScope.launch {
            userRepository.listenForOnlinePlayers()
                .collect { players ->
                    _onlinePlayers.value = players.filter { it.id != getCurrentUserId() }
                }
        }
    }

    fun stopListeningForOnlinePlayers() {
        _onlinePlayers.value = emptyList()
    }

    fun acceptChallenge(session: GameSession) {
        viewModelScope.launch {
            joinGameSession(session.id)
            _challengeNotification.value = null
        }
    }

    fun declineChallenge(session: GameSession) {
        _challengeNotification.value = null
    }

    fun endCurrentSession() {
        viewModelScope.launch {
            val currentSession = _gameSession.value
            if (currentSession.id.isNotEmpty()) {
                gameRepository.cleanupGameSession(currentSession.id)
                userRepository.setUserGameStatus(
                    isInGame = false,
                    isWaitingForPlayers = false,
                    gameId = ""
                )
                _gameSession.value = GameSession()
                _navigationEvent.value = NavigationEvent.NavigateBack
            }
        }
    }

    fun onBackPressed() {
        if (_gameSession.value.id.isNotEmpty()) {
            _navigationEvent.value = NavigationEvent.ShowExitDialog
        } else {
            _navigationEvent.value = NavigationEvent.NavigateBack
        }
    }

}

// DiceViewModel.kt
package com.mayor.kavi.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.lifecycle.*
import com.airbnb.lottie.compose.*
import dagger.hilt.android.lifecycle.HiltViewModel
import com.mayor.kavi.R
import com.mayor.kavi.data.*
import com.mayor.kavi.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random
import com.mayor.kavi.data.games.*
import com.mayor.kavi.data.manager.*
import dagger.assisted.Assisted

data class ScoreState(
    val currentTurnScore: Int = 0,
    val overallScore: Int = 0,
    val isGameOver: Boolean = false,
    val resultMessage: String = ""
)

@HiltViewModel
class DiceViewModel @Inject constructor(
    application: Application,
    private val statisticsManager: StatisticsManager,
    private val gameRepository: GameRepository,
    @Assisted private val multiplayerViewModel: MultiplayerViewModel,  // Fix injection
    private val shakeDetectionService: ShakeDetectionService,
    val networkConnection: NetworkConnection
) : AndroidViewModel(application) {
    private val diceDataStore = DataStoreManager.Companion.getInstance(application)

    // Settings & Board Selection
    private val _shakeEnabled = diceDataStore.getShakeEnabled().asLiveData()
    val shakeEnabled: LiveData<Boolean> = _shakeEnabled
    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled = diceDataStore.getVibrationEnabled().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )
    private val _selectedBoard = MutableStateFlow("")
    val selectedBoard: StateFlow<String> = _selectedBoard

    // Game States
    private val _scoreState = MutableStateFlow(ScoreState())
    val scoreState: StateFlow<ScoreState> = _scoreState
    private val _mexicoScoreState = MutableStateFlow(MexicoScoreState())
    val mexicoScoreState: StateFlow<MexicoScoreState> = _mexicoScoreState
    private val _chicagoScoreState = MutableStateFlow(ChicagoScoreState())
    val chicagoScoreState: StateFlow<ChicagoScoreState> = _chicagoScoreState
    private val _balutScoreState = MutableStateFlow(BalutScoreState())
    val balutScoreState: StateFlow<BalutScoreState> = _balutScoreState

    // Confetti settings
    private val _showConfetti = MutableStateFlow(false)
    val showConfetti: StateFlow<Boolean> = _showConfetti

    // Dice Display
    private val _diceImages = MutableStateFlow(List(6) { R.drawable.empty_dice })
    val diceImages: StateFlow<List<Int>> = _diceImages
    private val _isRolling = MutableStateFlow(false)
    val isRolling = _isRolling.asStateFlow()

    // Opponent management
    sealed class PlayMode {
        object SinglePlayer : PlayMode()
        data class Multiplayer(val sessionId: String) : PlayMode()
    }

    // Game Modes
    private val _playMode = MutableStateFlow<PlayMode>(PlayMode.SinglePlayer)
    val playMode: StateFlow<PlayMode> = _playMode

    // Statistics and Profile
    private val _gameStats = MutableStateFlow(GameStats(0, emptyMap(), emptyMap()))
    val gameStats: StateFlow<GameStats> = _gameStats
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile
    private val _nearbyPlayers = MutableStateFlow<List<UserProfile>>(emptyList())
    val nearbyPlayers: StateFlow<List<UserProfile>> = _nearbyPlayers

    init {
        viewModelScope.launch {
            statisticsManager.getGameStats().collect { stats ->
                _gameStats.value = stats
            }
        }
        viewModelScope.launch {
            diceDataStore.getVibrationEnabled().collect { enabled ->
                _vibrationEnabled.value = enabled
            }
        }
        shakeDetectionService.setOnShakeListener {
            viewModelScope.launch {
                _isRolling.value = true
                rollDice(_selectedBoard.value)
                delay(2000)
                _isRolling.value = false
            }
        }
        resetGame()
    }

    // Handle connection between Multiplayer and dice view model
    fun handleMultiplayerRoll(sessionId: String, diceResults: List<Int>, score: Int) {
        viewModelScope.launch {
            when (val currentSession = multiplayerViewModel.gameSession.value) {
                is Result.Success -> {
                    if (currentSession.data.currentTurn == gameRepository.getCurrentUserId()) {
                        multiplayerViewModel.updateGameState(
                            sessionId = sessionId,
                            diceResults = diceResults,
                            score = score,
                            isGameOver = score >= (winConditions[_selectedBoard.value]
                                ?: Int.MAX_VALUE)
                        )
                    }
                }

                else -> Timber.e("Invalid game session state")
            }
        }
    }

    // Win Conditions
    private val winConditions = mapOf(
        GameBoard.PIG.modeName to 100,
        GameBoard.GREED.modeName to 10000,
        GameBoard.MEXICO.modeName to 200,
        GameBoard.CHICAGO.modeName to 100,
        GameBoard.BALUT.modeName to 100
    )

    fun rollDice(selectedBoard: String) = viewModelScope.launch {
        if (!validateBoard(selectedBoard)) return@launch

        _isRolling.value = true  // Start rolling animation

        provideHapticFeedback()
        val diceCount = getDiceCountForBoard(selectedBoard)
        val results = generateDiceRolls(diceCount)
        updateDiceImages(results)
        processRollResults(selectedBoard, results)

        delay(2000)
        _isRolling.value = false  // End rolling animation
    }

    private fun validateBoard(board: String): Boolean {
        if (!GameBoard.entries.any { it.modeName == board }) {
            Timber.e("Invalid board selected: $board")
            return false
        }
        return true
    }

    private fun getDiceCountForBoard(board: String) = when (board) {
        GameBoard.PIG.modeName -> 1
        GameBoard.GREED.modeName -> 6
        GameBoard.MEXICO.modeName -> 2
        GameBoard.CHICAGO.modeName -> 2
        GameBoard.BALUT.modeName -> 5
        else -> 0
    }

    private fun generateDiceRolls(count: Int) = List(count) { Random.nextInt(6) + 1 }

    private fun updateDiceImages(results: List<Int>) {
        _diceImages.value = results.map { setImage(it) }
    }

    // Process Rolls for the results and for each board
    private fun processRollResults(board: String, results: List<Int>) {
        when (board) {
            GameBoard.PIG.modeName -> processPigRoll(results.first())
            GameBoard.GREED.modeName -> processGreedRoll(results)
            GameBoard.MEXICO.modeName -> processMexicoRoll(results)
            GameBoard.CHICAGO.modeName -> processChicagoRoll(results)
            GameBoard.BALUT.modeName -> processBalutRoll(results)
        }
    }

    private fun processPigRoll(result: Int) {
        if (result == 1) {
            _scoreState.value = _scoreState.value.copy(
                currentTurnScore = 0,
                resultMessage = "Rolled a 1! Turn ended."
            )
            endTurn(GameBoard.PIG.modeName)
        } else {
            val newScore = _scoreState.value.currentTurnScore + result
            _scoreState.value = _scoreState.value.copy(
                currentTurnScore = newScore,
                resultMessage = newScore.toString()
            )
        }
    }

    private fun processGreedRoll(results: List<Int>) {
        val score = calculateGreedScore(results)
        _scoreState.value = _scoreState.value.copy(
            currentTurnScore = _scoreState.value.currentTurnScore + score,
            resultMessage = buildGreedScoreMessage(results, score)
        )
    }

    private fun processMexicoRoll(results: List<Int>) {
        val score = calculateMexicoScore(results[0], results[1])
        _mexicoScoreState.value.roundScores.add(score)
        _scoreState.value = _scoreState.value.copy(
            currentTurnScore = score,
            resultMessage = buildMexicoResultMessage(results[0], results[1], score)
        )
    }

    private fun processChicagoRoll(results: List<Int>) {
        val sum = results.sum()
        val currentRound = _chicagoScoreState.value.currentRound

        if (sum == currentRound && !_chicagoScoreState.value.hasScored) {
            _chicagoScoreState.value = _chicagoScoreState.value.copy(
                totalScore = _chicagoScoreState.value.totalScore + currentRound,
                roundScore = currentRound,
                hasScored = true
            )
        }

        if (_chicagoScoreState.value.hasScored && currentRound < 12) {
            _chicagoScoreState.value = _chicagoScoreState.value.copy(
                currentRound = currentRound + 1,
                roundScore = 0,
                hasScored = false
            )
        }
    }

    private fun processBalutRoll(results: List<Int>) {
        _balutScoreState.value = _balutScoreState.value.copy(
            rollsLeft = _balutScoreState.value.rollsLeft - 1
        )

        if (_balutScoreState.value.rollsLeft == 0) {
            val currentIndex =
                BalutScoreState.categories.indexOf(_balutScoreState.value.currentCategory)
            if (currentIndex < BalutScoreState.categories.size - 1) {
                _balutScoreState.value = _balutScoreState.value.copy(
                    currentCategory = BalutScoreState.categories[currentIndex + 1],
                    rollsLeft = _balutScoreState.value.maxRolls,
                    currentRound = _balutScoreState.value.currentRound + 1
                )
            }
        }
    }

    // calculate and build the scores for greed and mexico
    private fun calculateGreedScore(dice: List<Int>): Int {
        val counts = dice.groupBy { it }.mapValues { it.value.size }
        var score = 0

        // Check for special combinations first
        when {
            dice.sorted() == listOf(1, 2, 3, 4, 5, 6) -> return 1500 // Straight
            counts.any { it.value == 6 } -> return 3000 // Six of a kind
            counts.any { it.value == 5 } -> return 2000 // Five of a kind
            counts.count { it.value == 2 } == 3 -> return 1500 // Three pairs
        }
        // Process three or more of a kind
        counts.forEach { (number, count) ->
            when {
                count >= 3 -> {
                    // Base score for three of a kind
                    score += when (number) {
                        1 -> 1000
                        else -> number * 100
                    }
                    // Additional singles for 1s and 5s beyond the three
                    val remainingCount = count - 3
                    if (remainingCount > 0) {
                        when (number) {
                            1 -> score += remainingCount * 100
                            5 -> score += remainingCount * 50
                        }
                    }
                }
                // Score single 1s and 5s
                else -> {
                    when (number) {
                        1 -> score += count * 100
                        5 -> score += count * 50
                    }
                }
            }
        }
        return score
    }

    private fun buildGreedScoreMessage(dice: List<Int>, score: Int): String {
        if (score == 0) return "No Score"

        val counts = dice.groupBy { it }.mapValues { it.value.size }
        return buildString {
            when {
                dice.sorted() == listOf(1, 2, 3, 4, 5, 6) -> append("Straight! +1500")
                counts.any { it.value == 6 } -> append("Six of a Kind! +3000")
                counts.any { it.value == 5 } -> append("Five of a Kind! +2000")
                counts.count { it.value == 2 } == 3 -> append("Three Pairs! +1500")
                else -> {
                    counts.forEach { (number, count) ->
                        when {
                            count >= 4 -> appendLine("Four ${number}s! +1000")
                            count >= 3 -> {
                                val baseScore = if (number == 1) 1000 else number * 100
                                appendLine("Three ${number}s! +$baseScore")
                            }

                            count > 0 && (number == 1 || number == 5) -> {
                                val singleScore = if (number == 1) count * 100 else count * 50
                                appendLine("Single ${if (number == 1) "Ones" else "Fives"} +$singleScore")
                            }
                        }
                    }
                }
            }
            appendLine("Total Score: $score")
        }.trim()
    }

    private fun calculateMexicoScore(dice1: Int, dice2: Int): Int {
        return when {
            // México (2,1) is highest
            setOf(dice1, dice2) == setOf(2, 1) -> 21
            // Doubles score as dice × 11
            dice1 == dice2 -> dice1 * 11
            // Otherwise, higher number first
            else -> maxOf(dice1, dice2) * 10 + minOf(dice1, dice2)
        }
    }

    private fun buildMexicoResultMessage(dice1: Int, dice2: Int, score: Int): String {
        return when {
            score == 21 -> "¡México! (21)"
            dice1 == dice2 -> "Double ${dice1}s ($score)"
            else -> "$score (${maxOf(dice1, dice2)},${minOf(dice1, dice2)})"
        }
    }

    fun endMexicoRound() {
        val currentLives = _mexicoScoreState.value.lives
        if (currentLives > 1) {
            _mexicoScoreState.value = _mexicoScoreState.value.copy(
                lives = currentLives - 1,
                isFirstRound = false,
                roundScores = mutableListOf(),
                currentRound = _mexicoScoreState.value.currentRound + 1
            )
            _scoreState.value = _scoreState.value.copy(
                resultMessage = "Lives remaining: ${currentLives - 1}"
            )
        } else {
            _scoreState.value = _scoreState.value.copy(
                isGameOver = true,
                resultMessage = "Game Over! Final Round: ${_mexicoScoreState.value.currentRound}"
            )
        }
    }

    // Update the endTurn function to trigger confetti on win
    fun endTurn(board: String = _selectedBoard.value) {
        val currentScore = _scoreState.value.overallScore + _scoreState.value.currentTurnScore
        val isGameOver = currentScore >= (winConditions[board] ?: Int.MAX_VALUE)
        _scoreState.value = ScoreState(
            overallScore = currentScore,
            isGameOver = isGameOver,
            resultMessage = if (isGameOver) "Game Over! Final Score: $currentScore" else currentScore.toString()
        )
        if (isGameOver) {
            triggerConfetti()
        }
    }

    fun resetGame() {
        _scoreState.value = ScoreState()
        _diceImages.value = List(6) { R.drawable.empty_dice }
        when (_selectedBoard.value) {
            GameBoard.MEXICO.modeName -> _mexicoScoreState.value = MexicoScoreState()
            GameBoard.CHICAGO.modeName -> _chicagoScoreState.value = ChicagoScoreState()
        }
    }

    fun triggerConfetti() {
        viewModelScope.launch {
            _showConfetti.value = true
            delay(2000) // Duration of confetti animation
            _showConfetti.value = false
        }
    }

    // Set the functions for the dice

    fun setSelectedBoard(board: String) {
        _selectedBoard.value = board
    }

    private fun setImage(value: Int) = when (value) {
        1 -> R.drawable.dice_1
        2 -> R.drawable.dice_2
        3 -> R.drawable.dice_3
        4 -> R.drawable.dice_4
        5 -> R.drawable.dice_5
        6 -> R.drawable.dice_6
        else -> R.drawable.empty_dice
    }

    fun pauseShakeDetection() {
        shakeDetectionService.clearOnShakeListener()
        shakeDetectionService.stopListening()
    }

    fun resumeShakeDetection() {
        shakeDetectionService.setOnShakeListener {
            viewModelScope.launch {
                if (!_isRolling.value) {
                    rollDice(_selectedBoard.value)
                }
            }
        }
        shakeDetectionService.startListening()
    }

    fun setVibrationEnabled(enabled: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        diceDataStore.setVibrationEnabled(enabled)
    }

    private fun provideHapticFeedback() {
        if (!_vibrationEnabled.value) return

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getApplication<Application>()
                .getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun setPlayMode(playMode: PlayMode) {
        _playMode.value = playMode
        Timber.d("Play mode set to: $playMode")
        resetGame()
    }

    override fun onCleared() {
        super.onCleared()
        shakeDetectionService.clearOnShakeListener()
    }
}

@Composable
fun ConfettiAnimation(
    modifier: Modifier = Modifier,
    onComplete: () -> Unit = {}
) {
    var isPlaying by remember { mutableStateOf(true) }
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.confetti)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        speed = 1.5f,
        iterations = 1,
        restartOnPlay = false,
        cancellationBehavior = LottieCancellationBehavior.Immediately
    )
    // Handle completion
    LaunchedEffect(progress) {
        if (progress == 1f) {
            isPlaying = false
            onComplete()
        }
    }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize()
        )
    }
}

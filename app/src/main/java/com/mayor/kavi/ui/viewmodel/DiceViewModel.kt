package com.mayor.kavi.ui.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.*
import android.os.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.*
import com.airbnb.lottie.compose.*
import dagger.hilt.android.lifecycle.HiltViewModel
import com.mayor.kavi.R
import com.mayor.kavi.data.DataStoreManager
import com.mayor.kavi.data.MultiplayerManager
import com.mayor.kavi.ui.components.GameAI
import com.mayor.kavi.ui.viewmodel.GameBoard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

enum class GameBoard(val modeName: String) {
    PIG("Pig"),
    GREED("Greed / 10000"),
    MEXICO("Mexico"),
    CHICAGO("Chicago"),
    BALUT("Balut")
}

data class ScoreState(
    val currentTurnScore: Int = 0,
    val overallScore: Int = 0,
    val isGameOver: Boolean = false,
    val resultMessage: String = ""
)

data class MexicoScoreState(
    val currentRound: Int = 1,
    val lives: Int = 6,
    val isFirstRound: Boolean = true,
    val roundScores: MutableList<Int> = mutableListOf()
)

data class ChicagoScoreState(
    val currentRound: Int = 2,
    val totalScore: Int = 0,
    val roundScore: Int = 0,
    val hasScored: Boolean = false
)

data class BalutScoreState(
    val currentCategory: String = "Aces",
    val currentRound: Int = 1,
    val maxRolls: Int = 3,
    val rollsLeft: Int = 3
) {
    companion object {
        val categories = listOf(
            "Aces", "Twos", "Threes", "Fours", "Fives", "Sixes",
            "Straight", "Full House", "Four of a Kind", "Five of a Kind", "Choice"
        )
    }
}

@HiltViewModel
class DiceViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    private val diceDataStore = DataStoreManager.getInstance(application)

    // Settings & Board Selection
    private val _shakeEnabled = diceDataStore.getShakeEnabled().asLiveData()
    val shakeEnabled: LiveData<Boolean> = _shakeEnabled
    private val _selectedBoard = diceDataStore.getSelectedBoard().asLiveData()
    val selectedBoard: LiveData<String> = _selectedBoard

    // Game States
    private val _scoreState = MutableStateFlow(ScoreState())
    val scoreState: StateFlow<ScoreState> = _scoreState
    private val _mexicoScoreState = MutableStateFlow(MexicoScoreState())
    val mexicoScoreState: StateFlow<MexicoScoreState> = _mexicoScoreState.asStateFlow()
    private val _chicagoScoreState = MutableStateFlow(ChicagoScoreState())
    val chicagoScoreState: StateFlow<ChicagoScoreState> = _chicagoScoreState.asStateFlow()
    private val _balutScoreState = MutableStateFlow(BalutScoreState())
    val balutScoreState: StateFlow<BalutScoreState> = _balutScoreState.asStateFlow()

    // Confetti settings
    private val _showConfetti = MutableStateFlow(false)
    val showConfetti: StateFlow<Boolean> = _showConfetti.asStateFlow()

    // Dice Display
    private val _diceImages = MutableStateFlow(List(6) { R.drawable.empty_dice })
    val diceImages: StateFlow<List<Int>> = _diceImages

    // Opponent management
    private val multiplayerManager = MultiplayerManager()
    private val gameAI = GameAI()

    private val _gameMode = MutableStateFlow<GameMode>(GameMode.SinglePlayer)
    val gameMode: StateFlow<GameMode> = _gameMode

    sealed class GameMode {
        object SinglePlayer : GameMode()
        object VsAI : GameMode()
        data class Multiplayer(val sessionId: String) : GameMode()
    }

    // Shake Detection
    private val sensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var accelerometerListener: SensorEventListener? = null
    private var lastShakeTime = 0L
    private val shakeCooldown = 1000L
    private val shakeThreshold = 0.5f

    // Win Conditions
    private val winConditions = mapOf(
        GameBoard.PIG.modeName to 100,
        GameBoard.GREED.modeName to 10000,
        GameBoard.MEXICO.modeName to 200,
        GameBoard.CHICAGO.modeName to 100,
        GameBoard.BALUT.modeName to 100
    )

    init {
        resetGame()
    }

    fun startAIGame(board: String) {
        _gameMode.value = GameMode.VsAI
        setSelectedBoard(board)
        resetGame()
    }

    fun processAITurn() {
        viewModelScope.launch {
            val currentBoard = selectedBoard.value ?: GameBoard.PIG.modeName

            // Convert dice images back to values for AI processing
            val diceValues = _diceImages.value.map { image ->
                when (image) {
                    R.drawable.dice_1 -> 1
                    R.drawable.dice_2 -> 2
                    R.drawable.dice_3 -> 3
                    R.drawable.dice_4 -> 4
                    R.drawable.dice_5 -> 5
                    R.drawable.dice_6 -> 6
                    else -> 0
                }
            }.filter { it != 0 }

            val decision = gameAI.makeDecision(
                currentBoard,
                scoreState.value.currentTurnScore,
                diceValues
            )

            delay(1000) // Add slight delay for more natural AI behavior

            when (decision) {
                is GameAI.AIDecision.Roll -> {
                    rollDice(currentBoard)
                    // Schedule next AI decision after roll animation
                    delay(2000)
                    processAITurn()
                }

                is GameAI.AIDecision.Bank -> {
                    endTurn(currentBoard)
                }

                is GameAI.AIDecision.SelectDice -> {
                    when (currentBoard) {
                        GameBoard.BALUT.modeName -> {
                            // Handle Balut-specific dice selection
                            val selectedDice = decision.indices
                            // Update held dice state
                            // Re-roll non-selected dice
                        }
                    }
                }
            }
        }
    }

    fun rollDice(selectedBoard: String) {
        if (!validateBoard(selectedBoard)) return

        provideHapticFeedback()
        val diceCount = getDiceCountForBoard(selectedBoard)
        val results = generateDiceRolls(diceCount)
        updateDiceImages(results)
        processRollResults(selectedBoard, results)
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

    fun triggerConfetti() {
        viewModelScope.launch {
            _showConfetti.value = true
            delay(2000) // Duration of confetti animation
            _showConfetti.value = false
        }
    }

    // Update the endTurn function to trigger confetti on win
    fun endTurn(board: String = selectedBoard.value ?: GameBoard.PIG.modeName) {
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

    fun startShakeDetection() {
        if (shakeEnabled.value != true) return

        accelerometerListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type != Sensor.TYPE_ACCELEROMETER) return
                    if (System.currentTimeMillis() - lastShakeTime < shakeCooldown) return

                    val acceleration = it.values.map { value -> value * value }.sum()
                    if (acceleration > shakeThreshold) {
                        lastShakeTime = System.currentTimeMillis()
                        selectedBoard.value?.let { board -> rollDice(board) }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let { sensor ->
            sensorManager.registerListener(
                accelerometerListener,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    fun setShakeEnabled(enabled: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        diceDataStore.setShakeEnabled(enabled)
        if (enabled) {
            startShakeDetection()
        } else {
            stopShakeDetection()
        }
    }

    fun stopShakeDetection() {
        accelerometerListener?.let {
            sensorManager.unregisterListener(it)
            accelerometerListener = null
        }
    }

    fun resetGame() {
        _scoreState.value = ScoreState()
        _diceImages.value = List(6) { R.drawable.empty_dice }
        when (selectedBoard.value) {
            GameBoard.MEXICO.modeName -> _mexicoScoreState.value = MexicoScoreState()
            GameBoard.CHICAGO.modeName -> _chicagoScoreState.value = ChicagoScoreState()
            GameBoard.BALUT.modeName -> _balutScoreState.value = BalutScoreState()
        }
    }

    private fun setImage(value: Int) = when (value) {
        in 1..6 -> R.drawable::class.java.getField("dice_$value").getInt(null)
        else -> R.drawable.empty_dice
    }

    fun setSelectedBoard(board: String) = viewModelScope.launch(Dispatchers.IO) {
        diceDataStore.setSelectedBoard(board)
        resetGame()
    }

    private fun provideHapticFeedback() {
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

    override fun onCleared() {
        super.onCleared()
        stopShakeDetection()
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
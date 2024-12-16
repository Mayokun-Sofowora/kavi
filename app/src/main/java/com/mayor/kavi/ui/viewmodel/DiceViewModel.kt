package com.mayor.kavi.ui.viewmodel

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
import kotlin.random.Random
import com.mayor.kavi.data.games.*
import com.mayor.kavi.data.manager.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class ScoreState(
    val currentTurnScore: Int = 0,
    val overallScore: Int = 0,
    val isGameOver: Boolean = false,
    val resultMessage: String = ""
)

@HiltViewModel
class DiceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val statisticsManager: StatisticsManager,
    val gameRepository: GameRepository,
    private val shakeDetectionService: ShakeDetectionService
) : ViewModel() {
    private val diceDataStore = DataStoreManager.getInstance(context)

    // Settings & Board Selection
    private val _vibrationEnabled = MutableStateFlow(true)
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
    private val _greedScoreState = MutableStateFlow(GreedScoreState())
    val greedScoreState: StateFlow<GreedScoreState> = _greedScoreState.asStateFlow()

    // Confetti settings
    private val _showConfetti = MutableStateFlow(false)

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
    private val _gameStats = MutableStateFlow(GameStats(0, emptyMap(), emptyMap(), emptyList()))
    val gameStats: StateFlow<GameStats> = statisticsManager.getGameStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GameStats(0, emptyMap(), emptyMap(), emptyList())
        )
    val playerAnalysis: StateFlow<PlayerAnalysis?> = statisticsManager.playerAnalysis.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val shakeRates: StateFlow<List<Pair<Long, Int>>> =
        statisticsManager.getGameStats().map { it.shakeRates }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val mostPlayedGame: StateFlow<String?> = statisticsManager.getGameStats().map {
        it.highScores.maxByOrNull { it.value }?.key
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _heldDice = MutableStateFlow<Set<Int>>(emptySet())
    val heldDice: StateFlow<Set<Int>> = _heldDice.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                statisticsManager.getGameStats().collect { stats -> _gameStats.value = stats }
            }
            launch {
                diceDataStore.getVibrationEnabled()
                    .collect { enabled -> _vibrationEnabled.value = enabled }
            }
        }
        resetGame()
    }

    // Win Conditions
    private val winConditions = mapOf(
        GameBoard.PIG.modeName to 100,
        GameBoard.GREED.modeName to 10000,
        GameBoard.MEXICO.modeName to 200,
        GameBoard.CHICAGO.modeName to 100,
        GameBoard.BALUT.modeName to 100
    )

    fun rollDice(selectedBoard: String) {
        if (!validateBoard(selectedBoard) || _isRolling.value) return

        viewModelScope.launch {
            try {
                _isRolling.value = true
                provideHapticFeedback()

                val diceCount = getDiceCountForBoard(selectedBoard)
                val results = when (selectedBoard) {
                    GameBoard.GREED.modeName, GameBoard.BALUT.modeName -> rollDiceWithoutHolds(
                        diceCount
                    )

                    else -> generateDiceRolls(diceCount)
                }
                updateDiceImages(results)

                when (playMode.value) {
                    is PlayMode.Multiplayer -> {
                        val session = (playMode.value as PlayMode.Multiplayer).sessionId
                        processRollResults(selectedBoard, results)
                    }

                    PlayMode.SinglePlayer -> {
                        processRollResults(selectedBoard, results)
                    }
                }
                delay(1000)
            } catch (e: Exception) {
                Timber.e(e, "Error during dice roll")
            } finally {
                _isRolling.value = false
            }
        }
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
        // Update statistics for all games
        viewModelScope.launch {
            statisticsManager.updateGameStats(
                board = GameBoard.valueOf(board.uppercase()),
                score = when (board) {
                    GameBoard.GREED.modeName -> _greedScoreState.value.currentScore
                    GameBoard.MEXICO.modeName -> _mexicoScoreState.value.roundScores.sum()
                    GameBoard.CHICAGO.modeName -> _chicagoScoreState.value.totalScore
                    GameBoard.BALUT.modeName -> _balutScoreState.value.categoryScores.values.sum()
                    else -> _scoreState.value.currentTurnScore
                },
                isWin = when (board) {
                    GameBoard.GREED.modeName -> _greedScoreState.value.isGameOver
                    GameBoard.MEXICO.modeName -> _mexicoScoreState.value.isGameOver
                    GameBoard.CHICAGO.modeName -> _chicagoScoreState.value.currentRound >= 12
                    GameBoard.BALUT.modeName -> _balutScoreState.value.currentRound >= BalutScoreState.categories.size
                    else -> _scoreState.value.isGameOver
                }
            )
        }
        // Update player analysis
        updateAnalysis()
    }

    private fun processPigRoll(result: Int) {
        if (result == 1) {
            statisticsManager.updateScoreState(0)
            _scoreState.value = _scoreState.value.copy(
                currentTurnScore = 0,
                resultMessage = "Rolled a 1! Turn ended"
            )
        } else {
            val newScore = _scoreState.value.currentTurnScore + result
            statisticsManager.updateScoreState(newScore)
            _scoreState.value = _scoreState.value.copy(
                currentTurnScore = newScore,
                resultMessage = newScore.toString()
            )
        }
    }

    private fun processGreedRoll(results: List<Int>) {
        val (score, newScoringDice) = calculateGreedScore(results, _greedScoreState.value.heldDice)

        if (score == 0) {
            // Bust - lose turn score
            _greedScoreState.value = _greedScoreState.value.copy(
                turnScore = 0,
                canReroll = false,
                scoringDice = emptySet()
            )
            _scoreState.value = _scoreState.value.copy(
                resultMessage = "Bust! No scoring dice."
            )
            return
        }

        _greedScoreState.value = _greedScoreState.value.copy(
            turnScore = _greedScoreState.value.turnScore + score,
            scoringDice = newScoringDice,
            canReroll = newScoringDice.isNotEmpty()
        )

        _scoreState.value = _scoreState.value.copy(
            currentTurnScore = _greedScoreState.value.turnScore,
            resultMessage = buildGreedScoreMessage(results, score)
        )
    }

    private fun processMexicoRoll(results: List<Int>) {
        if (_mexicoScoreState.value.lives <= 0 || _mexicoScoreState.value.isGameOver) return
        val score = calculateMexicoScore(results[0], results[1])
        val newRoundScores = _mexicoScoreState.value.roundScores.toMutableList()
        newRoundScores.add(score)
        _mexicoScoreState.value = _mexicoScoreState.value.copy(roundScores = newRoundScores)
        statisticsManager.updateScoreState(score)

        _scoreState.value = _scoreState.value.copy(
            currentTurnScore = score,
            resultMessage = buildMexicoResultMessage(results[0], results[1], score)
        )

        if (!_mexicoScoreState.value.isTimerRunning) {
            startMexicoRoundTimer()
        }
    }

    private fun processChicagoRoll(results: List<Int>) {
        val score = calculateChicagoScore(results)
        val roundScores = _chicagoScoreState.value.roundScores.toMutableMap()

        if (score > 0 && !_chicagoScoreState.value.hasScored) {
            roundScores[_chicagoScoreState.value.currentRound] = score
            _chicagoScoreState.value = _chicagoScoreState.value.copy(
                totalScore = _chicagoScoreState.value.totalScore + score,
                roundScore = score,
                hasScored = true,
                roundScores = roundScores
            )
        }

        _scoreState.value = _scoreState.value.copy(
            currentTurnScore = score,
            resultMessage = buildChicagoResultMessage(results, score)
        )

        // Check for round advancement or game end
        if (_chicagoScoreState.value.hasScored || _chicagoScoreState.value.currentRound >= 12) {
            if (_chicagoScoreState.value.currentRound >= 12) {
                _scoreState.value = _scoreState.value.copy(
                    overallScore = _chicagoScoreState.value.totalScore,
                    isGameOver = true,
                    resultMessage = "Game Over! Final Score: ${_chicagoScoreState.value.totalScore}"
                )
            } else {
                _chicagoScoreState.value = _chicagoScoreState.value.copy(
                    currentRound = _chicagoScoreState.value.currentRound + 1,
                    roundScore = 0,
                    hasScored = false
                )
            }
        }
    }

    private fun processBalutRoll(diceResults: List<Int>) {
        if (_balutScoreState.value.rollsLeft <= 0) return
        val newBalutState = _balutScoreState.value.copy(
            rollsLeft = _balutScoreState.value.rollsLeft - 1
        )
        _balutScoreState.value = newBalutState
        statisticsManager.updateBalutScoreState(newBalutState)
    }

    // Add function to allow scoring before 3 rolls are used
    fun scoreCurrentDice() {
        viewModelScope.launch {
            val currentDiceValues = _diceImages.value.mapIndexed { index, imageRes ->
                when (imageRes) {
                    R.drawable.dice_1 -> 1
                    R.drawable.dice_2 -> 2
                    R.drawable.dice_3 -> 3
                    R.drawable.dice_4 -> 4
                    R.drawable.dice_5 -> 5
                    R.drawable.dice_6 -> 6
                    else -> 0
                }
            }.filter { it != 0 }

            if (currentDiceValues.isNotEmpty()) {
                val category = _balutScoreState.value.currentCategory
                val score = calculateBalutScore(currentDiceValues, category.toString())
                val updatedScores = _balutScoreState.value.categoryScores.toMutableMap()
                updatedScores[category.toString()] = score

                // Move to next round
                val nextRound = _balutScoreState.value.currentRound + 1
                val nextCategory = if (nextRound < BalutScoreState.categories.size) {
                    BalutScoreState.categories[nextRound]
                } else {
                    _balutScoreState.value.currentCategory
                }

                _balutScoreState.value = _balutScoreState.value.copy(
                    categoryScores = updatedScores,
                    currentRound = nextRound,
                    currentCategory = nextCategory,
                    rollsLeft = 3,
                    heldDice = emptySet()
                )
                statisticsManager.updateBalutScoreState(_balutScoreState.value)

                // Update overall score and check for game over
                val totalScore = updatedScores.values.sum()
                _scoreState.value = _scoreState.value.copy(
                    overallScore = totalScore,
                    isGameOver = nextRound >= BalutScoreState.categories.size
                )
                statisticsManager.updateScoreState(totalScore)
            }
        }
    }

    private fun startMexicoRoundTimer() {
        viewModelScope.launch {
            _mexicoScoreState.value = _mexicoScoreState.value.copy(
                isTimerRunning = true,
                roundTimeSeconds = 15
            )

            while (_mexicoScoreState.value.roundTimeSeconds > 0 && _mexicoScoreState.value.isTimerRunning) {
                delay(1000)
                _mexicoScoreState.value = _mexicoScoreState.value.copy(
                    roundTimeSeconds = _mexicoScoreState.value.roundTimeSeconds - 1
                )
            }

            if (_mexicoScoreState.value.roundTimeSeconds == 0) {
                endMexicoRound()
            }
        }
    }

    private fun endMexicoRound() {
        val currentScore = _mexicoScoreState.value.roundScores.lastOrNull() ?: 0
        val simulatedOpponentScore = Random.nextInt(21, 66) // Random opponent score
        var newLives = _mexicoScoreState.value.lives
        var gameStatusMessage = ""

        if (_mexicoScoreState.value.currentRoundNumber >= 6) {
            _mexicoScoreState.value = _mexicoScoreState.value.copy(
                isTimerRunning = false,
                isGameOver = true,
                gameStatus = "Game Over! Max rounds reached."
            )
            _scoreState.value = _scoreState.value.copy(
                isGameOver = true,
                resultMessage = "Game Over! Final Score: ${_mexicoScoreState.value.roundScores.sum()} Max Rounds Reached"
            )
            return
        }

        if (currentScore < simulatedOpponentScore) {
            // Player lost the round
            newLives = _mexicoScoreState.value.lives - 1
            gameStatusMessage = if (newLives > 0)
                "Lost round! Opponent scored $simulatedOpponentScore. $newLives lives remaining."
            else
                "Game Over! Out of lives!"
            _mexicoScoreState.value = _mexicoScoreState.value.copy(
                lives = newLives,
                gameStatus = gameStatusMessage
            )
            if (newLives <= 0) {
                _mexicoScoreState.value = _mexicoScoreState.value.copy(
                    isTimerRunning = false,
                    isGameOver = true,
                    gameStatus = "Game Over! Out of lives!"
                )
                _scoreState.value = _scoreState.value.copy(
                    isGameOver = true,
                    resultMessage = "Game Over! Final Score: ${_mexicoScoreState.value.roundScores.sum()}"
                )
                return
            }
        } else {
            // Player won the round
            gameStatusMessage = "Won round! Opponent scored $simulatedOpponentScore"
            _mexicoScoreState.value = _mexicoScoreState.value.copy(
                gameStatus = gameStatusMessage,
                highestScore = maxOf(_mexicoScoreState.value.highestScore, currentScore)
            )
        }

        // Start next round (if not game over)
        if (_mexicoScoreState.value.currentRoundNumber < 6) {
            _mexicoScoreState.value = _mexicoScoreState.value.copy(
                currentRoundNumber = _mexicoScoreState.value.currentRoundNumber + 1,
                isTimerRunning = false
            )
            startMexicoRoundTimer()
        }
    }

    // calculate and build the scores
    private fun calculateGreedScore(dice: List<Int>, heldDice: Set<Int>): Pair<Int, Set<Int>> {
        var score = 0
        val scoringDice = mutableSetOf<Int>()

        // First check for combinations (three of a kind)
        val diceFrequency = dice.groupBy { it }
        for ((number, occurrences) in diceFrequency) {
            if (occurrences.size >= 3) {
                val combination = List(3) { number }
                val combinationScore = GreedScoreState.COMBINATIONS[combination]
                if (combinationScore != null) {
                    score += combinationScore
                    scoringDice.addAll(occurrences.take(3).map { dice.indexOf(it) })
                }
                // Handle remaining dice of same number
                val remainingCount = occurrences.size - 3
                if (number == 1 || number == 5) {
                    score += remainingCount * (GreedScoreState.SCORING_VALUES[number] ?: 0)
                    scoringDice.addAll(
                        occurrences.takeLast(remainingCount).map { dice.indexOf(it) })
                }
            } else {
                // Handle individual scoring dice (1s and 5s)
                if (number == 1 || number == 5) {
                    score += occurrences.size * (GreedScoreState.SCORING_VALUES[number] ?: 0)
                    scoringDice.addAll(occurrences.map { dice.indexOf(it) })
                }
            }
        }

        return Pair(score, scoringDice)
    }

    fun bankGreedScore() {
        viewModelScope.launch {
            val newTotalScore =
                _greedScoreState.value.currentScore + _greedScoreState.value.turnScore
            val isGameOver =
                newTotalScore >= (winConditions[GameBoard.GREED.modeName] ?: Int.MAX_VALUE)

            _greedScoreState.value = _greedScoreState.value.copy(
                currentScore = newTotalScore,
                turnScore = 0,
                scoringDice = emptySet(),
                canReroll = true,
                isGameOver = isGameOver,
                resultMessage = if (isGameOver) "Game Over! Final Score: $newTotalScore" else newTotalScore.toString(),
                roundHistory = _greedScoreState.value.roundHistory + _greedScoreState.value.turnScore
            )

            if (newTotalScore >= 10000) {
                _scoreState.value = _scoreState.value.copy(
                    overallScore = newTotalScore,
                    isGameOver = true,
                    resultMessage = "Game Over! Final Score: $newTotalScore"
                )
            }
        }
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
            appendLine("\nCombo Score: $score")
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

    private fun calculateChicagoScore(dice: List<Int>): Int {
        val sum = dice.sum()
        val currentRound = _chicagoScoreState.value.currentRound

        return when {
            sum == currentRound -> currentRound // Perfect match
            sum > currentRound -> 0 // Over the target
            else -> 0 // Under the target
        }
    }

    private fun buildChicagoResultMessage(dice: List<Int>, score: Int): String {
        val sum = dice.sum()
        val currentRound = _chicagoScoreState.value.currentRound

        return when {
            sum == currentRound -> "Perfect! Scored $score points"
            sum > currentRound -> "Too high! Need exactly $currentRound"
            else -> "Too low! Need exactly $currentRound"
        }
    }

    private fun calculateBalutScore(dice: List<Int>, category: String): Int {
        val counts = dice.groupBy { it }.mapValues { it.value.size }

        return when (category) {
            "Aces" -> counts[1]?.times(1) ?: 0
            "Twos" -> counts[2]?.times(2) ?: 0
            "Threes" -> counts[3]?.times(3) ?: 0
            "Fours" -> counts[4]?.times(4) ?: 0
            "Fives" -> counts[5]?.times(5) ?: 0
            "Sixes" -> counts[6]?.times(6) ?: 0
            "Straight" -> if (dice.sorted() == listOf(1, 2, 3, 4, 5)) 30 else 0
            "Full House" -> if (counts.values.toSet() == setOf(2, 3)) 25 else 0
            "Four of a Kind" -> counts.entries.find { it.value >= 4 }?.let { it.key * 4 } ?: 0
            "Five of a Kind" -> if (counts.any { it.value == 5 }) 50 else 0
            "Choice" -> dice.sum()
            else -> 0
        }
    }

    fun endPigTurn() {
        viewModelScope.launch {
            val currentScore = _scoreState.value.overallScore + _scoreState.value.currentTurnScore
            val isGameOver =
                currentScore >= (winConditions[GameBoard.PIG.modeName] ?: Int.MAX_VALUE)

            _scoreState.value = ScoreState(
                overallScore = currentScore,
                isGameOver = isGameOver,
                resultMessage = if (isGameOver) "Game Over! Final Score: $currentScore" else currentScore.toString()
            )

            if (isGameOver) {
                triggerConfetti()
                when (playMode.value) {
                    is PlayMode.Multiplayer -> {
                        val session = (playMode.value as PlayMode.Multiplayer).sessionId
                        statisticsManager.updateGameStats(
                            board = GameBoard.PIG,
                            score = currentScore,
                            isWin = true
                        )
                    }

                    PlayMode.SinglePlayer -> {
                        statisticsManager.updateGameStats(
                            board = GameBoard.PIG,
                            score = currentScore,
                            isWin = true
                        )
                    }
                }
            }
        }
    }

    fun resetGame() {
        _scoreState.value = ScoreState()
        _diceImages.value = List(6) { R.drawable.empty_dice }
        when (_selectedBoard.value) {
            GameBoard.GREED.modeName -> {
                _greedScoreState.value = GreedScoreState()
                _heldDice.value = emptySet()
            }

            GameBoard.MEXICO.modeName -> _mexicoScoreState.value = MexicoScoreState()
            GameBoard.CHICAGO.modeName -> _chicagoScoreState.value = ChicagoScoreState()
            GameBoard.BALUT.modeName -> {
                _balutScoreState.value = BalutScoreState()
                _heldDice.value = emptySet()
            }
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
                updateShakeRate()
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
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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

    fun toggleDiceHold(index: Int) {
        when (_selectedBoard.value) {
            GameBoard.GREED.modeName, GameBoard.BALUT.modeName -> {
                val currentHeld = _heldDice.value.toMutableSet()
                if (currentHeld.contains(index)) {
                    currentHeld.remove(index)
                } else {
                    currentHeld.add(index)
                }
                _heldDice.value = currentHeld
            }
        }
    }

    private fun rollDiceWithoutHolds(count: Int): List<Int> {
        val currentDice = _diceImages.value.map { image ->
            when (image) {
                R.drawable.dice_1 -> 1
                R.drawable.dice_2 -> 2
                R.drawable.dice_3 -> 3
                R.drawable.dice_4 -> 4
                R.drawable.dice_5 -> 5
                R.drawable.dice_6 -> 6
                else -> (1..6).random()
            }
        }.toMutableList()

        // Only roll non-held dice
        for (i in 0 until count) {
            if (!_heldDice.value.contains(i)) {
                currentDice[i] = (1..6).random()
            }
        }

        return currentDice
    }

    private fun updateAnalysis() {
        statisticsManager.updateAnalysis()
    }

    private fun updateShakeRate() {
        viewModelScope.launch {
            statisticsManager.updateShakeRate()
        }
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
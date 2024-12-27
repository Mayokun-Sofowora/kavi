package com.mayor.kavi.data.manager

import com.mayor.kavi.util.GameBoard
import kotlinx.coroutines.flow.*
import kotlin.random.Random
import com.mayor.kavi.R
import kotlinx.coroutines.*

class DiceManager {
    private var _rollingDice = MutableStateFlow<Set<Int>>(emptySet())
    val isRolling = _rollingDice.map { it.isNotEmpty() }.stateIn(
        CoroutineScope(Dispatchers.Main),
        SharingStarted.Eagerly,
        false
    )

    private val _diceImages = MutableStateFlow(List(6) { R.drawable.empty_dice })
    val diceImages: StateFlow<List<Int>> = _diceImages

    private val _heldDice = MutableStateFlow<Set<Int>>(emptySet())
    val heldDice: StateFlow<Set<Int>> = _heldDice.asStateFlow()

    private val _currentRolls = MutableStateFlow(emptyList<Int>())
    val currentRolls: StateFlow<List<Int>> = _currentRolls.asStateFlow()

    private var _customDiceCount = MutableStateFlow(2)

    fun getDiceCountForBoard(boardType: String): Int = when (boardType) {
        GameBoard.PIG.modeName -> 1
        GameBoard.GREED.modeName -> 6
        GameBoard.BALUT.modeName -> 6
        GameBoard.CUSTOM.modeName -> _customDiceCount.value
        else -> 1
    }

    fun setDiceCount(count: Int) {
        _customDiceCount.value = count.coerceIn(1, 6)
        // Update dice images to match new count
        _diceImages.value = List(_customDiceCount.value) { R.drawable.empty_dice }
        // Clear held dice and current rolls
        _heldDice.value = emptySet()
        _currentRolls.value = emptyList()
    }

    suspend fun rollDiceForBoard(boardType: String): List<Int> {
        val diceCount = getDiceCountForBoard(boardType)
        val effectiveHeldDice =
            _heldDice.value // Ensure we always use the internal state for held dice

        // Set rolling state for non-held dice
        _rollingDice.value = (0 until diceCount).filter { !effectiveHeldDice.contains(it) }.toSet()

        // Generate new results while preserving held dice values
        val newRolls = generateDiceRolls(diceCount)
        val results = List(diceCount) { index ->
            if (effectiveHeldDice.contains(index)) {
                _currentRolls.value.getOrNull(index) ?: newRolls[index]
            } else {
                newRolls[index]
            }
        }
        _currentRolls.value = results
        updateDiceImages(results, effectiveHeldDice)

        // Simulate rolling delay and reset rolling state
        delay(500)
        _rollingDice.value = emptySet()
        return results
    }

    fun toggleHold(index: Int, boardType: String) {
        if (_currentRolls.value.isEmpty() || index >= _currentRolls.value.size) {
            return
        }
        val currentHeld = _heldDice.value.toMutableSet()
        if (currentHeld.contains(index)) {
            currentHeld.remove(index)
        } else {
            currentHeld.add(index)
        }
        _heldDice.value = currentHeld
    }

    fun resetHeldDice() {
        _heldDice.value = emptySet()
    }

    fun resetGame() {
        _diceImages.value = List(6) { R.drawable.empty_dice }
        resetHeldDice()
        _rollingDice.value = emptySet()
        _currentRolls.value = emptyList()
    }

    private fun generateDiceRolls(count: Int): List<Int> {
        val rolls = List(count) { Random.nextInt(6) + 1 }
        return rolls
    }

    private fun updateDiceImages(results: List<Int>, heldDice: Set<Int>) {
        _diceImages.value = results.mapIndexed { index, result ->
            if (_heldDice.value.contains(index)) {
                _diceImages.value.getOrNull(index) ?: R.drawable.empty_dice
            } else {
                getDiceImage(result)
            }
        }
    }

    private fun getDiceImage(value: Int): Int = when (value) {
        1 -> R.drawable.dice_1
        2 -> R.drawable.dice_2
        3 -> R.drawable.dice_3
        4 -> R.drawable.dice_4
        5 -> R.drawable.dice_5
        6 -> R.drawable.dice_6
        else -> R.drawable.empty_dice
    }

}

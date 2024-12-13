package com.mayor.kavi.ui.viewmodel

import androidx.lifecycle.*
import com.mayor.kavi.data.*
import com.mayor.kavi.data.manager.StatisticsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import com.mayor.kavi.util.Result
import kotlinx.coroutines.launch

@HiltViewModel
class MultiplayerViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val statisticsManager: StatisticsManager
) : ViewModel() {

    private val _gameSession = MutableStateFlow<Result<GameSession>>(Result.Loading(null))
    val gameSession: StateFlow<Result<GameSession>> = _gameSession

    private val _opponents = MutableStateFlow<List<UserProfile>>(emptyList())
    val opponents: StateFlow<List<UserProfile>> = _opponents

    private val _currentGameState = MutableStateFlow<Map<String, Any>>(emptyMap())
    val currentGameState: StateFlow<Map<String, Any>> = _currentGameState

    init {
        viewModelScope.launch {
            loadNearbyPlayers()
        }
    }

    private suspend fun loadNearbyPlayers() {
        gameRepository.getAllPlayers().fold(
            onSuccess = { players ->
                _opponents.value = players.filter { it.uid != gameRepository.getCurrentUserId() }
            },
            onFailure = {
                Timber.e(it, "Failed to load nearby players")
            }
        )
    }

    fun createGame(opponentId: String) = viewModelScope.launch {
        gameRepository.createGameSession(opponentId).fold(
            onSuccess = { session ->
                _gameSession.value = Result.Success(session)
                observeGameSession(session.id)
            },
            onFailure = { exception ->
                _gameSession.value = Result.Error("Failed to create game", exception)
            }
        )
    }

    fun joinGame(sessionId: String) = viewModelScope.launch {
        gameRepository.joinGameSession(sessionId).fold(
            onSuccess = { session ->
                _gameSession.value = Result.Success(session)
                observeGameSession(session.id)
            },
            onFailure = { exception ->
                _gameSession.value = Result.Error("Failed to join game", exception)
            }
        )
    }

    private fun observeGameSession(sessionId: String) = viewModelScope.launch {
        gameRepository.listenToGameUpdates(sessionId).collect { session ->
            _gameSession.value = Result.Success(session)
            _currentGameState.value = session.gameState
        }
    }

    fun updateGameState(
        sessionId: String,
        diceResults: List<Int>,
        score: Int,
        isGameOver: Boolean = false
    ) = viewModelScope.launch {
        val state = mutableMapOf<String, Any>(
            "diceResults" to diceResults,
            "score" to score,
            "isGameOver" to isGameOver,
            "timestamp" to System.currentTimeMillis()
        )

        gameRepository.updateGameState(sessionId, state).fold(
            onSuccess = {
                // State update handled by listener
            },
            onFailure = { exception ->
                Timber.e(exception, "Failed to update game state")
            }
        )
    }

    fun endGame(sessionId: String, finalScore: Int) = viewModelScope.launch {
        val state = mapOf(
            "status" to "completed",
            "finalScore" to finalScore,
            "completedAt" to System.currentTimeMillis()
        )

        gameRepository.updateGameState(sessionId, state)
    }
}
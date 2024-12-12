package com.mayor.kavi.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class MultiplayerManager {
    private val _gameState = MutableStateFlow<MultiplayerGameState>(MultiplayerGameState.Waiting)
    val gameState: StateFlow<MultiplayerGameState> = _gameState

    data class GameSession(
        val sessionId: String,
        val players: List<String>,
        val currentTurn: String,
        val gameBoard: String,
        val scores: Map<String, Int>,
        val lastRoll: List<Int>? = null
    )

    sealed class MultiplayerGameState {
        object Waiting : MultiplayerGameState()
        data class InProgress(val session: GameSession) : MultiplayerGameState()
        data class Completed(val winner: String) : MultiplayerGameState()
    }

    suspend fun createGame(gameBoard: String, players: List<String>): String {
        // Create new game session in Firestore
        return FirebaseFirestore.getInstance()
            .collection("game_sessions")
            .add(GameSession(
                sessionId = "",
                players = players,
                currentTurn = players.first(),
                gameBoard = gameBoard,
                scores = players.associateWith { 0 }
            )).await().id
    }

    suspend fun updateGameState(sessionId: String, newState: GameSession) {
        FirebaseFirestore.getInstance()
            .collection("game_sessions")
            .document(sessionId)
            .set(newState)
    }
    
    fun observeGameSession(sessionId: String): Flow<GameSession> = callbackFlow {
        val subscription = FirebaseFirestore.getInstance()
            .collection("game_sessions")
            .document(sessionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                snapshot?.toObject(GameSession::class.java)?.let {
                    trySend(it)
                }
            }
        
        awaitClose { subscription.remove() }
    }
    
    suspend fun endTurn(sessionId: String, currentPlayerId: String) {
        FirebaseFirestore.getInstance()
            .collection("game_sessions")
            .document(sessionId)
            .get()
            .await()
            .toObject(GameSession::class.java)?.let { session ->
                val playerIndex = session.players.indexOf(currentPlayerId)
                val nextPlayer = session.players[(playerIndex + 1) % session.players.size]
                
                updateGameState(sessionId, session.copy(currentTurn = nextPlayer))
            }
    }
}
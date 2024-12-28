package com.mayor.kavi.data.repository

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mayor.kavi.data.models.*
import com.mayor.kavi.util.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class GameRepositoryImpl @Inject constructor(
    private val firebaseFirestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val userRepo: UserRepository,
    context: Context
) : GameRepository {
    private var firebaseApp: FirebaseApp? = null
    private var currentSessionId: String? = ""

    init {
        initializeFirebaseIfNeeded(context)
    }

    private fun initializeFirebaseIfNeeded(context: Context) {
        if (firebaseApp == null) {
            firebaseApp = FirebaseApp.initializeApp(context)
        }
    }

    override fun getGameSessionRef(sessionId: String) =
        firebaseFirestore.collection("games")
            .document("sessions")
            .collection("active")
            .document(sessionId)

    override fun getSessionId(): String? {
        return currentSessionId
    }

    override suspend fun createGameSession(opponent: String): Result<GameSession> = try {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")

        // Get player names
        val currentUser = userRepo.getUserById(currentUserId).dataOrNull
        val opponentUser = userRepo.getUserById(opponent).dataOrNull

        val session = GameSession(
            id = UUID.randomUUID().toString(),
            players = listOf(
                PlayerInfoData(
                    id = currentUserId,
                    name = currentUser?.name ?: "Player ${currentUserId.take(4)}",
                    isReady = true
                ),
                PlayerInfoData(
                    id = opponent,
                    name = opponentUser?.name ?: "Player ${opponent.take(4)}",
                    isReady = false
                )
            ),
            currentTurn = currentUserId,
            gameMode = GameBoard.GREED.modeName,
            gameState = GameState(
                status = "pending",
                scores = mapOf(currentUserId to 0, opponent to 0),
                turnData = emptyList(),
                lastUpdate = System.currentTimeMillis()
            ),
            isGameStarted = false
        )

        getGameSessionRef(session.id).set(session).await()
        Result.Success(session)
    } catch (e: Exception) {
        Result.Error("Failed to create game session", e)
    }

    override suspend fun setPlayerReady(sessionId: String, isReady: Boolean): Result<Unit> = try {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")

        val session = getGameSession(sessionId).dataOrNull
            ?: throw Exception("Session not found")

        val updatedPlayers = session.players.map { player ->
            if (player.id == currentUserId) {
                player.copy(isReady = isReady)
            } else player
        }

        val allPlayersReady = updatedPlayers.all { it.isReady }
        val updates = mapOf(
            "players" to updatedPlayers,
            "gameState.status" to if (allPlayersReady) "active" else "waiting_for_players",
            "isGameStarted" to allPlayersReady
        )

        getGameSessionRef(sessionId).update(updates).await()

        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Failed to update player ready status", e)
    }

    override suspend fun joinGameSession(sessionId: String): Result<GameSession> {
        return try {
            val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
            val sessionDoc = getGameSessionRef(sessionId).get().await()

            if (!sessionDoc.exists()) {
                Timber.e("Game session not found")
                return Result.Error("Game session not found")
            }

            val session = sessionDoc.toObject(GameSession::class.java)
                ?: return Result.Error("Invalid session data")

            if (session.isGameStarted) {
                Timber.e("Game has already started")
                return Result.Error("Game has already started")
            }

            if (!session.players.any { it.id == currentUserId }) {
                Timber.e("You are not a participant in this game")
                return Result.Error("You are not a participant in this game")
            }

            Result.Success(session)
        } catch (e: Exception) {
            Timber.e(e, "Error joining game session")
            Result.Error("Error joining game session", e)
        }
    }

    override suspend fun getGameSession(sessionId: String): Result<GameSession> = try {
        val sessionDoc = getGameSessionRef(sessionId).get().await()
        val session =
            sessionDoc.toObject(GameSession::class.java) ?: throw Exception("Invalid session data")
        Result.Success(session)
    } catch (e: Exception) {
        Timber.e(e, "Error fetching game session")
        Result.Error("Error fetching game session", e)
    }

    override suspend fun updateGameState(sessionId: String, state: GameState): Result<Unit> = try {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
        val session = getGameSession(sessionId).dataOrNull ?: throw Exception("Session not found")

        if (session.currentTurn != currentUserId) {
            throw Exception("Not your turn")
        }

        val updatedState = session.gameState.copy(
            scores = state.scores,
            turnData = state.turnData,
            lastUpdate = System.currentTimeMillis()
        )

        getGameSessionRef(sessionId).update("gameState", updatedState).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Error updating game state")
        Result.Error("Error updating game state", e)
    }

    override suspend fun switchTurn(sessionId: String): Result<Unit> = try {
        val session = getGameSession(sessionId).dataOrNull ?: throw Exception("Session not found")
        val nextTurn = calculateNextTurn(session)
        getGameSessionRef(sessionId).update("currentTurn", nextTurn).await()
        Result.Success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Error switching turn")
        Result.Error("Error switching turn", e)
    }

    override suspend fun getScorePoints(): Result<String> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.Error("User not authenticated")
            val sessionId = getSessionId() ?: return Result.Error("No active session found")

            val sessionDoc = getGameSessionRef(sessionId).get().await()
            val session = sessionDoc.toObject(GameSession::class.java)
                ?: return Result.Error("Invalid session data")

            val score = when (session.gameMode) {
                GameBoard.PIG.modeName -> session.scores[currentUserId]?.toString() ?: "0"
                GameBoard.GREED.modeName -> session.scores[currentUserId]?.toString() ?: "0"
                GameBoard.CUSTOM.modeName -> session.scores[currentUserId]?.toString() ?: "0"
                GameBoard.BALUT.modeName -> session.scores[currentUserId]?.toString() ?: "0"
                else -> "0"
            }

            Result.Success(score)
        } catch (e: Exception) {
            Timber.e(e, "Error fetching score points")
            Result.Error("Error fetching score points", e)
        }
    }

    private fun calculateNextTurn(session: GameSession): String {
        val currentIndex = session.players.indexOfFirst { it.id == session.currentTurn }
        return if (currentIndex != -1 && session.players.isNotEmpty()) {
            val nextIndex = (currentIndex + 1) % session.players.size
            session.players[nextIndex].id
        } else {
            session.currentTurn // Fallback to current turn if something goes wrong
        }
    }

    override fun listenToGameUpdates(sessionId: String): Flow<GameSession> = callbackFlow {
        val subscription = getGameSessionRef(sessionId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            snapshot?.toObject(GameSession::class.java)?.let { gameSession ->
                trySend(gameSession)
            }
        }

        awaitClose { subscription.remove() }
    }

    override fun getOnlinePlayers(): Flow<List<UserProfile>> = callbackFlow {
        val subscription = firebaseFirestore.collection("users")
            .whereEqualTo("isOnline", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val players =
                    snapshot?.documents?.mapNotNull { it.toObject(UserProfile::class.java) }
                        ?: emptyList()
                trySend(players)
            }

        awaitClose { subscription.remove() }
    }

    override fun listenForGameInvites(): Flow<GameSession> = callbackFlow {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")

        val subscription = firebaseFirestore.collection("games")
            .document("sessions")
            .collection("active")
            .whereArrayContains("players", currentUserId)
            .whereEqualTo("gameState.status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening for game invites")
                    return@addSnapshotListener
                }

                snapshot?.documents?.forEach { doc ->
                    val session = doc.toObject(GameSession::class.java)
                    if (session != null &&
                        session.players.any { it.id == currentUserId && !it.isReady }
                    ) {
                        trySend(session)
                    }
                }
            }

        awaitClose { subscription.remove() }
    }

    override suspend fun endSession(sessionId: String): Result<Unit> = try {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")

        // Get current session
        val session = getGameSession(sessionId).dataOrNull
        if (session != null) {
            // Update session to mark it as ended
            val updatedPlayers = session.players.map { player ->
                if (player.id == currentUserId) {
                    player.copy(isReady = false)
                } else player
            }

            val updates = mapOf(
                "players" to updatedPlayers,
                "gameState.status" to "ended",
                "isGameStarted" to false
            )

            getGameSessionRef(sessionId).update(updates).await()

            // If all players have left, delete the session
            if (updatedPlayers.none { it.isReady }) {
                getGameSessionRef(sessionId).delete().await()
            }
        }
        currentSessionId = null

        Result.Success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Error ending game session")
        Result.Error("Error ending game session", e)
    }

    override suspend fun cleanupGameSession(sessionId: String): Result<Unit> = try {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")

        // Get current session
        val session = getGameSession(sessionId).dataOrNull
        if (session != null) {
            // Update session to mark it as ended
            val updatedPlayers = session.players.map { player ->
                if (player.id == currentUserId) {
                    player.copy(isReady = false)
                } else player
            }

            val updates = mapOf(
                "players" to updatedPlayers,
                "gameState.status" to "ended",
                "isGameStarted" to false
            )

            getGameSessionRef(sessionId).update(updates).await()

            // If all players have left, delete the session
            if (updatedPlayers.none { it.isReady }) {
                getGameSessionRef(sessionId).delete().await()
            }
        }

        Result.Success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Error cleaning up game session")
        Result.Error("Error cleaning up game session", e)
    }

    private fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid
}
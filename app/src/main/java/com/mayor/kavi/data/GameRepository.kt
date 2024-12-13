package com.mayor.kavi.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mayor.kavi.data.manager.DataStoreManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

interface GameRepository {
    // User Data
    fun getCurrentUserId(): String?
    suspend fun getUserById(id: String): Result<UserProfile>
    suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit>
    suspend fun getAllPlayers(): Result<List<UserProfile>>
    suspend fun searchPlayer(query: String): Result<List<UserProfile>>

    // Game Data
    suspend fun getScorePoints(): Result<String>
    suspend fun getScoreModifiers(): Result<List<String>>
    suspend fun getDiceFaces(): Result<List<String>>
    suspend fun getDiceNumbers(): Result<List<String>>
    suspend fun getDiceSideNumbers(): Result<List<String>>
    suspend fun getBackgroundColors(): Result<List<String>>

    // Game Session
    suspend fun createGameSession(opponent: String): Result<GameSession>
    suspend fun joinGameSession(sessionId: String): Result<GameSession>
    suspend fun updateGameState(sessionId: String, state: Map<String, Any>): Result<Unit>
    suspend fun listenToGameUpdates(sessionId: String): Flow<GameSession>
}

class GameRepositoryImpl @Inject constructor(
    private val firebaseFirestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val context: Context
) : GameRepository {
    private val dataStore = DataStoreManager.Companion.getInstance(context)
    override fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid

    override suspend fun getUserById(id: String): Result<UserProfile> = try {
        val snapshot = firebaseFirestore
            .collection("users")
            .document(id)
            .get()
            .await()
        val user = snapshot.toObject(UserProfile::class.java)
        if (user != null) {
            Result.success(user)
        } else {
            Result.failure(Exception("User not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit> = try {
        firebaseFirestore
            .collection("users")
            .document(userProfile.uid)
            .set(userProfile)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getAllPlayers(): Result<List<UserProfile>> = try {
        val snapshot = firebaseFirestore
            .collection("users")
            .get()
            .await()
        val players = snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
        Result.success(players)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun searchPlayer(query: String): Result<List<UserProfile>> = try {
        val snapshot = firebaseFirestore
            .collection("users")
            .whereGreaterThanOrEqualTo("username", query)
            .whereLessThanOrEqualTo("username", query + "\uf8ff")
            .get()
            .await()
        val players = snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java) }
        Result.success(players)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getScorePoints(): Result<String> = fetchGameData("score_points")

    override suspend fun getScoreModifiers(): Result<List<String>> =
        fetchGameListData("score_modifiers")

    override suspend fun getDiceFaces(): Result<List<String>> = fetchGameListData("dice_faces")

    override suspend fun getDiceNumbers(): Result<List<String>> = fetchGameListData("dice_numbers")

    override suspend fun getDiceSideNumbers(): Result<List<String>> =
        fetchGameListData("dice_side_numbers")

    override suspend fun getBackgroundColors(): Result<List<String>> =
        fetchGameListData("background_colors")

    override suspend fun createGameSession(opponent: String): Result<GameSession> = try {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
        // create new game session
        val gameSession = GameSession(
            id = UUID.randomUUID().toString(),
            players = listOf(currentUserId, opponent),
            currentTurn = currentUserId,
            gameState = mapOf(
                "status" to "waiting",
                "lastUpdate" to System.currentTimeMillis()
            ),
            scores = mapOf(
                currentUserId to 0,
                opponent to 0
            ),
            timestamp = System.currentTimeMillis()
        )
        // Save to Firestore
        firebaseFirestore
            .collection("game_sessions")
            .document(gameSession.id)
            .set(gameSession)
            .await()
        Result.success(gameSession)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun joinGameSession(sessionId: String): Result<GameSession> = try {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
        // Get the game session
        val snapshot = firebaseFirestore
            .collection("game_sessions")
            .document(sessionId)
            .get()
            .await()
        val gameSession =
            snapshot.toObject(GameSession::class.java) ?: throw Exception("Game session not found")
        // verify the user is a participant
        if (!gameSession.players.contains(currentUserId)) {
            throw Exception("User is not a participant in this game")
        }
        // update the game session to show player has joined the game
        val updatedState = gameSession.gameState.toMutableMap().apply {
            put("status", "active")
            put("lastUpdate", System.currentTimeMillis())
        }
        // update firestore
        firebaseFirestore
            .collection("game_sessions")
            .document(sessionId)
            .update("gameState", updatedState)
            .await()
        Result.success(gameSession.copy(gameState = updatedState))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateGameState(
        sessionId: String,
        state: Map<String, Any>
    ): Result<Unit> = try {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
        // Get current game session
        val snapshot = firebaseFirestore
            .collection("game_sessions")
            .document(sessionId)
            .get()
            .await()
        val gameSession = snapshot.toObject(GameSession::class.java)
            ?: throw Exception("Game session not found")
        // verify that it is the user's turn
        if (gameSession.currentTurn != currentUserId) {
            throw Exception("It is not your turn")
        }
        // update the game session
        val updatedState = gameSession.gameState.toMutableMap().apply {
            putAll(state)
            put("lastUpdate", System.currentTimeMillis())
        }
        // determine next player's turn
        val currentPlayerIndex = gameSession.players.indexOf(currentUserId)
        val nextPlayerIndex = (currentPlayerIndex + 1) % gameSession.players.size
        val nextPlayer = gameSession.players[nextPlayerIndex]
        // update firestore
        firebaseFirestore
            .collection("game_sessions")
            .document(sessionId)
            .update(
                mapOf(
                    "gameState" to updatedState,
                    "currentTurn" to nextPlayer
                )
            )
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun listenToGameUpdates(sessionId: String): Flow<GameSession> = callbackFlow {
        val currentUserId = getCurrentUserId() ?: throw Exception("User not authenticated")
        // set up real-time listener
        val listener = firebaseFirestore
            .collection("game_session")
            .document(sessionId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                snapshot?.toObject(GameSession::class.java)?.let { gameSession ->
                    // verify user is a participant
                    if (gameSession.players.contains(currentUserId)) {
                        trySend(gameSession)
                    }
                }
            }
        // clean up listener when flow is cancelled
        awaitClose {
            listener.remove()
        }
    }

    // Helper for fetching single string data
    private suspend fun fetchGameData(documentId: String): Result<String> = try {
        val snapshot = firebaseFirestore
            .collection("game_data")
            .document(documentId)
            .get()
            .await()
        val data = snapshot.getString("value")
        if (data != null) {
            Result.success(data)
        } else {
            Result.failure(Exception("Data not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Helper for fetching list data
    private suspend fun fetchGameListData(documentId: String): Result<List<String>> = try {
        val snapshot = firebaseFirestore
            .collection("game_data")
            .document(documentId)
            .get()
            .await()
        val data = snapshot["value"] as? List<String>
        if (data != null) {
            Result.success(data)
        } else {
            Result.failure(Exception("Data not found"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

package com.mayor.kavi.authentication

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mayor.kavi.data.GameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.mayor.kavi.util.Result
import com.mayor.kavi.data.UserProfile
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import timber.log.Timber

interface AuthRepository {
    fun signInUser(email: String, password: String): Flow<Result<AuthResult>>
    fun createUser(username: String, email: String, password: String): Flow<Result<AuthResult>>
    fun saveUserProfileToFirestore(profileData: UserProfile, collection: String = "users")
    fun googleSignIn(credential: AuthCredential): Flow<Result<AuthResult>>
    fun signOut()
}

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFirestore: FirebaseFirestore,
    private val gameRepository: GameRepository
) : AuthRepository {

    override fun signInUser(email: String, password: String): Flow<Result<AuthResult>> = flow {
        try {
            emit(Result.Loading(null))
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                try {
                    saveUserProfileToFirestore(
                        UserProfile(
                            uid = user.uid,
                            name = user.displayName ?: email.substringBefore('@'),
                            email = user.email ?: "",
                            image = user.photoUrl?.toString(),
                            favoriteGames = listOf(),
                            recentScores = listOf()
                        ),
                        collection = "users"
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Failed to create profile but sign-in successful")
                }
            }
            emit(Result.Success(authResult))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            emit(Result.Error("Invalid email or password", e))
        } catch (e: FirebaseAuthInvalidUserException) {
            emit(Result.Error("No account exists with this email", e))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "An unknown error occurred", e))
        }
    }

    override fun createUser(
        username: String,
        email: String,
        password: String
    ): Flow<Result<AuthResult>> = flow {
        try {
            val authResult =
                firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            saveUserProfileToFirestore(
                UserProfile(
                    uid = gameRepository.getCurrentUserId()!!,
                    name = username,
                    email = email,
                    image = "",
                    favoriteGames = listOf(),
                    recentScores = listOf()
                )
            )
            emit(Result.Success(authResult))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "An unknown error occurred", e))
        }
    }

    override fun saveUserProfileToFirestore(profileData: UserProfile, collection: String) {
        firebaseFirestore.collection(collection)
            .document(gameRepository.getCurrentUserId()!!)
            .set(profileData, SetOptions.merge())
            .addOnFailureListener { throw it }
    }

    override fun googleSignIn(credential: AuthCredential): Flow<Result<AuthResult>> = flow {
        try {
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                saveUserProfileToFirestore(
                    UserProfile(
                        uid = user.uid,
                        email = user.email ?: "",
                        image = "",
                        favoriteGames = listOf(),
                        recentScores = listOf()
                    ),
                    collection = "users"
                )
            }
            emit(Result.Success(authResult))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "An unknown error occurred", e))
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }
}

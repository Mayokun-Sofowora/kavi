package com.mayor.kavi.data.repository

import com.google.firebase.auth.*
import com.google.firebase.firestore.*
import com.mayor.kavi.data.models.*
import com.mayor.kavi.util.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFirestore: FirebaseFirestore,
    private val userRepository: UserRepository
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
                            id = user.uid,
                            name = user.displayName ?: email.substringBefore('@').replace('.', '_'),
                            email = user.email ?: "",
                            avatar = Avatar.DEFAULT,
                            lastSeen = System.currentTimeMillis()
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
                    id = userRepository.getCurrentUserId()!!,
                    name = username,
                    email = email,
                    avatar = Avatar.DEFAULT,
                    lastSeen = System.currentTimeMillis()
                )
            )
            emit(Result.Success(authResult))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "An unknown error occurred", e))
        }
    }

    override fun saveUserProfileToFirestore(profileData: UserProfile, collection: String) {
        firebaseFirestore.collection(collection)
            .document(userRepository.getCurrentUserId()!!)
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
                        id = user.uid,
                        email = user.email ?: "",
                        avatar = Avatar.DEFAULT,
                        lastSeen = System.currentTimeMillis()
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

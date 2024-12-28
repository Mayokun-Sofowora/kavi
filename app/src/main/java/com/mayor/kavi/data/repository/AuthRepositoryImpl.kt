package com.mayor.kavi.data.repository

import com.google.firebase.auth.*
import com.google.firebase.firestore.*
import com.mayor.kavi.data.models.*
import com.mayor.kavi.util.Result
import com.mayor.kavi.util.dataOrNull
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFirestore: FirebaseFirestore,
    private val userRepository: UserRepository
) : AuthRepository {

    override fun signInUser(identifier: String, password: String): Flow<Result<AuthResult>> = flow {
        try {
            emit(Result.Loading(null))
            // Check if identifier is email or username
            val email = if (identifier.contains("@")) {
                identifier
            } else {
                // Query Firestore to get email by username
                val querySnapshot = firebaseFirestore.collection("users")
                    .whereEqualTo("name", identifier)
                    .get()
                    .await()

                querySnapshot.documents.firstOrNull()?.get("email") as? String
                    ?: throw FirebaseAuthInvalidUserException(
                        "ERROR_USER_NOT_FOUND",
                        "No user found with this username"
                    )
            }

            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                // Get existing profile to preserve avatar
                val existingProfile = userRepository.getUserById(user.uid).dataOrNull
                saveUserProfileToFirestore(
                    UserProfile(
                        id = user.uid,
                        name = existingProfile?.name ?: user.displayName
                        ?: email.split("@").first(),
                        email = user.email ?: "",
                        avatar = existingProfile?.avatar ?: Avatar.DEFAULT,
                        lastSeen = System.currentTimeMillis()
                    ),
                    collection = "users"
                )
            }
            emit(Result.Success(authResult))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            emit(Result.Error("Invalid credentials", e))
        } catch (e: FirebaseAuthInvalidUserException) {
            emit(Result.Error("No account exists with this username/email", e))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "An unknown error occurred", e))
        }
    }

    override fun createUser(
        username: String,
        email: String,
        password: String,
    ): Flow<Result<AuthResult>> = flow {
        try {
            emit(Result.Loading(null))

            // Check if username is already taken
            val existingUser = firebaseFirestore.collection("users")
                .whereEqualTo("name", username)
                .get()
                .await()

            if (!existingUser.isEmpty) {
                emit(Result.Error("Username already taken"))
                return@flow
            }
            val authResult =
                firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            saveUserProfileToFirestore(
                UserProfile(
                    id = userRepository.getCurrentUserId()!!,
                    name = username,
                    email = email,
                    avatar = Avatar.DEFAULT,
                    lastSeen = System.currentTimeMillis()
                ),
                collection = "users"
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
                val existingProfile = userRepository.getUserById(user.uid).dataOrNull
                saveUserProfileToFirestore(
                    UserProfile(
                        id = user.uid,
                        name = (existingProfile?.name ?: user.displayName
                        ?: user.email?.substringBefore('@')
                            ?.replace('.', '_')).toString(),
                        email = user.email ?: "",
                        avatar = existingProfile?.avatar ?: Avatar.DEFAULT,
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

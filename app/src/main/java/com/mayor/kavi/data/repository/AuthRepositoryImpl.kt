package com.mayor.kavi.data.repository

import com.google.firebase.auth.*
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.*
import com.mayor.kavi.data.models.*
import com.mayor.kavi.util.Result
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFirestore: FirebaseFirestore,
    private val userRepository: UserRepository
) : AuthRepository {

    /**
     * Checks if a user is signed in and has a valid profile.
     * This ensures that either the email is verified or the user has signed in using Google.
     */
    override fun isUserSignedIn(): Boolean {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) return false

        // Allow both Google Sign-In and email/password users
        val isGoogleUser =
            currentUser.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }

        // If not a Google user and email isn't verified, sign out
        if (!isGoogleUser && !currentUser.isEmailVerified) {
            signOut()
            return false
        }

        // Check if user exists in Firestore
        try {
            firebaseFirestore.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (!document.exists()) {
                        signOut()
                    } else {
                        // Update online status only if user is verified and has a profile
                        if (isGoogleUser || currentUser.isEmailVerified) {
                            firebaseFirestore.collection("users")
                                .document(currentUser.uid)
                                .update(
                                    mapOf(
                                        "lastSeen" to System.currentTimeMillis(),
                                        "isOnline" to true
                                    )
                                )
                        }
                    }
                }
                .addOnFailureListener {
                    signOut()
                }
            return true
        } catch (_: Exception) {
            signOut()
            return false
        }
    }

    override fun signInUser(identifier: String, password: String): Flow<Result<AuthResult>> = flow {
        try {
            emit(Result.Loading(null))
            // Determine if the identifier is an email or username
            val email = if (identifier.contains("@")) {
                identifier
            } else {
                val querySnapshot = firebaseFirestore.collection("users")
                    .whereEqualTo("name", identifier)
                    .get()
                    .await()
                querySnapshot.documents.firstOrNull()?.getString("email")
                    ?: throw FirebaseAuthInvalidUserException(
                        "ERROR_USER_NOT_FOUND",
                        "No user found with this username"
                    )
            }

            // Sign in with email and password
            signOut() // Ensure any previous session is cleared
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Authentication failed")

            // Check if email is verified for non-Google users
            val isGoogleUser =
                user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
            if (!isGoogleUser && !user.isEmailVerified) {
                signOut() // Sign out since email isn't verified
                user.sendEmailVerification().await()
                emit(Result.Error("Please verify your email address. A verification email has been sent."))
                return@flow
            }

            // Check if user exists in Firestore
            val userDoc = firebaseFirestore.collection("users")
                .document(user.uid)
                .get()
                .await()

            if (!userDoc.exists()) {
                signOut() // Sign out since profile doesn't exist
                emit(Result.Error("No user profile found. Please create an account first."))
                return@flow
            }

            // Update the last seen timestamp and online status
            firebaseFirestore.collection("users")
                .document(user.uid)
                .update(
                    mapOf(
                        "lastSeen" to System.currentTimeMillis(),
                        "isOnline" to true
                    )
                )
                .await()

            emit(Result.Success(authResult))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            emit(Result.Error("Invalid credentials", e))
        } catch (e: FirebaseAuthInvalidUserException) {
            emit(Result.Error("No account exists with this username/email", e))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "An unknown error occurred", e))
        }
    }.flowOn(Dispatchers.IO)

    override fun createUser(
        username: String,
        email: String,
        password: String,
    ): Flow<Result<AuthResult>> = flow {
        try {
            emit(Result.Loading(null))
            // Check for username uniqueness
            val existingUser = firebaseFirestore.collection("users")
                .whereEqualTo("name", username)
                .get()
                .await()
            if (!existingUser.isEmpty) {
                emit(Result.Error("Username already taken"))
                return@flow
            }
            // Create the user
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Failed to create user")

            // Send email verification
            user.sendEmailVerification().await()

            // Save profile to Firestore
            val profile = UserProfile(
                id = user.uid,
                name = username,
                email = email,
                avatar = Avatar.DEFAULT,
                lastSeen = System.currentTimeMillis(),
                isOnline = true,
                isInGame = false,
                isWaitingForPlayers = false,
                currentGameId = ""
            )
            firebaseFirestore.collection("users")
                .document(user.uid)
                .set(profile)
                .await()

            emit(Result.Success(authResult))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "An unknown error occurred", e))
        }
    }

    override fun saveUserProfileToFirestore(profileData: UserProfile, collection: String) {
        val userId =
            userRepository.getCurrentUserId() ?: throw IllegalStateException("User not signed in")
        firebaseFirestore.collection(collection)
            .document(userId)
            .set(profileData, SetOptions.merge())
            .addOnFailureListener { throw it }
    }

    override fun googleSignIn(credential: AuthCredential): Flow<Result<AuthResult>> = flow {
        emit(Result.Loading(null))
        try {
            signOut() // Ensure any previous session is cleared
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Authentication failed")

            val userDoc = firebaseFirestore.collection("users")
                .document(user.uid)
                .get()
                .await()

            if (!userDoc.exists()) {
                // Create a profile for the Google user
                val profile = UserProfile(
                    id = user.uid,
                    name = user.displayName ?: throw Exception("No display name provided"),
                    email = user.email ?: throw Exception("No email provided"),
                    avatar = Avatar.DEFAULT,
                    lastSeen = System.currentTimeMillis(),
                    isOnline = true,
                    isInGame = false,
                    isWaitingForPlayers = false,
                    currentGameId = ""
                )

                firebaseFirestore.collection("users")
                    .document(user.uid)
                    .set(profile)
                    .await()
            } else {
                // Update the last seen timestamp and online status
                firebaseFirestore.collection("users")
                    .document(user.uid)
                    .update(
                        mapOf(
                            "lastSeen" to System.currentTimeMillis(),
                            "isOnline" to true
                        )
                    )
                    .await()
            }

            emit(Result.Success(authResult))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Authentication failed", e))
        }
    }.flowOn(Dispatchers.IO)

    override fun signOut() {
        try {
            firebaseAuth.signOut()
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                firebaseFirestore.clearPersistence().addOnSuccessListener {
                    for (userInfo in currentUser.providerData) {
                        if (userInfo.providerId == GoogleAuthProvider.PROVIDER_ID) {
                            // Force refresh to clear any cached credentials
                            currentUser.getIdToken(true)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Log but don't throw as we still want to complete sign out
            println("Error during sign out: ${e.message}")
        }
    }
}

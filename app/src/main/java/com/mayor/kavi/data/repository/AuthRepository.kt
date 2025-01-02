package com.mayor.kavi.data.repository

import com.google.firebase.auth.*
import kotlinx.coroutines.flow.Flow
import com.mayor.kavi.util.Result
import com.mayor.kavi.data.models.UserProfile

interface AuthRepository {
    fun isUserSignedIn(): Boolean
    fun signInUser(identifier: String, password: String): Flow<Result<AuthResult>>
    fun createUser(username: String, email: String, password: String): Flow<Result<AuthResult>>
    fun saveUserProfileToFirestore(profileData: UserProfile, collection: String = "users")
    fun googleSignIn(credential: AuthCredential): Flow<Result<AuthResult>>
    fun signOut()
}

package com.mayor.kavi.authentication.signin

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.mayor.kavi.data.repository.AuthRepository
import com.mayor.kavi.data.repository.UserRepository
import com.mayor.kavi.util.Result.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.mayor.kavi.util.Result
import com.mayor.kavi.data.models.UserProfile
import com.google.firebase.auth.GoogleAuthProvider

data class SignInState(
    val isLoading: Boolean = false,
    val toastMessage: String? = null,
    val isAuthSuccess: AuthResult? = null
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _signInState = MutableStateFlow(SignInState())
    val signInState: StateFlow<SignInState> = _signInState

    fun signIn(
        email: String,
        password: String,
    ) = viewModelScope.launch {
        // Input validation
        if (!validateInput(email, password)) return@launch
        _signInState.value = SignInState(isLoading = true)
        repository.signInUser(email, password).collect { result ->
            handleSignInResult(result)
        }
    }

    fun googleSignIn(
        credential: AuthCredential
    ) = viewModelScope.launch {
        _signInState.value = SignInState(isLoading = true)
        repository.googleSignIn(credential).collect { result ->
            handleSignInResult(result)
        }
    }

    private fun handleSignInResult(
        result: Result<AuthResult>
    ) {
        when (result) {
            is Success -> {
                val user = result.data.user
                if (user != null && !user.isEmailVerified && 
                    user.providerData.none { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
                    // Email user that hasn't verified their email
                    _signInState.value = SignInState(
                        isLoading = false,
                        toastMessage = "Please verify your email before signing in. Check your inbox for the verification link."
                    )
                } else {
                    _signInState.value = SignInState(
                        isLoading = false,
                        isAuthSuccess = result.data,
                        toastMessage = "Sign in successful"
                    )
                }
            }

            is Loading -> {
                _signInState.value = SignInState(isLoading = true)
            }

            is Error -> {
                _signInState.value = SignInState(
                    isLoading = false,
                    toastMessage = result.message
                )
            }
        }
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        return when (val result = userRepository.getUserById(userId)) {
            is Success -> result.data
            else -> null
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isBlank() || password.isBlank()) {
            _signInState.value = SignInState(
                isLoading = false,
                toastMessage = "Email and password are required"
            )
            return false
        }
        if (!isValidEmail(email)) {
            _signInState.value = SignInState(
                isLoading = false,
                toastMessage = "Invalid email format"
            )
            return false
        }
        return true
    }

    private fun isValidEmail(email: String): Boolean {
        return try {
            Patterns.EMAIL_ADDRESS.matcher(email).matches()
        } catch (_: Exception) {
            // Fallback for tests or when Patterns is not available
            email.matches(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"))
        }
    }
}

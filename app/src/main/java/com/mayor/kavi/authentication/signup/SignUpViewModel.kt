package com.mayor.kavi.authentication.signup

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mayor.kavi.data.repository.AuthRepository
import com.mayor.kavi.data.models.Avatar
import com.mayor.kavi.data.models.UserProfile
import com.mayor.kavi.data.repository.UserRepository
import com.mayor.kavi.util.Result.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignUpState(
    val isLoading: Boolean = false,
    val toastMessage: String? = null
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _signUpState = MutableStateFlow(SignUpState())
    val signUpState: StateFlow<SignUpState> = _signUpState

    fun signUp(username: String, email: String, password: String) {
        // Validate input
        if (!validateInput(username, email, password)) return
        viewModelScope.launch {
            try {
                repository.createUser(username, email, password).collect { result ->
                    when (result) {
                        is Success -> {
                            val userId = userRepository.getCurrentUserId()
                            if (userId.isNullOrEmpty()) {
                                _signUpState.value = SignUpState(
                                    isLoading = false,
                                    toastMessage = "Sign-up failed: Unable to fetch user ID"
                                )
                                return@collect
                            }
                            val userProfile = UserProfile(
                                id = userId,
                                name = username,
                                email = email,
                                avatar = Avatar.DEFAULT,
                                lastSeen = System.currentTimeMillis(),
                                isOnline = true,
                                isInGame = false,
                                isWaitingForPlayers = false,
                                currentGameId = ""
                            )
                            createUserProfile(userProfile)
                            // Show verification message
                            _signUpState.value = SignUpState(
                                isLoading = false,
                                toastMessage = "Account created successfully! Please check your email to verify your account before signing in."
                            )
                        }
                        is Error -> {
                            _signUpState.value = SignUpState(
                                isLoading = false,
                                toastMessage = "Sign-up failed: ${result.message}"
                            )
                        }
                        is Loading -> {
                            _signUpState.value = SignUpState(isLoading = true)
                        }
                    }
                }
            } catch (e: Exception) {
                _signUpState.value = SignUpState(
                    isLoading = false,
                    toastMessage = "Sign-up failed: ${e.message}"
                )
            }
        }
    }

    private fun validateInput(username: String, email: String, password: String): Boolean {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _signUpState.value = SignUpState(
                isLoading = false,
                toastMessage = "All fields are required"
            )
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _signUpState.value = SignUpState(
                isLoading = false,
                toastMessage = "Invalid email format"
            )
            return false
        }
        if (password.length < 6) {
            _signUpState.value = SignUpState(
                isLoading = false,
                toastMessage = "Password must be at least 6 characters"
            )
            return false
        }
        return true
    }

    private fun createUserProfile(userProfile: UserProfile) {
        viewModelScope.launch {
            try {
                when (val result = userRepository.updateUserProfile(userProfile)) {
                    is Success -> {
                        _signUpState.value = SignUpState(
                            isLoading = false,
                            toastMessage = "Sign-up successful"
                        )
                    }
                    is Error -> {
                        _signUpState.value = SignUpState(
                            isLoading = false,
                            toastMessage = "Failed to save user profile: ${result.message}"
                        )
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _signUpState.value = SignUpState(
                    isLoading = false,
                    toastMessage = "Failed to save user profile: ${e.message}"
                )
            }
        }
    }
}
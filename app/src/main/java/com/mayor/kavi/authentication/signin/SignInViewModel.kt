package com.mayor.kavi.authentication.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.mayor.kavi.authentication.AuthRepository
import com.mayor.kavi.data.GameRepository
import com.mayor.kavi.ui.Routes
import com.mayor.kavi.util.Result
import com.mayor.kavi.util.Result.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignInState(
    val isLoading: Boolean = false,
    val toastMessage: String? = null,
    val isAuthSuccess: AuthResult? = null
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _signInState = MutableStateFlow(SignInState())
    val signInState: StateFlow<SignInState> = _signInState

    fun signIn(email: String, password: String, navController: NavController) = viewModelScope.launch {
        _signInState.value = SignInState(isLoading = true)
        repository.signInUser(email, password).collect { result ->
            _signInState.value = when (result) {
                is Success -> {
                    navController.navigate(Routes.MainMenu.route)
                    SignInState(isLoading = false, toastMessage = "Sign in successful")
                }
                is Loading -> SignInState(isLoading = true)
                is Error -> SignInState(
                    isLoading = false,
                    toastMessage = result.message
                )
            }
        }
    }

    fun googleSignIn(credential: AuthCredential, navController: NavController) = viewModelScope.launch {
        _signInState.value = SignInState(isLoading = true)
        repository.googleSignIn(credential).collect { result ->
            when (result) {
                is Success -> {
                    _signInState.value = SignInState(
                        isLoading = false,
                        isAuthSuccess = result.data,
                        toastMessage = "Google sign-in successful"
                    )
                    navController.navigate(Routes.MainMenu.route)
                }
                is Loading -> {
                    _signInState.value = SignInState(isLoading = true)
                }
                is Error -> {
                    _signInState.value = SignInState(
                        isLoading = false,
                        toastMessage = "Google sign-in failed: ${result.message}"
                    )
                }
            }
        }
    }

    // Methods for handling login states
    fun onLoginStart() {
        _signInState.value = SignInState(isLoading = true)
    }

    fun onLoginComplete() {
        _signInState.value = SignInState(isLoading = false)
    }

    fun onLoginCancel() {
        _signInState.value = SignInState(isLoading = false)
    }
    fun updateSignInStateWithError(message: String) {
        _signInState.value = SignInState(
            isLoading = false,
            toastMessage = message
        )
    }
}


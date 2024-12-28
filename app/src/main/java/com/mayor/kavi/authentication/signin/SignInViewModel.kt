package com.mayor.kavi.authentication.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.mayor.kavi.data.repository.AuthRepository
import com.mayor.kavi.ui.Routes
import com.mayor.kavi.ui.viewmodel.AppViewModel
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

    fun signIn(
        email: String,
        password: String,
        navController: NavController,
        appViewModel: AppViewModel
    ) = viewModelScope.launch {
        _signInState.value = SignInState(isLoading = true)
        repository.signInUser(email, password).collect { result ->
            _signInState.value = when (result) {
                is Success -> {
                    appViewModel.onLoginComplete()
                    navController.navigate(Routes.MainMenu.route)
                    SignInState(isLoading = false, toastMessage = "Sign in successful")
                }
                is Loading -> SignInState(isLoading = true)
                is Error -> {
                    appViewModel.onLoginCancel()
                    SignInState(
                        isLoading = false,
                        toastMessage = result.message
                    )
                }
            }
        }
    }

    fun googleSignIn(
        credential: AuthCredential,
        appViewModel: AppViewModel
    ) = viewModelScope.launch {
        _signInState.value = SignInState(isLoading = true)
        repository.googleSignIn(credential).collect { result ->
            when (result) {
                is Success -> {
                    _signInState.value = SignInState(
                        isLoading = false,
                        isAuthSuccess = result.data,
                        toastMessage = "Google sign-in successful"
                    )
                    appViewModel.onLoginComplete()
                }

                is Loading -> {
                    _signInState.value = SignInState(isLoading = true)
                }

                is Error -> {
                    _signInState.value = SignInState(
                        isLoading = false,
                        toastMessage = "Google sign-in failed: ${result.message}"
                    )
                    appViewModel.onLoginCancel()
                }
            }
        }
    }

    fun updateSignInStateWithError(message: String) {
        _signInState.value = SignInState(
            isLoading = false,
            toastMessage = message
        )
    }
}
package com.mayor.kavi.authentication.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mayor.kavi.authentication.AuthRepository
import com.mayor.kavi.ui.Routes
import com.mayor.kavi.util.Result
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
    private val repository: AuthRepository
) : ViewModel() {

    private val _signUpState = MutableStateFlow(SignUpState())
    val signUpState: StateFlow<SignUpState> = _signUpState

    fun signUp(username: String, email: String, password: String, navController: NavController) {
        // Launch a coroutine to perform sign-up operation
        viewModelScope.launch {
            // Collect the result of the flow returned by createUser
            repository.createUser(username, email, password).collect { result ->
                when (result) {
                    is Success -> {
                        navController.navigate(Routes.MainMenu.route)
                        _signUpState.value = SignUpState(
                            isLoading = false,
                            toastMessage = "Sign-up successful"
                        )
                    }

                    is Error -> {
                        _signUpState.value = SignUpState(
                            isLoading = false,
                            toastMessage = "Sign-up failed: ${result.message}"
                        )
                    }
                    // The flow handles loading internally.
                    is Loading -> {
                        _signUpState.value = SignUpState(isLoading = true)
                    }
                }
            }
        }
    }
}

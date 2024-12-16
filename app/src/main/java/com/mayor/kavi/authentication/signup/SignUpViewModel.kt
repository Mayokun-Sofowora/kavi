package com.mayor.kavi.authentication.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mayor.kavi.authentication.AuthRepository
import com.mayor.kavi.data.Avatar
import com.mayor.kavi.data.GameRepository
import com.mayor.kavi.data.UserProfile
import com.mayor.kavi.ui.Routes
import com.mayor.kavi.ui.viewmodel.AppViewModel
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
    private val repository: AuthRepository,
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _signUpState = MutableStateFlow(SignUpState())
    val signUpState: StateFlow<SignUpState> = _signUpState

    fun signUp(
        username: String,
        email: String,
        password: String,
        navController: NavController,
        appViewModel: AppViewModel
    ) {
        // Launch a coroutine to perform sign-up operation
        viewModelScope.launch {
            // Collect the result of the flow returned by createUser
            repository.createUser(username, email, password).collect { result ->
                when (result) {
                    is Success -> {
                        val userId = gameRepository.getCurrentUserId()
                        val userProfile = UserProfile(
                            id = userId.toString(),
                            name = username,
                            email = email,
                            avatar = Avatar.DEFAULT, // set default avatar
                            favoriteGames = emptyList()
                        )
                        appViewModel.createUserProfile(userProfile)
                        _signUpState.value = SignUpState(
                            isLoading = false,
                            toastMessage = "Sign-up successful"
                        )
                        appViewModel.onLoginComplete()

                    }

                    is Error -> {
                        _signUpState.value = SignUpState(
                            isLoading = false,
                            toastMessage = "Sign-up failed: ${result.message}"
                        )
                        appViewModel.onLoginCancel()
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
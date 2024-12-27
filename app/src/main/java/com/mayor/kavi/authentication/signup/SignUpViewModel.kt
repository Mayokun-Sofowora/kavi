package com.mayor.kavi.authentication.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mayor.kavi.data.repository.AuthRepository
import com.mayor.kavi.data.models.Avatar
import com.mayor.kavi.data.models.UserProfile
import com.mayor.kavi.data.repository.UserRepository
import com.mayor.kavi.ui.viewmodel.AppViewModel
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
    private val userRepository: UserRepository
) : ViewModel() {

    private val _signUpState = MutableStateFlow(SignUpState())
    val signUpState: StateFlow<SignUpState> = _signUpState

    fun signUp(
        username: String,
        email: String,
        password: String,
        appViewModel: AppViewModel
    ) {
        // Launch a coroutine to perform sign-up operation
        viewModelScope.launch {
            // Collect the result of the flow returned by createUser
            repository.createUser(username, email, password).collect { result ->
                when (result) {
                    is Success -> {
                        val userId = userRepository.getCurrentUserId()
                        val userProfile = UserProfile(
                            id = userId.toString(),
                            name = username,
                            email = email,
                            avatar = Avatar.DEFAULT,
                            lastSeen = System.currentTimeMillis()
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
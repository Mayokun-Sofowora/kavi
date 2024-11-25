package com.mayor.kavi.ui.viewmodel

import androidx.lifecycle.*
import com.mayor.kavi.data.dao.UserEntity
import com.mayor.kavi.data.models.GameType
import com.mayor.kavi.data.models.User
import com.mayor.kavi.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.mayor.kavi.utils.Result
import kotlinx.coroutines.flow.*

/***
 * Purpose: Manage user-related data, such as current user profiles and preferences.
 * Responsibilities:
 * Fetch or create a user based on the username provided in the "Classic Mode" screen.
 * Store user-specific preferences and update them when needed.
 * Provide user data to screens like profile, achievements, and settings.
 */
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Initial)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _userName = MutableStateFlow<String>("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _selectedMode = MutableStateFlow<GameType>(GameType.COMPUTER_AI)
    val selectedMode: StateFlow<GameType> = _selectedMode.asStateFlow()

    fun setUserName(name: String) {
        _userName.value = name
    }

    fun setGameType(mode: GameType) {
        _selectedMode.value = mode
    }

    init {
        observeCurrentUser()
    }

    private fun observeCurrentUser() {
        viewModelScope.launch {
            userRepository.getCurrentUser()
                .catch { e ->
                    _uiState.value = UserUiState.Error(e.localizedMessage ?: "User fetch failed")
                    _currentUser.value = null
                }
                .collect { user ->
                    _currentUser.value = user
                    _uiState.value = UserUiState.Success("User loaded")
                }
        }
    }

    fun saveUser(user: UserEntity) {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            val result = userRepository.saveUser(user)
            when (result) {
                is Result.Success -> {
                    _uiState.value = UserUiState.Success("User saved successfully")
                    observeCurrentUser() // Refresh user data
                }

                is Result.Error -> {
                    _uiState.value =
                        UserUiState.Error(result.exception.localizedMessage ?: "Save failed")
                }
            }
        }
    }

    fun updateUserPreferences(preferences: String) {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            val currentUser = _currentUser.value
            if (currentUser != null) {
                val updatedUser = currentUser.copy(preferences = preferences)
                val result = userRepository.saveUser(
                    UserEntity(
                        id = updatedUser.id,
                        username = updatedUser.username,
                        preferences = updatedUser.preferences
                    )
                )
                when (result) {
                    is Result.Success -> {
                        _uiState.value = UserUiState.Success("Preferences updated")
                        _currentUser.value = updatedUser
                    }

                    is Result.Error -> {
                        _uiState.value = UserUiState.Error(
                            result.exception.localizedMessage ?: "Preferences update failed"
                        )
                    }
                }
            } else {
                _uiState.value = UserUiState.Error("No current user")
            }
        }
    }

    fun clearCurrentUser() {
        viewModelScope.launch {
            userRepository.clearCurrentUser()
            _currentUser.value = null
            _uiState.value = UserUiState.Initial
        }
    }

    fun authenticate(username: String) {
        viewModelScope.launch {
            _uiState.value = UserUiState.Loading
            val result = userRepository.authenticate(username)
            when (result) {
                is Result.Success -> {
                    _uiState.value = UserUiState.Success("Authentication successful")
                    _currentUser.value = result.data
                }

                is Result.Error -> {
                    _uiState.value = UserUiState.Error(
                        result.exception.localizedMessage ?: "Authentication failed"
                    )
                }
            }
        }
    }

    // Sealed class for UI state management
    sealed class UserUiState {
        object Initial : UserUiState()
        object Loading : UserUiState()
        data class Success(val message: String) : UserUiState()
        data class Error(val message: String) : UserUiState()
    }
}

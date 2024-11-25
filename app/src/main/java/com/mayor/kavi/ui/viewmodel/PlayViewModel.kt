package com.mayor.kavi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mayor.kavi.data.dao.UserEntity
import com.mayor.kavi.data.repository.UserRepository
import com.mayor.kavi.utils.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _selectedMode = MutableStateFlow<String>("")
    val selectedMode: StateFlow<String> = _selectedMode.asStateFlow()

    // LiveData to observe user data or game state
    private val _userData = MutableStateFlow<UserEntity?>(null)
    val userData: StateFlow<UserEntity?> = _userData.asStateFlow()

    private val _gameModes = MutableStateFlow<List<String>>(emptyList())
    val gameModes: StateFlow<List<String>> = _gameModes.asStateFlow()

    private val _uiState = MutableStateFlow<PlayUiState>(PlayUiState.Initial)
    val uiState: StateFlow<PlayUiState> = _uiState.asStateFlow()

    init {
        loadGameModes()
    }

    fun selectMode(mode: String) {
        _selectedMode.value = mode
    }

    fun fetchUserData() {
        viewModelScope.launch {
            try {
                _uiState.value = PlayUiState.Loading
                // Collect the current user once
                val user = userRepository.getCurrentUser().first()

                user?.let {
                    _userData.value = it.toEntity()
                    _uiState.value = PlayUiState.Success
                } ?: run {
                    _uiState.value = PlayUiState.Error("No user found")
                }
            } catch (e: Exception) {
                _uiState.value = PlayUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun saveUserData(userEntity: UserEntity) {
        viewModelScope.launch {
            try {
                // Convert UserData to UserEntity if needed
                userRepository.saveUser(userEntity)
                _uiState.value = PlayUiState.Success
            } catch (e: Exception) {
                _uiState.value = PlayUiState.Error(e.localizedMessage ?: "Save failed")
            }
        }
    }

    private fun loadGameModes() {
        // Load available game modes
        _gameModes.value = listOf("Classic Mode", "AR Mode")
    }

    // Sealed class for UI state management
    sealed class PlayUiState {
        object Initial : PlayUiState()
        object Loading : PlayUiState()
        object Success : PlayUiState()
        data class Error(val message: String) : PlayUiState()
    }
}
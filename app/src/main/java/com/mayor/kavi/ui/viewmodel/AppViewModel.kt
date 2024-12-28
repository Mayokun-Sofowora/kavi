package com.mayor.kavi.ui.viewmodel

import androidx.lifecycle.*
import com.mayor.kavi.data.manager.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import javax.inject.Inject
import com.mayor.kavi.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.mayor.kavi.data.models.UserProfile
import com.mayor.kavi.data.repository.UserRepository
import com.mayor.kavi.util.Result.*

@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository
) : ViewModel() {
    private val diceDataStore = DataStoreManager.getInstance(context)

    // User state
    private val _userProfileState = MutableStateFlow<Result<UserProfile>>(Loading())
    val userProfileState: StateFlow<Result<UserProfile>> = _userProfileState

    private val _loginComplete = MutableStateFlow(false)
    val loginComplete: StateFlow<Boolean> = _loginComplete.asStateFlow()

    init {
        viewModelScope.launch {
            loadUserProfile()
        }
    }

    // Settings methods
    fun setInterfaceMode(mode: String) = viewModelScope.launch(Dispatchers.IO) {
        diceDataStore.setInterfaceMode(mode)
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _userProfileState.value = Loading() // Indicate loading
            val userId = userRepository.getCurrentUserId()

            if (userId != null) {
                val result = userRepository.getUserById(userId)
                _userProfileState.emit(result)

            } else {
                _userProfileState.emit(Error("Not logged in"))
            }
        }
    }

    fun updateUserProfile(profile: UserProfile) = viewModelScope.launch {
        _userProfileState.value = Loading(profile) // Set loading state with profile data

        val result = userRepository.updateUserProfile(profile)
        _userProfileState.value = when (result) {
            is Success -> {
                Success(profile)
            }
            is Error -> {
                Error(result.message, result.exception, profile)
            }
            else -> {
                Error("Unknown error", data = profile)
            }
        }
    }

    // Login methods
    fun updateLogin() {
        _loginComplete.value = true
    }

    fun onLoginComplete() {
        _loginComplete.value = false
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setUserOnlineStatus(true)
            loadUserProfile() // Load profile after setting online status
        }
    }

    fun onLoginCancel() {
        _loginComplete.value = false
    }

    fun createUserProfile(userProfile: UserProfile) = viewModelScope.launch {
        _userProfileState.value = Loading()
        val result = userRepository.updateUserProfile(userProfile)

        _userProfileState.value = when (result) {
            is Success -> {
                Success(userProfile)
            }
            is Error -> {
                Error(result.message, result.exception)
            }
            else -> {
                Loading()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.setUserOnlineStatus(false)
        }
    }

}
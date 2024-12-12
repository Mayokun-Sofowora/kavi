package com.mayor.kavi.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.mayor.kavi.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.mayor.kavi.util.Result

@HiltViewModel
class AppViewModel @Inject constructor(
    application: Application,
    private val gameRepository: GameRepository
) : AndroidViewModel(application) {
    private val diceDataStore = DataStoreManager.getInstance(application)
    
    // App-wide settings and modes
    private val _appTheme = diceDataStore.getThemeMode().asLiveData()
    val appTheme: LiveData<String> = _appTheme

    private val _interfaceMode = diceDataStore.getInterfaceMode().asLiveData()
    val interfaceMode: LiveData<String> = _interfaceMode

    private val _playMode = diceDataStore.getPlayMode().asLiveData()
    val playMode: LiveData<String> = _playMode

    // User state
    private val _userProfileState = MutableStateFlow<Result<UserProfile>>(Result.Loading(null))
    val userProfileState: StateFlow<Result<UserProfile>> = _userProfileState

    private val _loginComplete = MutableLiveData(false)
    val loginComplete: LiveData<Boolean> = _loginComplete

    init {
        Timber.i("AppViewModel created")
        loadUserProfile()
    }

    // Settings methods
    fun setInterfaceMode(mode: String) = viewModelScope.launch(Dispatchers.IO) {
        diceDataStore.setInterfaceMode(mode)
    }

    fun setPlayMode(mode: String) = viewModelScope.launch(Dispatchers.IO) {
        diceDataStore.setPlayMode(mode)
    }

    fun setSelectedBoard(board: String) = viewModelScope.launch(Dispatchers.IO) {
        diceDataStore.setSelectedBoard(board)
    }

    fun setAppTheme(theme: String) = viewModelScope.launch(Dispatchers.IO) {
        diceDataStore.setThemeMode(theme)
    }

    // User profile methods
    fun loadUserProfile() = viewModelScope.launch {
        _userProfileState.value = Result.Loading(null)
        
        try {
            val currentUserId = gameRepository.getCurrentUserId()
            if (currentUserId == null) {
                _userProfileState.value = Result.Error("User not logged in", null)
                return@launch
            }

            gameRepository.getUserById(currentUserId).fold(
                onSuccess = { profile ->
                    _userProfileState.value = Result.Success(profile)
                },
                onFailure = { exception ->
                    _userProfileState.value = Result.Error(
                        exception.message ?: "Failed to load user profile",
                        exception
                    )
                    Timber.e(exception)
                }
            )
        } catch (e: Exception) {
            _userProfileState.value = Result.Error("Failed to load user profile", e)
            Timber.e(e)
        }
    }

    fun updateUserProfile(userProfile: UserProfile) = viewModelScope.launch {
        _userProfileState.value = Result.Loading(null)
        
        gameRepository.updateUserProfile(userProfile).fold(
            onSuccess = {
                _userProfileState.value = Result.Success(userProfile)
            },
            onFailure = { exception ->
                _userProfileState.value = Result.Error(
                    exception.message ?: "Failed to update user profile",
                    exception
                )
                Timber.e(exception)
            }
        )
    }

    // Login methods
    fun updateLogin() {
        _loginComplete.value = true
    }

    fun onLoginComplete() {
        _loginComplete.value = false
    }

    fun onLoginCancel() {
        _loginComplete.value = false
    }

    fun createUserProfile(userProfile: UserProfile) = viewModelScope.launch {
        _userProfileState.value = Result.Loading(null)
        gameRepository.updateUserProfile(userProfile).fold(
            onSuccess = {
                _userProfileState.value = Result.Success(userProfile)
            },
            onFailure = { exception ->
                _userProfileState.value = Result.Error(
                    exception.message ?: "Failed to create user profile",
                    exception
                )
            }
        )
    }
} 
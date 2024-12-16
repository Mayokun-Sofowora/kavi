package com.mayor.kavi.ui.viewmodel

import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.mayor.kavi.data.*
import com.mayor.kavi.data.manager.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import com.mayor.kavi.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.mayor.kavi.util.dataOrNull

@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val gameRepository: GameRepository
) : ViewModel() {
    private val diceDataStore = DataStoreManager.getInstance(context)

    // App-wide settings and modes
    val interfaceMode: StateFlow<String> = diceDataStore.getInterfaceMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    // User state
    private val _userProfileState: MutableStateFlow<Result<UserProfile>> =
        MutableStateFlow(Result.Loading(null))
    val userProfileState: StateFlow<Result<UserProfile>> = _userProfileState.asStateFlow()

    private val _loginComplete = MutableStateFlow(false)
    val loginComplete: StateFlow<Boolean> = _loginComplete.asStateFlow()

    init {
        loadUserProfile()
    }

    // Settings methods
    fun setInterfaceMode(mode: String) = viewModelScope.launch(Dispatchers.IO) {
        diceDataStore.setInterfaceMode(mode)
    }

    fun loadUserProfile() = viewModelScope.launch {
        _userProfileState.value = Result.Loading(_userProfileState.value.dataOrNull)
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

    fun updateUserProfile(profile: UserProfile) = viewModelScope.launch {
        _userProfileState.value = Result.Loading(profile)
        gameRepository.updateUserProfile(profile).fold(
            onSuccess = {loadUserProfile()},
            onFailure = {exception ->
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
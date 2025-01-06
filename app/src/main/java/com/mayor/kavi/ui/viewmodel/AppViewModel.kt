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
import com.mayor.kavi.data.repository.AuthRepository
import com.mayor.kavi.data.repository.UserRepository
import com.mayor.kavi.util.Result.*
import com.mayor.kavi.data.manager.StatisticsManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    val authRepository: AuthRepository,
    private val statisticsManager: StatisticsManager,
    private val firebaseFirestore: FirebaseFirestore
) : ViewModel() {

    private val diceDataStore = DataStoreManager.getInstance(context)

    private val _userProfileState = MutableStateFlow<Result<UserProfile>>(Result.Loading())
    val userProfileState: StateFlow<Result<UserProfile>> = _userProfileState.asStateFlow()

    private val _isUserSignedIn = MutableStateFlow(authRepository.isUserSignedIn())
    val isUserSignedIn: StateFlow<Boolean> = _isUserSignedIn.asStateFlow()

    private val _loginComplete = MutableStateFlow(false)
    val loginComplete: StateFlow<Boolean> = _loginComplete.asStateFlow()

    private val _modelRetrainingStatus = MutableStateFlow<String?>(null)
    val modelRetrainingStatus: StateFlow<String?> = _modelRetrainingStatus.asStateFlow()

    init {
        viewModelScope.launch {
            loadUserProfile()
            // Listen for model retraining updates
            statisticsManager.modelTrainingStatus.collect { status ->
                _modelRetrainingStatus.value = status
            }
        }
    }

    // Settings methods
    fun setInterfaceMode(mode: String) = viewModelScope.launch(Dispatchers.IO) {
        diceDataStore.setInterfaceMode(mode)
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _userProfileState.value = Loading()
            _modelRetrainingStatus.value = null
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
        val result = userRepository.updateUserProfile(profile)
        _userProfileState.value = when (result) {
            is Success -> Success(profile)
            is Error -> Error(result.message, result.exception, profile)
            else -> Error("Unknown error", data = profile)
        }
    }

    fun onSignInComplete() {
        viewModelScope.launch {
            _userProfileState.value = Loading()
            delay(500) // Give time for Firebase to complete its operations
            loadUserProfile() // Reload profile after sign-in
            _loginComplete.value = true
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                val currentUserId = userRepository.getCurrentUserId()
                if (currentUserId != null) {
                    firebaseFirestore.collection("users")
                        .document(currentUserId)
                        .update(
                            mapOf(
                                "isOnline" to false,
                                "lastSeen" to System.currentTimeMillis()
                            )
                        )
                        .await()
                }
                statisticsManager.clearAllData()
                _loginComplete.value = false
                _isUserSignedIn.value = false
                _userProfileState.value = Error("Not logged in")
                authRepository.signOut()
            } catch (e: Exception) {
                _userProfileState.value = Error("Error during sign out: ${e.message}")
            }
        }
    }
}
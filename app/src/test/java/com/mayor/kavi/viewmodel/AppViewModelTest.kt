package com.mayor.kavi.viewmodel

import android.content.Context
import com.mayor.kavi.data.models.UserProfile
import com.mayor.kavi.data.repository.*
import com.mayor.kavi.ui.viewmodel.AppViewModel
import com.mayor.kavi.util.Result
import io.mockk.*
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private lateinit var appViewModel: AppViewModel
    private lateinit var userRepository: UserRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var context: Context
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        Dispatchers.setMain(testDispatcher)

        context = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)

        coEvery { userRepository.getCurrentUserId() } returns "test-user"
        coEvery { authRepository.isUserSignedIn() } returns false

        appViewModel = AppViewModel(
            context = context,
            userRepository = userRepository,
            authRepository = authRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `test load user profile success`() = runTest {
        // Arrange
        val profile = UserProfile(id = "test-user", name = "Test User")
        coEvery { userRepository.getUserById(any()) } returns Result.Success(profile)

        // Act
        appViewModel.loadUserProfile()
        advanceUntilIdle()

        // Assert
        assertTrue(appViewModel.userProfileState.value is Result.Success)
        assertEquals(profile, (appViewModel.userProfileState.value as Result.Success).data)
    }

    @Test
    fun `test login completion`() = runTest {
        // Act
        appViewModel.updateLogin()
        advanceUntilIdle()

        // Assert
        assertTrue(appViewModel.loginComplete.value)

        // Act - complete login
        appViewModel.onLoginComplete()
        advanceUntilIdle()

        // Assert
        assertFalse(appViewModel.loginComplete.value)
        coVerify { userRepository.setUserOnlineStatus(true) }
    }
}
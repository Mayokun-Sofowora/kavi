package com.mayor.kavi.viewmodel

import com.mayor.kavi.authentication.signin.SignInViewModel
import com.mayor.kavi.data.models.UserProfile
import com.mayor.kavi.data.repository.AuthRepository
import com.mayor.kavi.data.repository.UserRepository
import com.mayor.kavi.util.Result
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {
    private lateinit var viewModel: SignInViewModel
    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: UserRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        viewModel = SignInViewModel(authRepository, userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `getUserProfile returns profile on success`() = runTest {
        val userId = "test-user-id"
        val mockProfile = mockk<UserProfile>()

        coEvery { userRepository.getUserById(userId) } returns Result.Success(mockProfile)

        val result = viewModel.getUserProfile(userId)

        assertEquals(mockProfile, result)
        coVerify { userRepository.getUserById(userId) }
    }

    @Test
    fun `getUserProfile returns null on failure`() = runTest {
        val userId = "test-user-id"

        coEvery { userRepository.getUserById(userId) } returns Result.Error("User not found")

        val result = viewModel.getUserProfile(userId)

        assertNull(result)
        coVerify { userRepository.getUserById(userId) }
    }
}
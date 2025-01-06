package com.mayor.kavi.viewmodel

import app.cash.turbine.test
import com.google.firebase.auth.*
import com.mayor.kavi.authentication.signup.SignUpViewModel
import com.mayor.kavi.data.repository.AuthRepository
import com.mayor.kavi.data.repository.UserRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {

    private lateinit var viewModel: SignUpViewModel
    private lateinit var authRepository: AuthRepository
    private lateinit var userRepository: UserRepository
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockAuthResult: AuthResult
    private lateinit var mockUser: FirebaseUser

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)

        mockAuthResult = mockk()
        mockUser = mockk()
        every { mockAuthResult.user } returns mockUser
        every { mockUser.uid } returns "test-user-id"

        viewModel = SignUpViewModel(authRepository, userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `when fields are empty, signUp should show error message`() = runTest {
        // Given
        val username = ""
        val email = "test@example.com"
        val password = "password123"

        // When
        viewModel.signUp(username, email, password)

        // Then
        viewModel.signUpState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("All fields are required", state.toastMessage)
        }

        coVerify(exactly = 0) { authRepository.createUser(any(), any(), any()) }
    }

}

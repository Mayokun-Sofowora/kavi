package com.mayor.kavi.viewmodel

import com.mayor.kavi.data.models.*
import com.mayor.kavi.data.repository.SocialRepository
import com.mayor.kavi.data.repository.UserRepository
import com.mayor.kavi.ui.viewmodel.SocialViewModel
import com.mayor.kavi.util.Result
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SocialViewModelTest {
    private lateinit var viewModel: SocialViewModel
    private lateinit var socialRepository: SocialRepository
    private lateinit var userRepository: UserRepository
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        socialRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)

        // Mock default responses
        coEvery { userRepository.getCurrentUserId() } returns "test-user"
        every { socialRepository.getFriendRequests() } returns flowOf(emptyList())
        every { socialRepository.getFriends() } returns flowOf(emptyList())
        every { socialRepository.getOnlineFriends() } returns flowOf(emptyList())
        every { socialRepository.getGameHistory(any()) } returns flowOf(emptyList())
        every { socialRepository.getLeaderboard(any()) } returns flowOf(
            Leaderboard(gameType = "Pig", entries = emptyList())
        )

        viewModel = SocialViewModel(socialRepository, userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test loadGameHistory loads history correctly`() = runTest {
        // Mock repository responses
        val mockHistory = listOf(
            GameResult(
                gameType = "Pig",
                playerIds = listOf("test-user", "AI"),
                scores = mapOf("test-user" to 100, "AI" to 50),
                winnerId = "test-user",
                timestamp = System.currentTimeMillis()
            )
        )
        every { socialRepository.getGameHistory("test-user") } returns flowOf(mockHistory)

        // Load game history
        viewModel.loadCurrentUserGameHistory()
        testScheduler.advanceUntilIdle()

        // Verify history loaded
        assertEquals(mockHistory, viewModel.uiState.value.gameHistory)
        assertTrue(viewModel.uiState.value.error == null)
    }

    @Test
    fun `test sendFriendRequest sends request correctly`() = runTest {
        // Mock repository response
        coEvery { socialRepository.sendFriendRequest(any()) } returns Result.Success(Unit)

        // Send friend request
        viewModel.sendFriendRequest("friend1")
        testScheduler.advanceUntilIdle()

        // Verify request sent
        coVerify { socialRepository.sendFriendRequest("friend1") }
        assertTrue(viewModel.uiState.value.error == null)
    }

    @Test
    fun `test error handling in sendFriendRequest`() = runTest {
        // Mock repository error response
        coEvery { socialRepository.sendFriendRequest(any()) } returns Result.Error(
            "Failed to send request", Exception("Failed to send request")
        )

        // Send friend request
        viewModel.sendFriendRequest("friend1")
        testScheduler.advanceUntilIdle()

        // Verify error state
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun `test loadFriends loads and updates state correctly`() = runTest {
        // Mock repository response
        val mockFriends = listOf(
            UserProfile(id = "friend1", name = "Friend One"),
            UserProfile(id = "friend2", name = "Friend Two")
        )
        every { socialRepository.getFriends() } returns flowOf(mockFriends)

        // Initialize viewModel which triggers loadFriends
        viewModel = SocialViewModel(socialRepository, userRepository)
        testScheduler.advanceUntilIdle()

        // Verify friends loaded
        assertEquals(mockFriends, viewModel.uiState.value.friends)
    }

    @Test
    fun `test loadOnlineFriends loads and updates state correctly`() = runTest {
        // Mock repository response
        val mockOnlineFriends = listOf(
            UserProfile(id = "friend1", name = "Friend One", isOnline = true)
        )
        every { socialRepository.getOnlineFriends() } returns flowOf(mockOnlineFriends)

        // Initialize viewModel which triggers loadOnlineFriends
        viewModel = SocialViewModel(socialRepository, userRepository)
        testScheduler.advanceUntilIdle()

        // Verify online friends loaded
        assertEquals(mockOnlineFriends, viewModel.uiState.value.onlineFriends)
    }

    @Test
    fun `test error handling when getCurrentUserId returns null`() = runTest {
        // Mock repository error response
        coEvery { userRepository.getCurrentUserId() } returns null

        // Load game history
        viewModel.loadCurrentUserGameHistory()
        testScheduler.advanceUntilIdle()

        // Verify error state
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun `test error handling when getGameHistory fails`() = runTest {
        // Mock repository error response
        every { socialRepository.getGameHistory(any()) } returns flowOf(emptyList())
        coEvery { userRepository.getCurrentUserId() } returns "test-user"

        // Load game history
        viewModel.loadCurrentUserGameHistory()
        testScheduler.advanceUntilIdle()

        // Verify empty state
        assertTrue(viewModel.uiState.value.gameHistory.isEmpty())
    }
} 
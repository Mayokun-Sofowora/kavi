package com.mayor.kavi.viewmodel

import com.mayor.kavi.ui.viewmodel.LeaderboardViewModel
import app.cash.turbine.test
import com.mayor.kavi.data.models.LeaderboardEntry
import com.mayor.kavi.data.repository.LeaderboardRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModelTest {

    private lateinit var viewModel: LeaderboardViewModel
    private lateinit var repository: LeaderboardRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = LeaderboardViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `loadLeaderboard should update state with entries when successful`() = runTest {
        // Given
        val entries = listOf(
            LeaderboardEntry(
                userId = "1",
                position = 1,
                displayName = "Player 1",
                score = 100,
                gamesPlayed = 10,
                gamesWon = 5
            ),
            LeaderboardEntry(
                userId = "2",
                position = 2,
                displayName = "Player 2",
                score = 90,
                gamesPlayed = 8,
                gamesWon = 4
            )
        )
        coEvery { repository.getGlobalLeaderboard() } returns entries

        // When
        viewModel.loadLeaderboard()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(entries, state.leaderboardEntries)
            assertNull(state.error)
        }
    }

    @Test
    fun `loadLeaderboard should update state with error when fails`() = runTest {
        // Given
        val errorMessage = "Failed to load leaderboard"
        coEvery { repository.getGlobalLeaderboard() } throws Exception(errorMessage)

        // When
        viewModel.loadLeaderboard()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.leaderboardEntries.isEmpty())
            assertEquals(errorMessage, state.error)
        }
    }

    @Test
    fun `clearError should remove error message`() = runTest {
        // Given - set an error state
        coEvery { repository.getGlobalLeaderboard() } throws Exception("Error")
        viewModel.loadLeaderboard()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.error)
        }
    }
}

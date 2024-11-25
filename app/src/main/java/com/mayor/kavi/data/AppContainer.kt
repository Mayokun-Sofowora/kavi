package com.mayor.kavi.data

import android.content.Context
import com.mayor.kavi.data.repository.*
import com.mayor.kavi.data.repository.fakes.*
import com.mayor.kavi.data.service.AuthServiceImpl

/**
 * Dependency Injection container at the application level.
 */
interface AppContainer {
    val userRepository: UserRepository
    val gameRepository: GameRepository
    val leaderboardRepository: LeaderboardRepository
    val playerStatsRepository: StatisticsRepository
    val achievementRepository: AchievementRepository
}

/**
 * Implementation for the Dependency Injection container at the application level.
 *
 * Variables are initialized lazily and the same instance is shared across the whole app.
 */
class AppContainerImpl(private val applicationContext: Context) : AppContainer {
    private val database = KaviDatabase.getInstance(applicationContext)

    override val userRepository: UserRepository by lazy {
        UserRepositoryImpl(
            database.userDao(),
            authService = AuthServiceImpl(),
            context = applicationContext
        )
    }

    override val gameRepository: GameRepository by lazy {
        FakeGameRepository()
    }
    override val leaderboardRepository: LeaderboardRepository by lazy {
        FakeLeaderboardRepository()
    }
    override val playerStatsRepository: StatisticsRepository by lazy {
        FakeUserStatisticsRepository()
    }
    override val achievementRepository: AchievementRepository by lazy {
        FakeAchievementRepository()
    }
}

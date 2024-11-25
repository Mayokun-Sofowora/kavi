package com.mayor.kavi.din

import android.content.Context
import com.mayor.kavi.data.dao.*
import com.mayor.kavi.data.repository.*
import com.mayor.kavi.data.service.AuthService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepoModule {

    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao,
        authService: AuthService,
        @ApplicationContext context: Context
    ): UserRepository = UserRepositoryImpl(userDao, authService, context)

    @Provides
    @Singleton
    fun provideStatisticsRepository(
        playerStatsDao: UserStatisticsDao,
        achievementDao: AchievementDao
    ): StatisticsRepository = StatisticsRepositoryImpl(playerStatsDao, achievementDao)

    @Provides
    @Singleton
    fun provideGameRepository(
        gameDao: GameDao,
        playerStatsDao: UserStatisticsDao
    ): GameRepository = GameRepositoryImpl(gameDao, playerStatsDao)

    @Provides
    @Singleton
    fun provideLeaderboardRepository(
        leaderboardDao: LeaderboardDao
    ): LeaderboardRepository = LeaderboardRepositoryImpl(leaderboardDao)

    @Provides
    @Singleton
    fun provideAchievementRepository(
        achievementDao: AchievementDao
    ): AchievementRepository = AchievementRepositoryImpl(achievementDao)

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepositoryImpl(context) // Use the concrete implementation here
    }
}

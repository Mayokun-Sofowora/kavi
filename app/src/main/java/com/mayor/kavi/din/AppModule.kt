package com.mayor.kavi.din

import android.content.Context
import com.mayor.kavi.data.KaviDatabase
import com.mayor.kavi.data.dao.*
import com.mayor.kavi.data.service.*
import com.mayor.kavi.data.repository.SettingsRepository
import com.mayor.kavi.data.repository.SettingsRepositoryImpl
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideAuthService(): AuthService = AuthServiceImpl()

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): KaviDatabase =
        KaviDatabase.getInstance(context)

    @Provides
    fun provideGameDao(database: KaviDatabase): GameDao = database.gameDao()

    @Provides
    fun provideLeaderboardDao(database: KaviDatabase): LeaderboardDao = database.leaderboardDao()

    @Provides
    fun provideUserDao(database: KaviDatabase): UserDao = database.userDao()

    @Provides
    fun providePlayerStatsDao(database: KaviDatabase): UserStatisticsDao = database.userStatisticsDao()

    @Provides
    fun provideGameSessionsDao(database: KaviDatabase): GameSessionsDao = database.gameSessionsDao()

    @Provides
    fun provideGameResultsDao(database: KaviDatabase): GameResultsDao = database.gameResultsDao()

    @Provides
    fun provideUserSettingsDao(database: KaviDatabase): UserSettingsDao = database.userSettingsDao()

    @Provides
    fun provideAchievementDao(database: KaviDatabase): AchievementDao = database.achievementDao()

    @Provides
    fun provideFriendsDao(database: KaviDatabase): FriendsDao = database.friendsDao()

}

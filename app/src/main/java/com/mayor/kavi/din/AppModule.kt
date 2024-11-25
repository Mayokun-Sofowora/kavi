package com.mayor.kavi.din

import android.content.Context
import com.mayor.kavi.data.KaviDatabase
import com.mayor.kavi.data.dao.*
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
    fun provideAppDatabase(@ApplicationContext context: Context): KaviDatabase =
        KaviDatabase.getInstance(context)

    @Provides
    fun provideGamesDao(database: KaviDatabase): GamesDao = database.gamesDao()

    @Provides
    fun provideUsersDao(database: KaviDatabase): UsersDao = database.usersDao()

    @Provides
    fun providePlayerStatisticsDao(database: KaviDatabase): UserStatisticsDao =
        database.userStatisticsDao()

    @Provides
    fun provideGameSessionsDao(database: KaviDatabase): GameSessionsDao = database.gameSessionsDao()

    @Provides
    fun provideGameResultsDao(database: KaviDatabase): GameResultsDao = database.gameResultsDao()

    @Provides
    fun provideUserSettingsDao(database: KaviDatabase): UserSettingsDao = database.userSettingsDao()

    @Provides
    fun provideAchievementsDao(database: KaviDatabase): AchievementsDao = database.achievementsDao()

    @Provides
    fun provideFriendsDao(database: KaviDatabase): FriendsDao = database.friendsDao()

    @Provides
    fun provideGamePlayerDao(database: KaviDatabase): GamePlayersDao = database.gamePlayersDao()
}

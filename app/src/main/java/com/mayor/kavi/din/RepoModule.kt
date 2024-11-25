package com.mayor.kavi.din

import com.mayor.kavi.data.dao.*
import com.mayor.kavi.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepoModule {

    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UsersDao
    ): UserRepository = UserRepositoryImpl(userDao)

    @Provides
    @Singleton
    fun provideUserStatisticsRepository(
        playerStatsDao: UserStatisticsDao
    ): UserStatisticsRepository = UserStatisticsRepositoryImpl(playerStatsDao)

    @Provides
    @Singleton
    fun provideFriendsRepository(
        friendsDao: FriendsDao
    ): FriendsRepository = FriendsRepositoryImpl(friendsDao)

    @Provides
    @Singleton
    fun provideAchievementRepository(
        achievementsDao: AchievementsDao,
        friendsDao: FriendsDao
    ): AchievementRepository = AchievementRepositoryImpl(achievementsDao, friendsDao)

    @Provides
    @Singleton
    fun provideUserSettingsRepository(
        userSettingsDao: UserSettingsDao
    ): UserSettingsRepository = UserSettingsRepositoryImpl(userSettingsDao)

    @Provides
    @Singleton
    fun provideGamePlayersRepository(
        gamePlayersDao: GamePlayersDao
    ): GamePlayerRepository = GamePlayersRepositoryImpl(gamePlayersDao)

    @Provides
    @Singleton
    fun provideGameRepository(
        gameDao: GamesDao,
        usersDao: UsersDao,
        playerStatsDao: UserStatisticsDao
    ): GameRepository = GameRepositoryImpl(gameDao, usersDao, playerStatsDao)

    @Provides
    @Singleton
    fun provideGameSessionRepository(
        gameSessionsDao: GameSessionsDao
    ): GameSessionRepository = GameSessionRepositoryImpl(gameSessionsDao)

    @Provides
    @Singleton
    fun provideGameResultsRepository(
        gameResultsDao: GameResultsDao
    ): GameResultsRepository = GameResultsRepositoryImpl(gameResultsDao)

}

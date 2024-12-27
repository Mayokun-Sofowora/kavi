package com.mayor.kavi.di

import android.content.Context
import android.net.ConnectivityManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.mayor.kavi.data.manager.*
import com.mayor.kavi.data.manager.games.*
import com.mayor.kavi.data.repository.*
import com.mayor.kavi.di.AppModule.GameScope
import com.mayor.kavi.util.IoDispatcher
import com.mayor.kavi.util.NetworkConnection
import dagger.*
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.*
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class GameScope

    // Firebase & Core Services
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // System Services
    @Provides
    @Singleton
    fun provideConnectivityManager(@ApplicationContext context: Context): ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Provides
    @Singleton
    fun provideNetworkConnection(
        connectivityManager: ConnectivityManager
    ): NetworkConnection = NetworkConnection(connectivityManager)

    // Repositories
    @Provides
    @Singleton
    fun provideGameRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        userRepo: UserRepository,
        @ApplicationContext context: Context
    ): GameRepository = GameRepositoryImpl(firestore, auth, userRepo, context)

    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): UserRepository = UserRepositoryImpl(firestore, auth)

    @Provides
    @Singleton
    fun provideStatisticsRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        @ApplicationContext context: Context
    ): StatisticsRepository = StatisticsRepositoryImpl(firestore, auth, context)

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth, firestore: FirebaseFirestore, userRepository: UserRepository
    ): AuthRepository = AuthRepositoryImpl(firebaseAuth, firestore, userRepository)

    // Game Managers
    @Provides
    @Singleton
    fun provideDiceManager(): DiceManager = DiceManager()

    @Provides
    @Singleton
    fun provideGameSessionManager(
        gameRepository: GameRepository
    ): GameSessionManager = GameSessionManager(gameRepository = gameRepository)

    @Provides
    @Singleton
    fun providePigGameManager(
        statisticsManager: StatisticsManager
    ): PigGameManager =
        PigGameManager(statisticsManager = statisticsManager)

    @Provides
    @Singleton
    fun provideGreedGameManager(
        statisticsManager: StatisticsManager
    ): GreedGameManager =
        GreedGameManager(statisticsManager = statisticsManager)

    @Provides
    @Singleton
    fun provideMyGameManager(): MyGameManager = MyGameManager()

    @Provides
    @Singleton
    fun provideBalutGameManager(
        statisticsManager: StatisticsManager
    ): BalutGameManager =
        BalutGameManager(statisticsManager = statisticsManager)

    // Utility Managers
    @Provides
    @Singleton
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager =
        DataStoreManager(context)

    @Provides
    @Singleton
    fun provideShakeDetectionService(
        @ApplicationContext context: Context,
        settingsManager: SettingsManager,
        statisticsManager: StatisticsManager,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): ShakeDetectionManager =
        ShakeDetectionManager(context, settingsManager, statisticsManager, dispatcher)

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager =
        SettingsManager(context)

    @Provides
    @Singleton
    fun provideStatisticsManager(
        @ApplicationContext context: Context,
        statisticsRepository: StatisticsRepository,
        userRepository: UserRepository,
        applicationScope: CoroutineScope,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): StatisticsManager = StatisticsManager(
        context,
        statisticsRepository,
        userRepository,
        applicationScope,
        dispatcher
    )

    // Coroutines & Threading
    @Provides
    @Singleton
    @GameScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    @GameScope
    fun provideGameCoroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideAuthCoroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @IoDispatcher
    @Provides
    @Singleton
    fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO
}

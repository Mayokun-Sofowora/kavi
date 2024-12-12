package com.mayor.kavi.di

import android.content.Context
import android.net.ConnectivityManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mayor.kavi.authentication.AuthRepository
import com.mayor.kavi.authentication.AuthRepositoryImpl
import com.mayor.kavi.data.DataStoreManager
import com.mayor.kavi.data.GameRepository
import com.mayor.kavi.data.GameRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        gameRepository: GameRepository
    ): AuthRepository {
        return AuthRepositoryImpl(firebaseAuth, firestore, gameRepository)
    }

    @Provides
    @Singleton
    fun provideGameRepository(
        firebaseFirestore: FirebaseFirestore,
        firebaseAuth: FirebaseAuth,
        @ApplicationContext applicationContext: Context
    ): GameRepository {
        return GameRepositoryImpl(firebaseFirestore, firebaseAuth, applicationContext)
    }

    @Singleton
    @Provides
    fun provideNetworkConnDependencies(@ApplicationContext context: Context): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @Provides
    @Singleton
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager {
        return DataStoreManager(context)
    }
}

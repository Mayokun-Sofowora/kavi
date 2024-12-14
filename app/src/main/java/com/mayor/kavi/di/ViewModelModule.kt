package com.mayor.kavi.di

import com.mayor.kavi.data.GameRepository
import com.mayor.kavi.data.manager.StatisticsManager
import com.mayor.kavi.ui.viewmodel.MultiplayerViewModel
import dagger.*
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {
    @Provides
    fun provideMultiplayerViewModel(
        gameRepository: GameRepository,
        statisticsManager: StatisticsManager
    ): MultiplayerViewModel {
        return MultiplayerViewModel(gameRepository, statisticsManager)
    }
}
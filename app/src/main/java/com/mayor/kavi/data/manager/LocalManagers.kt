package com.mayor.kavi.data.manager

import androidx.compose.runtime.staticCompositionLocalOf

val LocalStatisticsManager = staticCompositionLocalOf<StatisticsManager> {
    error("No StatisticsManager provided")
}

val LocalSettingsManager = staticCompositionLocalOf<SettingsManager> {
    error("No SettingsManager provided")
}
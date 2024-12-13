package com.mayor.kavi

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.firestore.*
import com.mayor.kavi.data.manager.LocalSettingsManager
import com.mayor.kavi.data.manager.LocalStatisticsManager
import com.mayor.kavi.data.manager.SettingsManager
import com.mayor.kavi.data.manager.StatisticsManager
import com.mayor.kavi.ui.AppNavigation
import com.mayor.kavi.ui.theme.KaviTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var statisticsManager: StatisticsManager
    @Inject
    lateinit var settingsManager: SettingsManager

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag("MainActivity").d("onCreate Called")

        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings

        installSplashScreen()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            enableEdgeToEdge()
        }
        setContent {
            CompositionLocalProvider(
                LocalStatisticsManager provides statisticsManager,
                LocalSettingsManager provides settingsManager
            ) {
                KaviTheme {
                    AppNavigation()
                }
            }
            Timber.tag("MainActivity").d("setContent Launched")
        }
    }
}
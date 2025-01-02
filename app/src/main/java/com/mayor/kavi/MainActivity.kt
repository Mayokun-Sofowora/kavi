package com.mayor.kavi

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.mayor.kavi.data.manager.StatisticsManager
import com.mayor.kavi.data.manager.SettingsManager
import com.mayor.kavi.data.repository.UserRepository
import com.mayor.kavi.di.LocalUserRepository
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

    @Inject
    lateinit var userRepository: UserRepository

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        checkGooglePlayServices()
        setContent {
            KaviTheme {
                CompositionLocalProvider(
                    LocalUserRepository provides userRepository
                ) {
                    KaviApp(statisticsManager = statisticsManager, settingsManager = settingsManager)
                }
            }
        }
    }

    @Composable
    fun KaviApp(statisticsManager: StatisticsManager, settingsManager: SettingsManager) {
        val localStatisticsManager = remember { statisticsManager }
        CompositionLocalProvider(
            StatisticsManager.LocalStatisticsManager provides localStatisticsManager,
        ) {
            val localSettingsManager = remember { settingsManager }
            CompositionLocalProvider(
                SettingsManager.LocalSettingsManager provides localSettingsManager
            ) {
                KaviTheme {
                    AppNavigation()
                }
            }
        }
    }

    private fun checkGooglePlayServices() {
        try {
            val availability = GoogleApiAvailability.getInstance()
            val resultCode = availability.isGooglePlayServicesAvailable(this)
            if (resultCode != ConnectionResult.SUCCESS) {
                if (availability.isUserResolvableError(resultCode)) {
                    availability.getErrorDialog(this, resultCode, 9000)?.show()
                } else {
                    Toast.makeText(this, "Device not supported", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking Google Play Services")
        }
    }
}

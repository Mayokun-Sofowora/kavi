package com.mayor.kavi

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.common.*
import com.mayor.kavi.data.manager.*
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
        installSplashScreen()
        // Check Google Play Services
        checkGooglePlayServices()

        setContent {
            val statisticsManager = remember { statisticsManager }
            CompositionLocalProvider(
                StatisticsManager.LocalStatisticsManager provides statisticsManager,
            ) {
                val settingsManager = remember { settingsManager }
                CompositionLocalProvider(
                    SettingsManager.LocalSettingsManager provides settingsManager
                ) {
                    KaviTheme {
                        AppNavigation()
                    }
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

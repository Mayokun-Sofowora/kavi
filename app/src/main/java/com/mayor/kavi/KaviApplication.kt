package com.mayor.kavi

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * KaviApplication class for initializing global dependencies.
 * - Initializes FirebaseApp for Firebase services.
 * - Sets up Timber for logging in debug builds.
 */
@HiltAndroidApp
class KaviApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}


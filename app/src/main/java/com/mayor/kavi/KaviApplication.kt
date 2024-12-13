package com.mayor.kavi

import android.app.Application
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class KaviApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.tag("KaviApplication").d("Timber Initialized")
        } else {
            Timber.tag("KaviApplication").d("Timber not initialized in release")
        }
        Timber.tag("KaviApplication").d("onCreate called")
    }
}
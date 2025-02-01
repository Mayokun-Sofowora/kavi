package com.mayor.kavi

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * KaviApplication class for initializing global dependencies.
 * - Sets up Timber for logging in debug builds.
 */
@HiltAndroidApp
class KaviApplication : Application() {
//    override fun onCreate() {
//        super.onCreate()
//        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
//    }
}


package com.fluxer.client

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class FluxerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        Timber.i("🚀 Fluxer Application initialized")
    }
}

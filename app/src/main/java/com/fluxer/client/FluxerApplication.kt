package com.fluxer.client

import android.app.Application
import com.fluxer.client.data.local.InstanceConfigStore
import com.fluxer.client.util.UnifiedPushManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class FluxerApplication : Application() {
    @Inject lateinit var instanceConfigStore: InstanceConfigStore

    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        UnifiedPushManager.register(this, instanceConfigStore.getPublicVapidKey())
        Timber.i("Fluxer Application initialized")
    }
}

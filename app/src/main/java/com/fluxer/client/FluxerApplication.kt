package com.fluxer.client

import android.app.Application
import android.os.Build
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.fluxer.client.data.local.InstanceConfigStore
import com.fluxer.client.util.UnifiedPushManager
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class FluxerApplication : Application() {
    @Inject lateinit var instanceConfigStore: InstanceConfigStore
    @Inject lateinit var okHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Configure Coil with the app's auth-aware OkHttpClient so images behind
        // the API (avatars, server icons, attachments) load correctly.
        // Also enables GIF playback via coil-gif.
        Coil.setImageLoader {
            ImageLoader.Builder(this)
                .okHttpClient(okHttpClient)
                .components {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .crossfade(true)
                .build()
        }

        UnifiedPushManager.register(this, instanceConfigStore.getPublicVapidKey())
        Timber.i("Flukavike Application initialized")
    }
}

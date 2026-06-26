package com.fluxer.client.util

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import com.fluxer.client.data.local.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.unifiedpush.android.connector.UnifiedPush
import timber.log.Timber

/**
 * Manager for UnifiedPush distributor registration and preferences.
 */
object UnifiedPushManager {

    private val PUSH_PROVIDER_KEY = stringPreferencesKey("push_provider")
    private val UNIFIED_PUSH_INSTANCE = "fluxer"

    enum class PushProvider(val value: String) {
        FCM("fcm"),
        UNIFIEDPUSH("unifiedpush")
    }

    fun getSelectedProvider(context: Context): Flow<PushProvider> {
        return context.dataStore.data.map { prefs ->
            val value = prefs[PUSH_PROVIDER_KEY] ?: PushProvider.UNIFIEDPUSH.value
            PushProvider.entries.find { it.value == value } ?: PushProvider.FCM
        }
    }

    suspend fun setSelectedProvider(context: Context, provider: PushProvider) {
        context.dataStore.edit { prefs ->
            prefs[PUSH_PROVIDER_KEY] = provider.value
        }
    }

    /**
     * Register for push notifications using the selected provider.
     */
    fun register(context: Context, vapid: String? = null) {
        val distributor = UnifiedPush.getSavedDistributor(context)
        if (!distributor.isNullOrEmpty()) {
            Timber.d("Registering UnifiedPush with saved distributor: $distributor")
            if (vapid.isNullOrBlank()) {
                UnifiedPush.registerApp(context, UNIFIED_PUSH_INSTANCE)
            } else {
                UnifiedPush.registerApp(
                    context,
                    UNIFIED_PUSH_INSTANCE,
                    UnifiedPush.DEFAULT_FEATURES,
                    vapid
                )
            }
        } else {
            Timber.d("No saved UnifiedPush distributor, showing registration dialog")
        }
    }

    /**
     * Unregister from UnifiedPush.
     */
    fun unregister(context: Context) {
        UnifiedPush.unregisterApp(context, UNIFIED_PUSH_INSTANCE)
    }

    /**
     * Get available UnifiedPush distributors.
     */
    fun getDistributors(context: Context): List<String> {
        return UnifiedPush.getDistributors(context)
    }

    /**
     * Save a distributor preference and register.
     */
    fun saveDistributor(context: Context, distributor: String, vapid: String? = null) {
        UnifiedPush.saveDistributor(context, distributor)
        register(context, vapid)
    }
}

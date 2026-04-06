package com.fluxer.client.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Broadcast receiver for handling call-related actions from notifications.
 */
class CallBroadcastReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val callId = intent.getStringExtra("call_id")
        
        Timber.d("CallBroadcastReceiver: action=$action, callId=$callId")
        
        when (action) {
            ACTION_ACCEPT_CALL -> {
                // Handle call acceptance
                // This would typically trigger the call service to accept the call
            }
            ACTION_DECLINE_CALL -> {
                // Handle call decline
            }
            ACTION_END_CALL -> {
                // Handle call end
            }
        }
    }
    
    companion object {
        const val ACTION_ACCEPT_CALL = "com.fluxer.client.ACTION_ACCEPT_CALL"
        const val ACTION_DECLINE_CALL = "com.fluxer.client.ACTION_DECLINE_CALL"
        const val ACTION_END_CALL = "com.fluxer.client.ACTION_END_CALL"
    }
}

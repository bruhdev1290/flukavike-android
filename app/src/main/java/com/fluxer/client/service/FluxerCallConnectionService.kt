package com.fluxer.client.service

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.graphics.drawable.Icon
import com.fluxer.client.R
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * CallConnectionService for native CallKit-like integration on Android.
 * This allows calls to appear in the system phone app and handle audio routing.
 */
@AndroidEntryPoint
class FluxerCallConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Timber.d("onCreateIncomingConnection called")
        
        return FluxerConnection().apply {
            setRinging()
            setAddress(request?.address, TelecomManager.PRESENTATION_ALLOWED)
            request?.extras?.getString("caller_name")?.let { name ->
                setCallerDisplayName(name, TelecomManager.PRESENTATION_ALLOWED)
            }
        }
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        Timber.e("onCreateIncomingConnectionFailed")
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        Timber.e("onCreateOutgoingConnectionFailed")
    }

    companion object {
        private const val PHONE_ACCOUNT_HANDLE_ID = "fluxer_call_account"
        
        fun registerPhoneAccount(context: Context) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            
            val componentName = ComponentName(context, FluxerCallConnectionService::class.java)
            val phoneAccountHandle = PhoneAccountHandle(componentName, PHONE_ACCOUNT_HANDLE_ID)
            
            val phoneAccount = PhoneAccount.builder(phoneAccountHandle, "Fluxer")
                .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER or PhoneAccount.CAPABILITY_VIDEO_CALLING)
                .setIcon(Icon.createWithResource(context, R.drawable.ic_notification))
                .setHighlightColor(context.getColor(R.color.phantom_red))
                .build()
            
            telecomManager.registerPhoneAccount(phoneAccount)
        }
        
        fun placeCall(context: Context, handle: Uri, extras: Bundle? = null) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            
            val componentName = ComponentName(context, FluxerCallConnectionService::class.java)
            val phoneAccountHandle = PhoneAccountHandle(componentName, PHONE_ACCOUNT_HANDLE_ID)
            
            val callExtras = Bundle().apply {
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
                extras?.let { putAll(it) }
            }
            
            telecomManager.placeCall(handle, callExtras)
        }
    }
}

/**
 * Custom Connection class for handling call state
 */
class FluxerConnection : Connection() {
    
    override fun onStateChanged(state: Int) {
        super.onStateChanged(state)
        Timber.d("Connection state changed: $state")
    }
    
    override fun onAnswer() {
        super.onAnswer()
        Timber.d("Call answered")
        setActive()
    }
    
    override fun onReject() {
        super.onReject()
        Timber.d("Call rejected")
        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
    }
    
    override fun onDisconnect() {
        super.onDisconnect()
        Timber.d("Call disconnected")
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }
    
    override fun onHold() {
        super.onHold()
        Timber.d("Call on hold")
        setOnHold()
    }
    
    override fun onUnhold() {
        super.onUnhold()
        Timber.d("Call unheld")
        setActive()
    }
    
    override fun onCallAudioStateChanged(state: CallAudioState?) {
        super.onCallAudioStateChanged(state)
        Timber.d("Call audio state changed: $state")
    }
    
    override fun onShowIncomingCallUi() {
        super.onShowIncomingCallUi()
        Timber.d("Showing incoming call UI")
    }
}

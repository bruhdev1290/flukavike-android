package com.fluxer.client.data.remote

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebAuthnService @Inject constructor(
    private val json: Json
) {
    suspend fun authenticate(
        context: Context,
        options: JsonObject
    ): WebAuthnResult {
        return try {
            val requestJson = options.toString()
            Timber.d("Starting Credential Manager WebAuthn request")
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(GetPublicKeyCredentialOption(requestJson))
                .build()

            val result = CredentialManager.create(context).getCredential(
                context = context,
                request = request
            )

            val credential = result.credential
            if (credential is PublicKeyCredential) {
                Timber.d("Credential Manager returned PublicKeyCredential")
                WebAuthnResult.Success(
                    credentialResponse = json.parseToJsonElement(credential.authenticationResponseJson),
                    challenge = options["challenge"]?.jsonPrimitive?.content.orEmpty()
                )
            } else {
                Timber.w("Credential Manager returned unsupported credential type: ${credential.type}")
                WebAuthnResult.Error("Selected credential is not a passkey")
            }
        } catch (e: GetCredentialCancellationException) {
            Timber.i("Credential Manager WebAuthn cancelled")
            WebAuthnResult.Cancelled
        } catch (e: GetCredentialException) {
            Timber.e(e, "Credential Manager WebAuthn failed")
            WebAuthnResult.Error(e.message ?: "Passkey verification failed")
        } catch (e: Exception) {
            Timber.e(e, "Unexpected WebAuthn failure")
            WebAuthnResult.Error(e.message ?: "Passkey verification failed")
        }
    }
}

sealed class WebAuthnResult {
    data class Success(
        val credentialResponse: JsonElement,
        val challenge: String
    ) : WebAuthnResult()

    data class Error(val message: String) : WebAuthnResult()
    object Cancelled : WebAuthnResult()
}

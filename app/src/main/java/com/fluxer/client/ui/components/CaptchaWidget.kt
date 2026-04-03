package com.fluxer.client.ui.components

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.fluxer.client.BuildConfig
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CaptchaWidget(
    siteKey: String,
    provider: String,
    baseUrl: String?,
    onToken: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val resolvedSiteKey = siteKey.takeIf { it.isNotBlank() } ?: BuildConfig.HCAPTCHA_SITE_KEY
    val resolvedProvider = provider.trim().lowercase().let {
        if (it.contains("turnstile")) "turnstile" else "hcaptcha"
    }
    val html = generateCaptchaHtml(resolvedSiteKey, resolvedProvider)

    val onTokenState = rememberUpdatedState(onToken)
    val onErrorState = rememberUpdatedState(onError)

    var popupWebView by remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.setSupportMultipleWindows(true)
                webViewClient = WebViewClient()
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onToken(token: String) {
                        Timber.d("CaptchaBridge.onToken called")
                        onTokenState.value(token)
                    }
                    @JavascriptInterface
                    fun onError(message: String) {
                        Timber.e("CaptchaBridge.onError: $message")
                        onErrorState.value(message)
                    }
                }, "HCaptchaBridge")
                webChromeClient = object : WebChromeClient() {
                    override fun onCreateWindow(
                        view: WebView,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: android.os.Message
                    ): Boolean {
                        val popup = WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            webChromeClient = object : WebChromeClient() {
                                override fun onCloseWindow(window: WebView) {
                                    popupWebView = null
                                    window.destroy()
                                }
                            }
                        }
                        val transport = resultMsg.obj as WebView.WebViewTransport
                        transport.webView = popup
                        resultMsg.sendToTarget()
                        popupWebView = popup
                        return true
                    }
                }
                val url = baseUrl?.takeIf { it.isNotBlank() } ?: "https://web.fluxer.app/"
                loadDataWithBaseURL(url, html, "text/html", "UTF-8", null)
            }
        },
        onRelease = {
            CookieManager.getInstance().setAcceptThirdPartyCookies(it, false)
            it.destroy()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
    )

    val currentPopup = popupWebView
    if (currentPopup != null) {
        Dialog(onDismissRequest = {
            popupWebView = null
            currentPopup.destroy()
        }) {
            AndroidView(
                factory = { currentPopup },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
            )
        }
    }
}

private fun generateCaptchaHtml(siteKey: String, provider: String): String {
    val escapedSiteKey = siteKey
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    val scriptUrl = if (provider == "turnstile") {
        "https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit"
    } else {
        "https://js.hcaptcha.com/1/api.js?render=explicit"
    }

    return """
    <!doctype html>
    <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
          body, html { margin: 0; padding: 0; width: 100%; height: 100%; background: transparent; }
          #captcha-container { display: flex; align-items: center; justify-content: center; min-height: 78px; }
        </style>
        <script src="$scriptUrl" async defer></script>
        <script>
          const provider = "$provider";
          const siteKey = "$escapedSiteKey";
          let renderAttempts = 0;
          const maxRenderAttempts = 80;
          function postError(message) {
            window.HCaptchaBridge.onError(message);
          }
          function onSolve(token) {
            window.HCaptchaBridge.onToken(token);
          }
          function onError(message) {
            postError(message);
          }
          function onExpired() {
            postError("Verification expired, please try again");
          }
          function renderCaptcha() {
            renderAttempts += 1;
            if (provider === "turnstile") {
              if (!window.turnstile || typeof window.turnstile.render !== "function") {
                if (renderAttempts < maxRenderAttempts) { setTimeout(renderCaptcha, 100); return; }
                postError("Failed to load captcha provider"); return;
              }
              window.turnstile.render("#captcha-container", {
                sitekey: siteKey,
                callback: onSolve,
                "error-callback": onError,
                "expired-callback": onExpired,
                theme: "auto"
              });
              return;
            }
            if (!window.hcaptcha || typeof window.hcaptcha.render !== "function") {
              if (renderAttempts < maxRenderAttempts) { setTimeout(renderCaptcha, 100); return; }
              postError("Failed to load captcha provider"); return;
            }
            window.hcaptcha.render("captcha-container", {
              sitekey: siteKey,
              callback: onSolve,
              "error-callback": onError,
              "expired-callback": onExpired
            });
          }
          function onLoad() { renderCaptcha(); }
        </script>
      </head>
      <body onload="onLoad()">
        <div id="captcha-container"></div>
      </body>
    </html>
    """.trimIndent()
}

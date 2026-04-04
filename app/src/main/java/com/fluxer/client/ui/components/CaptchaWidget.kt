// =============================================================================
// !! DO NOT REVERT TO A POLLING APPROACH !!
// The captcha script uses ?onload=onCaptchaLoaded callback.
// A previous polling/interval approach timed out before hCaptcha finished loading.
// See CLAUDE.md for full details.
// =============================================================================
package com.fluxer.client.ui.components

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
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
                    override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                        Timber.tag("CaptchaJS").d("[${msg.messageLevel()}] ${msg.message()} (${msg.sourceId()}:${msg.lineNumber()})")
                        return true
                    }
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

    // Use onload callback so rendering triggers as soon as the script is ready,
    // regardless of network latency. The callback name must match the function below.
    val scriptUrl = if (provider == "turnstile") {
        "https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit&onload=onCaptchaLoaded"
    } else {
        "https://js.hcaptcha.com/1/api.js?render=explicit&onload=onCaptchaLoaded"
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
        <script>
          const provider = "$provider";
          const siteKey = "$escapedSiteKey";
          function onSolve(token) { window.HCaptchaBridge.onToken(token); }
          function onExpired() { window.HCaptchaBridge.onError("Verification expired, please try again"); }
          function onCaptchaError(e) { window.HCaptchaBridge.onError(String(e)); }
          function onCaptchaLoaded() {
            try {
              if (provider === "turnstile") {
                window.turnstile.render("#captcha-container", {
                  sitekey: siteKey,
                  callback: onSolve,
                  "error-callback": onCaptchaError,
                  "expired-callback": onExpired,
                  theme: "auto"
                });
              } else {
                window.hcaptcha.render("captcha-container", {
                  sitekey: siteKey,
                  callback: onSolve,
                  "error-callback": onCaptchaError,
                  "expired-callback": onExpired
                });
              }
            } catch(e) {
              window.HCaptchaBridge.onError("Render failed: " + e);
            }
          }
        </script>
        <script src="$scriptUrl" async defer></script>
      </head>
      <body>
        <div id="captcha-container"></div>
      </body>
    </html>
    """.trimIndent()
}

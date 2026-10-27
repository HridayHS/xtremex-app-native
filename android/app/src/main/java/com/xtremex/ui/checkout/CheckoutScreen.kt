package com.xtremex.ui.checkout

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.xtremex.ui.home.Muted
import java.net.URL

// Checkout WebView, ported from checkout/webview.tsx: trusted HTTPS hosts
// only, initial URL must be a /cart URL. Never loosen this allowlist.
private val ALLOWED_HOSTS = setOf("xtremexmartialarts.com", "www.xtremexmartialarts.com", "checkout.shopify.com", "pay.shopify.com", "shop.app")

fun trustedCheckoutUrl(value: String?): String? {
    if (value.isNullOrEmpty()) return null
    return try {
        val u = URL(value)
        if (u.protocol != "https") return null
        val host = u.host.lowercase()
        if (!(ALLOWED_HOSTS.contains(host) || host.endsWith(".xtremexmartialarts.com"))) return null
        if (!u.path.startsWith("/cart")) return null
        u.toString()
    } catch (e: Exception) { null }
}

@Composable
fun CheckoutScreen(url: String) {
    val trusted = trustedCheckoutUrl(url)
    if (trusted == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Checkout unavailable.", color = Muted) }
        return
    }
    AndroidView(factory = { ctx ->
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val target = request.url.toString()
                    return try {
                        val u = URL(target)
                        val host = u.host.lowercase()
                        !(u.protocol == "https" && (ALLOWED_HOSTS.contains(host) || host.endsWith(".xtremexmartialarts.com")))
                    } catch (e: Exception) { true }
                }
            }
            loadUrl(trusted)
        }
    }, modifier = Modifier.fillMaxSize())
}

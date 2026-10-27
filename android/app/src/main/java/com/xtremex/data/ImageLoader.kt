package com.xtremex.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

// Image loading via OkHttp — the same network path the Shopify API calls
// use (those load fine on device). Coil's default fetcher did not return
// any image on the user's Pixel (Oct 2026: hero + all product cards blank),
// so images go through this loader instead. In-memory cache only.
object ImageLoader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val cache = object : LinkedHashMap<String, Bitmap>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean = size > 128
    }

    @Synchronized fun getCached(url: String): Bitmap? = cache[url]
    @Synchronized fun put(url: String, bmp: Bitmap) { cache[url] = bmp }

    fun load(url: String): Bitmap? {
        getCached(url)?.let { return it }
        val req = Request.Builder().url(url).header("Accept", "image/*").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val bytes = resp.body?.bytes() ?: return null
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            put(url, bmp)
            return bmp
        }
    }
}

@Composable
fun ShopImage(
    url: String,
    contentDescription: String?,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(url) { mutableStateOf(ImageLoader.getCached(url)) }
    LaunchedEffect(url) {
        if (bitmap == null && url.isNotEmpty()) {
            bitmap = try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { ImageLoader.load(url) }
            } catch (e: Exception) { null }
        }
    }
    // V2: crossfade in once loaded, instead of popping in.
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (bitmap != null) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(300), label = "imgFade"
    )
    if (bitmap != null) {
        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = contentDescription, contentScale = contentScale, modifier = modifier.graphicsLayer(alpha = alpha))
    } else {
        Box(modifier = modifier)
    }
}

fun sizedImage(url: String, width: Int): String {
    if (url.isEmpty()) return url
    return try {
        val uri = android.net.Uri.parse(url)
        uri.buildUpon().appendQueryParameter("width", width.toString()).build().toString()
    } catch (e: Exception) { url }
}

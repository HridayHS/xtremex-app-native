package com.xtremex.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

// Shopify public endpoints, ported from docs/API.md. No auth, no token.
// Collection /products.json variants DO include `available`.
// Single-product .json variants do NOT — product-page code must merge
// .js availability (see docs/API.md); Home only uses collections, so
// `available` here is trustworthy.
object ShopifyRepository {
    const val BASE_URL = "https://xtremexmartialarts.com"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private data class CacheEntry(val value: List<Product>, val expiresAt: Long)
    private val collectionCache = mutableMapOf<String, CacheEntry>()
    private const val CACHE_TTL_MS = 5 * 60 * 1000L

    private fun get(url: String, timeoutSeconds: Long = 12): String {
        val req = Request.Builder().url(url).header("Accept", "application/json").build()
        val c = if (timeoutSeconds == 12L) client else client.newBuilder()
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS).build()
        c.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("Request failed (${resp.code})")
            return resp.body?.string().orEmpty()
        }
    }

    fun normaliseImageUrl(raw: String): String {
        val t = raw.trim()
        return when {
            t.startsWith("//") -> "https:$t"
            t.startsWith("http") -> t
            t.isEmpty() -> ""
            else -> "https://$t"
        }
    }

    private fun parsePriceToPaise(value: Any?): Int {
        return when (value) {
            is Number -> (BigDecimal(value.toString()).multiply(BigDecimal(100))).toInt()
            is String -> {
                val s = value.trim()
                if (s.isEmpty()) 0 else try {
                    (BigDecimal(s).multiply(BigDecimal(100))).toInt()
                } catch (e: Exception) { 0 }
            }
            else -> 0
        }
    }

    private fun parseTags(json: JSONObject): List<String> {
        return when {
            json.has("tags") && !json.isNull("tags") -> {
                val t = json.opt("tags")
                when (t) {
                    is JSONArray -> (0 until t.length()).map { t.optString(it) }.filter { it.isNotEmpty() }
                    is String -> t.split(",").map { it.trim().replace("&amp;", "&") }.filter { it.isNotEmpty() }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }

    private fun parseImages(json: JSONObject): List<String> {
        val out = mutableListOf<String>()
        if (json.has("images") && !json.isNull("images")) {
            val arr = json.optJSONArray("images") ?: return out
            for (i in 0 until arr.length()) {
                val item = arr.opt(i)
                val src = when (item) {
                    is JSONObject -> item.optString("src")
                    is String -> item
                    else -> ""
                }
                if (src.isNotEmpty()) out.add(normaliseImageUrl(src))
            }
        }
        return out
    }

    fun parseProduct(json: JSONObject): Product {
        val variants = mutableListOf<Variant>()
        val varr = json.optJSONArray("variants")
        if (varr != null) {
            for (i in 0 until varr.length()) {
                val v = varr.optJSONObject(i) ?: continue
                variants.add(
                    Variant(
                        id = v.opt("id").toString(),
                        title = v.optString("title"),
                        pricePaise = parsePriceToPaise(v.opt("price")),
                        compareAtPricePaise = if (v.isNull("compare_at_price")) null else parsePriceToPaise(v.opt("compare_at_price")),
                        // available !== false: missing counts as available (docs/DATA.md).
                        available = if (v.has("available") && !v.isNull("available")) v.optBoolean("available", true) else true,
                        option1 = v.optString("option1").takeIf { it.isNotEmpty() },
                        option2 = v.optString("option2").takeIf { it.isNotEmpty() },
                        option3 = v.optString("option3").takeIf { it.isNotEmpty() },
                        position = v.optInt("position", i + 1)
                    )
                )
            }
        }
        val options = mutableListOf<OptionDefinition>()
        val oarr = json.optJSONArray("options")
        if (oarr != null) {
            for (i in 0 until oarr.length()) {
                val o = oarr.optJSONObject(i) ?: continue
                val vals = mutableListOf<String>()
                val varr2 = o.optJSONArray("values")
                if (varr2 != null) for (j in 0 until varr2.length()) vals.add(varr2.optString(j))
                options.add(
                    OptionDefinition(
                        key = "option${o.optInt("position", i + 1)}",
                        name = o.optString("name"),
                        position = o.optInt("position", i + 1),
                        values = vals
                    )
                )
            }
        }
        return Product(
            id = json.opt("id").toString(),
            title = json.optString("title"),
            handle = json.optString("handle"),
            bodyHtml = json.optString("body_html"),
            vendor = json.optString("vendor"),
            tags = parseTags(json),
            variants = variants,
            options = options,
            images = parseImages(json),
            createdAt = json.optString("created_at"),
            publishedAt = json.optString("published_at")
        )
    }

    fun collectionProducts(handle: String): List<Product> {
        val now = System.currentTimeMillis()
        collectionCache[handle]?.let { if (it.expiresAt > now) return it.value }
        val out = mutableListOf<Product>()
        var page = 1
        while (true) {
            val url = "$BASE_URL/collections/$handle/products.json?limit=250&page=$page"
            val body = get(url)
            val arr = JSONObject(body).optJSONArray("products") ?: break
            if (arr.length() == 0) break
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { out.add(parseProduct(it)) }
            }
            if (arr.length() < 250) break
            page++
        }
        collectionCache[handle] = CacheEntry(out, now + CACHE_TTL_MS)
        return out
    }

    fun productByHandle(handle: String): Product? {
        val url = "$BASE_URL/products/$handle.json"
        val body = get(url)
        val obj = JSONObject(body).optJSONObject("product") ?: return null
        return parseProduct(obj)
    }

    fun newArrivals(limit: Int = 8): List<Product> {
        return collectionProducts("new-arrivals")
            .sortedByDescending { (it.createdAt.ifEmpty { it.publishedAt }) }
            .take(limit)
    }

    // Best sellers: homepage HTML, BEST SELLER(S) <h2> section only.
    // Fragile by design — returns empty on any shape change, never crashes.
    fun bestSellerHandles(): List<String> {
        return try {
            val req = Request.Builder().url("$BASE_URL/").header("Accept", "text/html").build()
            val html = client.newBuilder().readTimeout(15, TimeUnit.SECONDS).build()
                .newCall(req).execute().use { it.body?.string().orEmpty() }
            parseBestSellerHandles(html)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseBestSellerHandles(html: String): List<String> {
        val heading = Regex("<h2[^>]*>\\s*BEST SELLER(?:S)?\\s*</h2>", RegexOption.IGNORE_CASE)
            .find(html) ?: return emptyList()
        val after = html.substring(heading.range.last + 1)
        val nextH2 = after.indexOf("<h2")
        val section = if (nextH2 == -1) after.take(10000) else after.substring(0, nextH2)
        val seen = linkedSetOf<String>()
        Regex("""href=["'](?:https?://xtremexmartialarts\.com)?/products/([a-z0-9][a-z0-9-]*)["']""", RegexOption.IGNORE_CASE)
            .findAll(section).forEach { seen.add(it.groupValues[1]) }
        return seen.toList()
    }

    fun bestSellers(): List<Product> {
        val handles = bestSellerHandles()
        if (handles.isEmpty()) return emptyList()
        return handles.mapNotNull { handle ->
            try { productByHandle(handle) } catch (e: Exception) { null }
        }
    }

    fun refreshHome(): Pair<List<Product>, List<Product>> {
        collectionCache.remove("new-arrivals")
        return Pair(newArrivals(), bestSellers())
    }
}

package com.xtremex.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

// Cart + wishlist, ported from the old app's store/cart.ts + store/wishlist.ts.
// Persisted in SharedPreferences as JSON (the old app used MMKV); the shape
// is the same item fields, keyed by productId:variantId for cart and by
// product id for wishlist.

data class CartItem(
    val key: String,
    val productId: String,
    val variantId: String,
    val title: String,
    val variantTitle: String,
    val handle: String,
    val image: String,
    val pricePaise: Int,
    val qty: Int
)

data class WishlistItem(
    val id: String,
    val title: String,
    val handle: String,
    val image: String,
    val priceText: String
)

object CartStore {
    private lateinit var prefs: SharedPreferences
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()
    val count: Int get() = _items.value.sumOf { it.qty }
    val subtotalPaise: Int get() = _items.value.sumOf { it.pricePaise * it.qty }

    fun init(context: Context) {
        prefs = context.getSharedPreferences("xtremex_cart", Context.MODE_PRIVATE)
        _items.value = load()
    }
    private fun load(): List<CartItem> = try {
        val arr = JSONArray(prefs.getString("items", "[]") ?: "[]")
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let { o ->
            CartItem(o.optString("key"), o.optString("productId"), o.optString("variantId"),
                o.optString("title"), o.optString("variantTitle"), o.optString("handle"),
                o.optString("image"), o.optInt("pricePaise"), o.optInt("qty", 1))
        } }
    } catch (e: Exception) { emptyList() }
    private fun save(list: List<CartItem>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().apply {
            put("key", it.key); put("productId", it.productId); put("variantId", it.variantId)
            put("title", it.title); put("variantTitle", it.variantTitle); put("handle", it.handle)
            put("image", it.image); put("pricePaise", it.pricePaise); put("qty", it.qty)
        }) }
        prefs.edit().putString("items", arr.toString()).apply()
        _items.value = list
    }
    fun add(item: CartItem) {
        val cur = _items.value.toMutableList()
        val idx = cur.indexOfFirst { it.key == item.key }
        if (idx >= 0) cur[idx] = cur[idx].copy(qty = cur[idx].qty + item.qty) else cur.add(item)
        save(cur)
    }
    fun setQty(key: String, qty: Int) {
        if (qty <= 0) save(_items.value.filter { it.key != key })
        else save(_items.value.map { if (it.key == key) it.copy(qty = qty) else it })
    }
    fun remove(key: String) = save(_items.value.filter { it.key != key })
    fun clear() = save(emptyList())
    fun qtyForKey(key: String): Int = _items.value.firstOrNull { it.key == key }?.qty ?: 0
    fun checkoutUrl(): String? {
        val parts = _items.value.filter { it.qty > 0 }.map { "${it.variantId}:${it.qty}" }
        return if (parts.isEmpty()) null else "${ShopifyRepository.BASE_URL}/cart/${parts.joinToString(",")}"
    }
}

object WishlistStore {
    private lateinit var prefs: SharedPreferences
    private val _items = MutableStateFlow<List<WishlistItem>>(emptyList())
    val items: StateFlow<List<WishlistItem>> = _items.asStateFlow()
    fun ids(): Set<String> = _items.value.map { it.id }.toSet()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("xtremex_wishlist", Context.MODE_PRIVATE)
        _items.value = load()
        _followingStore.value = prefs.getBoolean("following_store", false)
    }
    private fun load(): List<WishlistItem> = try {
        val arr = JSONArray(prefs.getString("items", "[]") ?: "[]")
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let { o ->
            WishlistItem(o.optString("id"), o.optString("title"), o.optString("handle"),
                o.optString("image"), o.optString("priceText"))
        } }
    } catch (e: Exception) { emptyList() }
    private fun save(list: List<WishlistItem>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().apply {
            put("id", it.id); put("title", it.title); put("handle", it.handle)
            put("image", it.image); put("priceText", it.priceText)
        }) }
        prefs.edit().putString("items", arr.toString()).apply()
        _items.value = list
    }
    fun toggle(item: WishlistItem) {
        val cur = _items.value.toMutableList()
        val idx = cur.indexOfFirst { it.id == item.id }
        if (idx >= 0) cur.removeAt(idx) else cur.add(item)
        save(cur)
    }
    fun toggleProduct(p: Product) = toggle(WishlistItem(p.id, p.title, p.handle, p.image, formatPricePaise(p.firstVariant?.pricePaise)))
    fun remove(id: String) = save(_items.value.filter { it.id != id })
    // Shop-style "Follow store" (own code, XtremeX only): persisted like wishlist.
    private val _followingStore = MutableStateFlow(false)
    val followingStore: StateFlow<Boolean> = _followingStore.asStateFlow()
    fun toggleFollowStore() {
        _followingStore.value = !_followingStore.value
        prefs.edit().putBoolean("following_store", _followingStore.value).apply()
    }
    fun clear() = save(emptyList())
    fun isWishlisted(id: String): Boolean = _items.value.any { it.id == id }
}

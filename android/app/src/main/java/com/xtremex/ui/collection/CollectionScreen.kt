package com.xtremex.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xtremex.data.Product
import com.xtremex.data.ShopImage
import com.xtremex.data.ShopifyRepository
import com.xtremex.data.formatPricePaise
import com.xtremex.data.sizedImage
import com.xtremex.ui.home.BgSecondary
import com.xtremex.ui.home.Muted
import com.xtremex.ui.home.Primary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Collection screen, ported from collections/[handle].tsx: 2-column grid,
// sort (newest default), in-stock / out-of-stock, price range. Filter sheet
// simplified to a sort menu + toggles + price fields on this screen.

val COLLECTION_LABELS = mapOf(
    "new-arrivals" to "New Arrivals", "mens-apparel" to "Men's Apparel",
    "track-pants-joggers" to "Track pants & joggers", "trackpants-and-joggers" to "Trackpants and joggers",
    "compression-tshirts" to "Compression Tshirts", "oversized-tshirts" to "Oversized Tshirts",
    "tank-tops" to "Tank tops", "hoodies-jackets" to "Hoodies & Jackets",
    "womens-apparel" to "Women's Apparel", "crop-tees" to "Crop tshirts",
    "women-trackpants-joggers" to "Track pants & joggers", "women-compression" to "Women compression",
    "leggings-shorts" to "Leggings & shorts", "women-oversized-tshirt" to "Oversized Tshirts",
    "women-hoodies-jackets" to "Hoodies & Jackets", "weightlifting-gear" to "Weightlifting Gear",
    "combat-equipments" to "Combat Equipments", "bags" to "Bags"
)
val COLLECTION_BANNERS = mapOf(
    "new-arrivals" to "https://xtremexmartialarts.com/cdn/shop/files/Untitled-1_d1939f67-14bd-4f80-a7be-8b0e82ec5e43.png",
    "track-pants-joggers" to "https://xtremexmartialarts.com/cdn/shop/files/JOGGERS.png",
    "compression-tshirts" to "https://xtremexmartialarts.com/cdn/shop/files/Untitled-4_07a778d8-1825-480d-8d62-c968a414c89c.png",
    "oversized-tshirts" to "https://xtremexmartialarts.com/cdn/shop/files/Untitled-3_b3f72377-cc40-4534-9354-436bd1c030b3.png",
    "tank-tops" to "https://xtremexmartialarts.com/cdn/shop/files/Untitled-9_5a00073d-f113-4029-897c-9d7b857b2276.png",
    "hoodies-jackets" to "https://xtremexmartialarts.com/cdn/shop/files/Untitled-37.png",
    "crop-tees" to "https://xtremexmartialarts.com/cdn/shop/files/Untitled-39.png",
    "women-trackpants-joggers" to "https://xtremexmartialarts.com/cdn/shop/files/JOGGERS.png",
    "women-compression" to "https://xtremexmartialarts.com/cdn/shop/files/Untitled-4_07a778d8-1825-480d-8d62-c968a414c89c.png",
    "leggings-shorts" to "https://xtremexmartialarts.com/cdn/shop/files/Untitled-3_133529fc-7f6e-48b3-898c-35befe671cb7.png",
    "women-hoodies-jackets" to "https://xtremexmartialarts.com/cdn/shop/files/Untitled-37.png",
    "weightlifting-gear" to "https://xtremexmartialarts.com/cdn/shop/files/Untitled-5_9c885e6c-58ee-49a1-8406-0278f89342bb.png",
    "combat-equipments" to "https://xtremexmartialarts.com/cdn/shop/files/Untitled-7_826b027b-058d-4ecf-b1f5-879e3ec2e9fc.png"
)

private fun titleFor(handle: String) = COLLECTION_LABELS[handle]
    ?: handle.split("-").joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }

@Composable
fun CollectionScreen(handle: String, onBack: () -> Unit, onProduct: (String) -> Unit) {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var sort by remember { mutableStateOf("newest") }
    var showMenu by remember { mutableStateOf(false) }
    var inStockOnly by remember { mutableStateOf(false) }
    var priceMin by remember { mutableStateOf("") }
    var priceMax by remember { mutableStateOf("") }

    LaunchedEffect(handle) {
        loading = true; error = null
        try { products = withContext(Dispatchers.IO) { ShopifyRepository.collectionProducts(handle) } }
        catch (e: Exception) { error = "Could not load products. Please try again." }
        loading = false
    }

    val filtered = remember(products, sort, inStockOnly, priceMin, priceMax) {
        var arr = products.toList()
        if (inStockOnly) arr = arr.filter { it.inStock }
        val min = priceMin.toIntOrNull(); val max = priceMax.toIntOrNull()
        if (min != null || max != null) arr = arr.filter { p ->
            val price = (p.firstVariant?.pricePaise ?: 0) / 100
            (min == null || price >= min) && (max == null || price <= max)
        }
        when (sort) {
            "price-asc" -> arr.sortedBy { it.firstVariant?.pricePaise ?: 0 }
            "price-desc" -> arr.sortedByDescending { it.firstVariant?.pricePaise ?: 0 }
            "az" -> arr.sortedBy { it.title }
            "za" -> arr.sortedByDescending { it.title }
            "oldest" -> arr.sortedBy { it.createdAt.ifEmpty { it.publishedAt } }
            else -> arr.sortedByDescending { it.createdAt.ifEmpty { it.publishedAt } }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Primary) }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(titleFor(handle).uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Primary, letterSpacing = 0.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!loading && filtered.isNotEmpty()) Text("${filtered.size} items", fontSize = 11.sp, color = Muted)
            }
            Box {
                Button(onClick = { showMenu = true }) { Text("Sort", fontSize = 12.sp) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    listOf("newest" to "Newest", "oldest" to "Oldest", "price-asc" to "Price: Low to High", "price-desc" to "Price: High to Low", "az" to "A–Z", "za" to "Z–A").forEach { (k, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { sort = k; showMenu = false })
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Checkbox(checked = inStockOnly, onCheckedChange = { inStockOnly = it }, modifier = Modifier.size(20.dp))
            Text("In stock only", fontSize = 12.sp, color = Primary)
            OutlinedTextField(value = priceMin, onValueChange = { priceMin = it.filter { c -> c.isDigit() } }, placeholder = { Text("Min ₹", fontSize = 12.sp) }, singleLine = true, modifier = Modifier.width(90.dp))
            OutlinedTextField(value = priceMax, onValueChange = { priceMax = it.filter { c -> c.isDigit() } }, placeholder = { Text("Max ₹", fontSize = 12.sp) }, singleLine = true, modifier = Modifier.width(90.dp))
        }
        when {
            loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
            error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(error!!, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp)) }
            filtered.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No products found in this collection.", color = Primary, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp)) }
            else -> LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(22.dp), modifier = Modifier.fillMaxSize()) {
                COLLECTION_BANNERS[handle]?.let { banner ->
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        ShopImage(sizedImage(banner, 1200), titleFor(handle), ContentScale.Crop, Modifier.fillMaxWidth().aspectRatio(5.5f).clip(RoundedCornerShape(12.dp)))
                    }
                }
                items(filtered) { p ->
                    Column(modifier = Modifier.fillMaxWidth().clickable { onProduct(p.handle) }) {
                        Box {
                            ShopImage(sizedImage(p.image, 600), p.title, ContentScale.Crop, Modifier.fillMaxWidth().aspectRatio(0.6f).clip(RoundedCornerShape(12.dp)))
                            if (!p.inStock) Text("SOLD OUT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp).background(Primary, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Text(p.title.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Primary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp).height(30.dp))
                        Text(formatPricePaise(p.firstVariant?.pricePaise), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Primary)
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

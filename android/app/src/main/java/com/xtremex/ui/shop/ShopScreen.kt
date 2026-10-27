package com.xtremex.ui.shop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xtremex.data.Product
import com.xtremex.data.ShopImage
import com.xtremex.data.ShopifyRepository
import com.xtremex.data.WishlistStore
import com.xtremex.data.formatPricePaise
import com.xtremex.data.sizedImage
import com.xtremex.ui.home.BgSecondary
import com.xtremex.ui.home.Muted
import com.xtremex.ui.home.Primary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// Shop tab, ported from (tabs)/shop.tsx: search box + MEN/WOMEN/MORE tabs
// with category rows, and predictive search results via suggest.json.

data class ShopCategory(val label: String, val handle: String, val banner: String? = null)

private val MEN = listOf(
    ShopCategory("Track pants & joggers", "track-pants-joggers"),
    ShopCategory("Compression Tshirts", "compression-tshirts"),
    ShopCategory("Oversized Tshirts", "oversized-tshirts"),
    ShopCategory("Tank tops", "tank-tops"),
    ShopCategory("Hoodies & Jackets", "hoodies-jackets")
)
private val WOMEN = listOf(
    ShopCategory("Crop tshirts", "crop-tees"),
    ShopCategory("Track pants & joggers", "women-trackpants-joggers"),
    ShopCategory("Women compression", "women-compression"),
    ShopCategory("Leggings & shorts", "leggings-shorts"),
    ShopCategory("Oversized Tshirts", "women-oversized-tshirt"),
    ShopCategory("Hoodies & Jackets", "women-hoodies-jackets")
)
private val MORE = listOf(
    ShopCategory("New Arrivals", "new-arrivals"),
    ShopCategory("Weightlifting Gear", "weightlifting-gear"),
    ShopCategory("Combat Equipments", "combat-equipments")
)
private val MEN_BANNER = "https://xtremexmartialarts.com/cdn/shop/files/Untitled-3_b3f72377-cc40-4534-9354-436bd1c030b3.png"
private val WOMEN_BANNER = "https://xtremexmartialarts.com/cdn/shop/files/Untitled-39.png"

@Composable
fun ShopScreen(onProduct: (String) -> Unit, onCollection: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var debounced by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Product>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(0) }
    val wishlist by WishlistStore.items.collectAsState()
    val wishIds = wishlist.map { it.id }.toSet()

    LaunchedEffect(query) {
        delay(300)
        debounced = query.trim()
    }
    LaunchedEffect(debounced) {
        if (debounced.isEmpty()) { results = emptyList(); searching = false; return@LaunchedEffect }
        searching = true; searchError = null
        try {
            results = withContext(Dispatchers.IO) { ShopifyRepository.searchSuggest(debounced) }
        } catch (e: Exception) { searchError = "Could not load search results. Please try again." }
        searching = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp)) {
        Text("Shop", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it }, placeholder = { Text("Search products", color = Muted) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Muted) },
            singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        when {
            searching -> { androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) } }
            searchError != null -> { Text(searchError!!, color = Muted, modifier = Modifier.padding(16.dp)) }
            query.trim().isNotEmpty() && results.isEmpty() -> { Text("No products found.", color = Muted, modifier = Modifier.padding(16.dp)) }
            query.trim().isNotEmpty() -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(results) { p ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).clickable { onProduct(p.handle) }, verticalAlignment = Alignment.CenterVertically) {
                            ShopImage(sizedImage(p.image, 200), p.title, ContentScale.Crop, Modifier.size(72.dp).clip(RoundedCornerShape(6.dp)))
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(p.title.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Primary, maxLines = 2)
                                Text(formatPricePaise(p.firstVariant?.pricePaise), fontSize = 13.sp, color = Muted, modifier = Modifier.padding(top = 6.dp))
                            }
                            IconButton(onClick = { WishlistStore.toggleProduct(p) }) {
                                Icon(if (wishIds.contains(p.id)) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Wishlist", tint = Primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            else -> {
                TabRow(selectedTabIndex = tab, containerColor = Color.White, contentColor = Primary) {
                    listOf("MEN", "WOMEN", "MORE").forEachIndexed { i, label ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) })
                    }
                }
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (tab == 0) {
                        item { ShopBanner(MEN_BANNER) { onCollection("mens-apparel") } }
                        items(MEN) { c -> CategoryRow(c.label) { onCollection(c.handle) } }
                    } else if (tab == 1) {
                        item { ShopBanner(WOMEN_BANNER) { onCollection("womens-apparel") } }
                        items(WOMEN) { c -> CategoryRow(c.label) { onCollection(c.handle) } }
                    } else {
                        items(MORE) { c -> CategoryRow(c.label) { onCollection(c.handle) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopBanner(url: String, onClick: () -> Unit) {
    ShopImage(sizedImage(url, 800), "Shop", ContentScale.Crop, Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp)).clickable { onClick() })
    Text("SHOP NOW", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, color = Color.White, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun CategoryRow(label: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp).clickable { onClick() }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Primary, modifier = Modifier.weight(1f))
        Text("›", fontSize = 18.sp, color = Muted)
    }
}

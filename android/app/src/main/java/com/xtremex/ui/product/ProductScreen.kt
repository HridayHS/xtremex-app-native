package com.xtremex.ui.product

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xtremex.data.CartItem
import com.xtremex.data.CartStore
import com.xtremex.data.Product
import com.xtremex.data.ShopImage
import com.xtremex.data.ShopifyRepository
import com.xtremex.data.WishlistStore
import com.xtremex.data.availabilityMap
import com.xtremex.data.defaultVariant
import com.xtremex.data.formatPricePaise
import com.xtremex.data.initialSelection
import com.xtremex.data.optionDefinitionsOf
import com.xtremex.data.selectOption
import com.xtremex.data.sizedImage
import com.xtremex.data.variantDisplayTitle
import com.xtremex.data.variantImageIndex
import com.xtremex.ui.home.Muted
import com.xtremex.ui.home.Primary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Product screen, ported from products/[handle].tsx: image carousel (0.7
// aspect), title + price, variant selector (colour first, then size), plain
// description text stripped of HTML, BUY NOW / ADD TO CART / quantity bar /
// GO TO CART. Availability is force-refreshed from .js (docs/DATA.md).

private fun stripHtml(html: String): String {
    var t = html.replace(Regex("<[^>]+>"), " ").replace("&nbsp;", " ").replace("&amp;", "&")
    t = t.replace(Regex("\\s+"), " ").trim()
    return t
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductScreen(handle: String, onBack: () -> Unit, onCart: () -> Unit, onCheckout: (String) -> Unit) {
    var product by remember { mutableStateOf<Product?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selection by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var showDesc by remember { mutableStateOf(false) }
    val wishlist by WishlistStore.items.collectAsState()
    val cartItems by CartStore.items.collectAsState()

    LaunchedEffect(handle) {
        loading = true; error = null
        try {
            val p = withContext(Dispatchers.IO) { ShopifyRepository.productWithAvailability(handle) }
            product = p
            if (p != null) selection = initialSelection(p, optionDefinitionsOf(p))
            if (p == null) error = "Could not load product details. Please try again."
        } catch (e: Exception) { error = "Could not load product details. Please try again." }
        loading = false
    }

    val p = product
    val defs = if (p != null) optionDefinitionsOf(p) else emptyList()
    val selectedVariant = if (p != null) {
        if (defs.isEmpty()) defaultVariant(p) else p.variants.firstOrNull { v -> defs.all { d -> (when (d.position) { 1 -> v.option1; 2 -> v.option2; else -> v.option3 } ?: "") == (selection[d.position] ?: "") } }
    } else null
    val availMap = if (p != null) availabilityMap(p, defs, selection) else emptyMap()
    val variantKey = if (p != null && selectedVariant != null) "${p.id}:${selectedVariant.id}" else null
    val qtyInCart = if (variantKey != null) CartStore.qtyForKey(variantKey) else 0
    val isWishlisted = p != null && wishlist.any { it.id == p.id }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable { onBack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Primary)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (p != null) Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable { WishlistStore.toggleProduct(p) }, contentAlignment = Alignment.Center) {
                Icon(if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Wishlist", tint = if (isWishlisted) Color(0xFFE53935) else Primary)
            }
        }
        when {
            loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
            error != null || p == null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(error ?: "Product not found.", color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp)) }
            else -> {
                val images = p.images
                val pagerState = rememberPagerState(pageCount = { images.size.coerceAtLeast(1) })
                val variantIdx = variantImageIndex(p, selectedVariant)
                LaunchedEffect(variantIdx) {
                    if (variantIdx >= 0 && variantIdx < images.size && variantIdx != pagerState.currentPage) pagerState.scrollToPage(variantIdx)
                }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        if (images.isNotEmpty()) {
                            Box {
                                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().aspectRatio(0.7f)) { page ->
                                    ShopImage(sizedImage(images[page], 1080), p.title, ContentScale.Crop, Modifier.fillMaxSize())
                                }
                                if (images.size > 1) {
                                    Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
                                        repeat(images.size) { i ->
                                            Box(modifier = Modifier.padding(horizontal = 2.dp).size(6.dp).clip(CircleShape).background(if (i == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.45f)))
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.7f).background(Color(0xFFF0F0F0)), contentAlignment = Alignment.Center) { Text("No image available", color = Muted) }
                        }
                    }
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                            Text(p.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary, letterSpacing = 0.2.sp)
                            Text(formatPricePaise(selectedVariant?.pricePaise), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Primary, modifier = Modifier.padding(top = 6.dp))
                            if (selectedVariant?.compareAtPricePaise != null) Text(formatPricePaise(selectedVariant.compareAtPricePaise), fontSize = 14.sp, color = Muted, modifier = Modifier.padding(top = 2.dp))
                            val vt = variantDisplayTitle(selectedVariant)
                            if (vt.isNotEmpty()) Text(vt, fontSize = 13.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    defs.forEach { def ->
                        item {
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                                Text(def.name.trimEnd(':'), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary)
                                Row(modifier = Modifier.padding(top = 12.dp)) {
                                    def.values.forEach { value ->
                                        val sel = selection[def.position] == value
                                        val avail = availMap[def.position]?.contains(value) ?: true
                                        Box(
                                            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                                                .border(1.dp, if (sel) Primary else Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                                .background(if (sel) Primary else Color.White, RoundedCornerShape(8.dp))
                                                .clickable(enabled = avail) { selection = selectOption(p, defs, selection, def.position, value) }
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(value, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) Color.White else if (avail) Primary else Color(0xFFAAAAAA))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        val desc = stripHtml(p.bodyHtml)
                        if (desc.isNotEmpty()) {
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                                Text("Description", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Primary)
                                Text(if (showDesc || desc.length <= 300) desc else desc.take(300) + "…", fontSize = 14.sp, color = Muted, modifier = Modifier.padding(top = 8.dp))
                                if (desc.length > 300) Button(onClick = { showDesc = !showDesc }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Primary)) { Text(if (showDesc) "Show less" else "Read more", fontSize = 12.sp) }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
                // CTA bar
                if (selectedVariant != null) {
                    val available = selectedVariant.available
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        if (qtyInCart > 0) {
                            Row(modifier = Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(12.dp)).background(Primary), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { CartStore.setQty(variantKey!!, qtyInCart - 1) }) { Text("−", color = Color.White, fontSize = 22.sp) }
                                Text("$qtyInCart", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                IconButton(onClick = { if (available) CartStore.setQty(variantKey!!, qtyInCart + 1) }, enabled = available) { Text("+", color = Color.White, fontSize = 22.sp) }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(onClick = onCart, modifier = Modifier.height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Primary), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.5.dp, Primary)) { Text("GO TO CART", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp) }
                        } else if (available) {
                            Button(onClick = { onCheckout("${ShopifyRepository.BASE_URL}/cart/${selectedVariant.id}:1") }, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Primary), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.5.dp, Primary)) { Text("BUY NOW", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(onClick = {
                                CartStore.add(CartItem(variantKey!!, p.id, selectedVariant.id, p.title, variantDisplayTitle(selectedVariant), p.handle, selectedVariant.let { p.images.getOrNull(variantImageIndex(p, it).coerceAtLeast(0)) ?: p.image }, selectedVariant.pricePaise, 1))
                            }, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = RoundedCornerShape(12.dp)) { Text("ADD TO CART", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) }
                        } else {
                            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8D8D8D)), shape = RoundedCornerShape(12.dp)) { Text("SOLD OUT", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) }
                        }
                    }
                }
            }
        }
    }
}

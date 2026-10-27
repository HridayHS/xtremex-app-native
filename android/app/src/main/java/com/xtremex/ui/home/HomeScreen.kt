package com.xtremex.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xtremex.data.Product
import com.xtremex.data.ShopImage
import com.xtremex.data.WishlistStore
import com.xtremex.data.formatPricePaise
import com.xtremex.data.sizedImage
import kotlinx.coroutines.delay

// Home, ported from the old app's (tabs)/index.tsx + components/home/*.
// Images go through ShopImage (OkHttp) — Coil did not fetch on device.

val Primary = Color(0xFF111111)
val Secondary = Color(0xFF444444)
val Muted = Color(0xFF666666)
val BgSecondary = Color(0xFFF7F7F7)

private data class HeroSlide(val title: String, val poster: String, val handle: String)
private val HERO_SLIDES = listOf(
    HeroSlide("Skull Matrix", "https://xtremexmartialarts.com/cdn/shop/files/skull_mob_1.png", "skull-matrix-unisex-oversized-full-sleeve-tshirt"),
    HeroSlide("Signature Edition", "https://xtremexmartialarts.com/cdn/shop/files/jogger_mob_1.png", "signature-edition-unisex-joggers-black")
)
private data class DiscoverBanner(val handle: String, val image: String)
private val DISCOVER_BANNERS = listOf(
    DiscoverBanner("track-pants-and-joggers", "https://xtremexmartialarts.com/cdn/shop/files/csd.png"),
    DiscoverBanner("compression-tshirts", "https://xtremexmartialarts.com/cdn/shop/files/011_f7e0a246-b324-4902-a9b8-95c1aef8dd13.png"),
    DiscoverBanner("tank-tops", "https://xtremexmartialarts.com/cdn/shop/files/0144.png"),
    DiscoverBanner("oversized-tshirts", "https://xtremexmartialarts.com/cdn/shop/files/233.png")
)
private const val FOOTER_POSTER = "https://xtremexmartialarts.com/cdn/shop/files/preview_images/24adc4d90b40414e8cc05d5ebfdda1c0.thumbnail.0000000000_small.jpg"

@Composable
fun HomeHeader(onShop: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("XTREMEX", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp, color = Primary, modifier = Modifier.weight(1f))
        IconButton(onClick = onShop, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Search, contentDescription = "Search", tint = Primary, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier,
    onProduct: (String) -> Unit,
    onCollection: (String) -> Unit,
    onShop: () -> Unit
) {
    val state by vm.state.collectAsState()
    val wishlist by WishlistStore.items.collectAsState()
    val wishIds = wishlist.map { it.id }.toSet()
    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = { vm.refresh() }, modifier = modifier.fillMaxSize()) {
        if (state.loading && state.newArrivals.isEmpty() && state.bestSellers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
            return@PullToRefreshBox
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { HeroCarousel(onProduct) }
            item { SectionTitle("NEW ARRIVALS") }
            item {
                if (state.newArrivals.isEmpty()) {
                    Text("New arrivals will show here.", color = Muted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                } else {
                    LazyRow(contentPadding = PaddingValues(start = 16.dp, end = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.newArrivals) { p -> ProductCard(p, 158.dp, wishIds.contains(p.id), { WishlistStore.toggleProduct(p) }, { onProduct(p.handle) }) }
                        item {
                            Box(modifier = Modifier.width(158.dp).aspectRatio(0.6f).clip(RoundedCornerShape(12.dp)).background(BgSecondary).clickable { onCollection("new-arrivals") }, contentAlignment = Alignment.Center) {
                                Button(onClick = { onCollection("new-arrivals") }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Primary), shape = CircleShape) { Text("➔  See more", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(30.dp)) }
            item { SectionTitle("DISCOVER THE LATEST") }
            item { DiscoverGrid(onCollection) }
            item { Spacer(modifier = Modifier.height(30.dp)) }
            if (state.bestSellers.isNotEmpty()) {
                item { SectionTitle("BEST SELLER") }
                item {
                    LazyRow(contentPadding = PaddingValues(start = 16.dp, end = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.bestSellers) { p -> ProductCard(p, 270.dp, wishIds.contains(p.id), { WishlistStore.toggleProduct(p) }, { onProduct(p.handle) }) }
                    }
                }
                item { Spacer(modifier = Modifier.height(30.dp)) }
            }
            item { SectionTitle("FIND YOUR STYLE") }
            item { StylePills() }
            item { Spacer(modifier = Modifier.height(18.dp)) }
            item {
                ShopImage(sizedImage(FOOTER_POSTER, 800), "XtremeX", ContentScale.Crop, Modifier.fillMaxWidth().aspectRatio(0.75f).background(Color(0xFF1A1A1A)))
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
            if (state.error != null) item { Text(state.error!!, color = Muted, modifier = Modifier.padding(16.dp), textAlign = TextAlign.Center) }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, color = Primary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 20.dp, bottom = 16.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCarousel(onProduct: (String) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { HERO_SLIDES.size })
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % HERO_SLIDES.size)
        }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().aspectRatio(1f / 1.5f)) { page ->
            val slide = HERO_SLIDES[page]
            Box(modifier = Modifier.fillMaxSize().background(Primary)) {
                ShopImage(sizedImage(slide.poster, 1080), slide.title, ContentScale.Crop, Modifier.fillMaxSize())
                Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(slide.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onProduct(slide.handle) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Primary), shape = CircleShape) { Text("➔  Buy Now", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) }
                }
            }
        }
        Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(HERO_SLIDES.size) { i ->
                Box(modifier = Modifier.size(width = if (i == pagerState.currentPage) 24.dp else 8.dp, height = 8.dp).clip(CircleShape).background(if (i == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.4f)))
            }
        }
    }
}

@Composable
private fun DiscoverGrid(onCollection: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        for (row in DISCOVER_BANNERS.chunked(2)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { banner ->
                    ShopImage(sizedImage(banner.image, 800), banner.handle, ContentScale.Crop, Modifier.weight(1f).aspectRatio(1f / 1.4f).clip(RoundedCornerShape(14.dp)).background(BgSecondary).clickable { onCollection(banner.handle) })
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StylePills() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("HOODIE", "COMPRESSION").forEach { Pill(it) } }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("JOGGERS", "TSHIRTS").forEach { Pill(it) } }
    }
}

@Composable
private fun Pill(label: String) {
    Box(modifier = Modifier.border(1.5.dp, Primary, CircleShape).padding(vertical = 9.dp, horizontal = 18.dp), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = Primary)
    }
}

@Composable
fun ProductCard(product: Product, width: Dp, wishlisted: Boolean, onWishlist: () -> Unit, onProduct: () -> Unit) {
    Column(modifier = Modifier.width(width).clickable { onProduct() }) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ShopImage(sizedImage(product.image, 600), product.title, ContentScale.Crop, Modifier.fillMaxWidth().aspectRatio(0.6f).clip(RoundedCornerShape(12.dp)).background(BgSecondary))
            IconButton(onClick = onWishlist, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp)) {
                Icon(if (wishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Wishlist", tint = Primary, modifier = Modifier.size(18.dp))
            }
            if (!product.inStock) {
                Text("SOLD OUT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Primary, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        Text(product.title.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Primary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp).height(30.dp))
        Text(formatPricePaise(product.firstVariant?.pricePaise), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary, modifier = Modifier.padding(top = 3.dp))
        if (product.firstVariant?.compareAtPricePaise != null) {
            Text(formatPricePaise(product.firstVariant?.compareAtPricePaise), fontSize = 11.sp, color = Secondary, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

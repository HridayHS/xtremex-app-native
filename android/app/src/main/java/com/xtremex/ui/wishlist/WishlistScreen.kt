package com.xtremex.ui.wishlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.xtremex.data.ShopImage
import com.xtremex.data.ShopifyRepository
import com.xtremex.data.WishlistStore
import com.xtremex.data.defaultVariant
import com.xtremex.data.sizedImage
import com.xtremex.data.variantDisplayTitle
import com.xtremex.ui.home.Muted
import com.xtremex.ui.home.Primary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Wishlist tab, ported from (tabs)/wishlist.tsx (variant sheet simplified:
// MOVE TO CART adds the default available variant directly).

@Composable
fun WishlistScreen(onProduct: (String) -> Unit) {
    val items by WishlistStore.items.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Wishlist", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black, modifier = Modifier.weight(1f))
            if (items.isNotEmpty()) Button(onClick = { WishlistStore.clear() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Primary), shape = RoundedCornerShape(999.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Primary)) { Text("CLEAR", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp) }
        }
        Text("Items you saved for later.", fontSize = 14.sp, color = Muted, modifier = Modifier.padding(top = 8.dp, bottom = 14.dp))
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your wishlist is empty.", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                    Text("Tap the heart icon on products to save them.", fontSize = 14.sp, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items) { item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).clickable { onProduct(item.handle) }, verticalAlignment = Alignment.CenterVertically) {
                        ShopImage(sizedImage(item.image, 200), item.title, ContentScale.Crop, Modifier.size(72.dp).clip(RoundedCornerShape(6.dp)))
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(item.title.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary, maxLines = 2)
                            Text(item.priceText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333), modifier = Modifier.padding(top = 6.dp))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            IconButton(onClick = { WishlistStore.remove(item.id) }) { Text("✕", fontSize = 16.sp, color = Primary) }
                            Button(onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val p = ShopifyRepository.productWithAvailability(item.handle) ?: return@launch
                                        val v = defaultVariant(p) ?: return@launch
                                        if (!v.available) return@launch
                                        CartStore.add(CartItem("${p.id}:${v.id}", p.id, v.id, p.title, variantDisplayTitle(v), p.handle, p.image, v.pricePaise, 1))
                                        WishlistStore.remove(item.id)
                                    } catch (e: Exception) { }
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = RoundedCornerShape(999.dp)) { Text("MOVE TO CART", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp, color = Color.White) }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

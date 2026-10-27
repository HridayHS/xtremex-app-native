package com.xtremex.ui.cart

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
import com.xtremex.data.CartStore
import com.xtremex.data.ShopImage
import com.xtremex.data.formatPricePaise
import com.xtremex.data.sizedImage
import com.xtremex.ui.home.Muted
import com.xtremex.ui.home.Primary

// Cart tab, ported from (tabs)/cart.tsx.

@Composable
fun CartScreen(onProduct: (String) -> Unit, onCheckout: (String) -> Unit) {
    val items by CartStore.items.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp)) {
        Text("Cart", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
        Text("Review your selected items.", fontSize = 14.sp, color = Muted, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your cart is empty.", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                    Text("Add a product from any product page.", fontSize = 14.sp, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items) { item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).clickable { onProduct(item.handle) }, verticalAlignment = Alignment.CenterVertically) {
                        ShopImage(sizedImage(item.image, 200), item.title, ContentScale.Crop, Modifier.size(72.dp).clip(RoundedCornerShape(6.dp)))
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(item.title.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Primary, maxLines = 2)
                            if (item.variantTitle.isNotEmpty()) Text(item.variantTitle, fontSize = 12.sp, color = Muted, modifier = Modifier.padding(top = 2.dp))
                            Text(formatPricePaise(item.pricePaise), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary, modifier = Modifier.padding(top = 4.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { CartStore.setQty(item.key, item.qty - 1) }) { Text("−", fontSize = 20.sp, color = Primary) }
                            Text("${item.qty}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Primary)
                            IconButton(onClick = { CartStore.setQty(item.key, item.qty + 1) }) { Text("+", fontSize = 20.sp, color = Primary) }
                        }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Text("Subtotal", fontSize = 16.sp, color = Color(0xFF333333), modifier = Modifier.weight(1f))
                Text(formatPricePaise(CartStore.subtotalPaise), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }
            Button(onClick = { CartStore.clear() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF222222)), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCCCCCC))) { Text("CLEAR CART", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
            Spacer(modifier = Modifier.height(10.dp))
        }
        Button(onClick = { CartStore.checkoutUrl()?.let { onCheckout(it) } }, enabled = items.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F1F1F)), shape = RoundedCornerShape(12.dp)) { Text("PROCEED TO CHECKOUT", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White) }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

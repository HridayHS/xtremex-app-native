package com.xtremex.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xtremex.data.CartStore
import com.xtremex.ui.cart.CartScreen
import com.xtremex.ui.checkout.CheckoutScreen
import com.xtremex.ui.collection.CollectionScreen
import com.xtremex.ui.home.HomeHeader
import com.xtremex.ui.home.HomeScreen
import com.xtremex.ui.product.ProductScreen
import com.xtremex.ui.profile.ProfileScreen
import com.xtremex.ui.shop.ShopScreen
import com.xtremex.ui.wishlist.WishlistScreen
import java.net.URLDecoder
import java.net.URLEncoder

// App navigation, ported from Expo Router: tabs (Home/Shop/Wishlist/Cart/
// Profile, 80dp bar, black active / grey inactive, no labels, cart badge)
// plus full-screen Collection / Product / Checkout routes above the tabs.

@Composable
fun AppNav() {
    val nav = rememberNavController()
    var tab by remember { mutableStateOf(0) }
    val cartItems by CartStore.items.collectAsState()
    val cartCount = cartItems.sumOf { it.qty }
    val goProduct: (String) -> Unit = { handle -> nav.navigate("product/$handle") }
    val goCollection: (String) -> Unit = { handle -> nav.navigate("collection/$handle") }
    val goCheckout: (String) -> Unit = { url -> nav.navigate("checkout?url=${URLEncoder.encode(url, "UTF-8")}") }

    NavHost(navController = nav, startDestination = "tabs") {
        composable("tabs") {
            Scaffold(
                topBar = { if (tab == 0) HomeHeader(onShop = { tab = 1 }) },
                bottomBar = {
                    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp, modifier = Modifier.height(80.dp)) {
                        val icons = listOf(
                            Icons.Filled.Home to "Home",
                            Icons.Filled.Search to "Shop",
                            Icons.Outlined.FavoriteBorder to "Wishlist",
                            Icons.Filled.ShoppingCart to "Cart",
                            Icons.Filled.Person to "Profile"
                        )
                        icons.forEachIndexed { i, (icon, label) ->
                            NavigationBarItem(
                                selected = tab == i,
                                onClick = { tab = i },
                                icon = {
                                    if (i == 3 && cartCount > 0) {
                                        BadgedBox(badge = { Badge(containerColor = Color.Black) { Text("$cartCount", fontSize = 10.sp, color = Color.White) } }) {
                                            Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
                                        }
                                    } else {
                                        Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
                                    }
                                },
                                label = null,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    unselectedIconColor = Color(0xFF999999),
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    when (tab) {
                        0 -> HomeScreen(onProduct = goProduct, onCollection = goCollection, onShop = { tab = 1 })
                        1 -> ShopScreen(onProduct = goProduct, onCollection = goCollection)
                        2 -> WishlistScreen(onProduct = goProduct)
                        3 -> CartScreen(onProduct = goProduct, onCheckout = goCheckout)
                        else -> ProfileScreen()
                    }
                }
            }
        }
        composable("collection/{handle}", arguments = listOf(navArgument("handle") { type = NavType.StringType })) { back ->
            CollectionScreen(handle = back.arguments?.getString("handle") ?: "", onBack = { nav.popBackStack() }, onProduct = goProduct)
        }
        composable("product/{handle}", arguments = listOf(navArgument("handle") { type = NavType.StringType })) { back ->
            ProductScreen(
                handle = back.arguments?.getString("handle") ?: "",
                onBack = { nav.popBackStack() },
                onCart = { tab = 3; nav.navigate("tabs") { popUpTo("tabs") { inclusive = false } } },
                onCheckout = goCheckout
            )
        }
        composable("checkout?url={url}", arguments = listOf(navArgument("url") { type = NavType.StringType; nullable = true })) { back ->
            val raw = back.arguments?.getString("url") ?: ""
            CheckoutScreen(url = try { URLDecoder.decode(raw, "UTF-8") } catch (e: Exception) { raw })
        }
    }
}

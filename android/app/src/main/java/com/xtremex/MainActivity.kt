package com.xtremex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.xtremex.ui.theme.XtremeXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XtremeXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScaffoldPlaceholder()
                }
            }
        }
    }
}

@Composable
fun ScaffoldPlaceholder() {
    // Scaffold placeholder: the 1:1 port screens (Home, Shop, Cart,
    // Wishlist, Profile, Collection, Product, Checkout) land here
    // one at a time, following docs/PRODUCT.md.
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "XTREMEX", style = MaterialTheme.typography.headlineMedium)
    }
}

@Preview(showBackground = true)
@Composable
fun ScaffoldPlaceholderPreview() {
    XtremeXTheme {
        ScaffoldPlaceholder()
    }
}

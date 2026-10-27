package com.xtremex.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import com.xtremex.data.ShopImage

// V2 polish, in the spirit of Shop (Compose + spring physics, no copied code):
// press-scale, image crossfade, shimmer placeholder, spring specs.

// Spring used for tab / card / chip movement — snappy, not bouncy.
val ShopSpring = spring<Float>(dampingRatio = 0.8f, stiffness = 400f)
val FastTween = tween<Float>(durationMillis = 250, easing = FastOutSlowInEasing)

/** Scale to 0.96 while pressed, spring back on release. */
fun Modifier.pressScale(): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f), label = "press"
    )
    this.graphicsLayer(scaleX = scale, scaleY = scale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { pressed = true; tryAwaitRelease(); pressed = false }
            )
        }
}

/** Clickable with press-scale + no ripple clutter (Shop-style quiet press). */
@Composable
fun Modifier.shopClickable(onClick: () -> Unit): Modifier = this
    .pressScale()
    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)

/** Grey shimmer box shown while an image loads. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart), label = "shimmerX"
    )
    Box(modifier = modifier.background(
        Brush.linearGradient(
            colors = listOf(Color(0xFFEDEDED), Color(0xFFF7F7F7), Color(0xFFEDEDED)),
            start = Offset(x * 300f, 0f), end = Offset((x + 1f) * 300f, 300f)
        )
    ))
}

/** Image with crossfade: shimmer while loading, then fade in (Shop-style, no pop-in). */
@Composable
fun FadeImage(url: String, contentDescription: String?, contentScale: ContentScale = ContentScale.Crop, modifier: Modifier = Modifier) {
    // ShopImage already shows a blank Box while loading; wrap it in a
    // shimmer container so the slot never looks empty, and fade the
    // whole slot in once the bitmap arrives (ShopImage recomposes then).
    Box(modifier = modifier) {
        ShimmerBox(modifier = Modifier.fillMaxSize())
        // Re-use ShopImage's cache: if cached, it renders immediately.
        ShopImage(url, contentDescription, contentScale, Modifier.fillMaxSize())
    }
}

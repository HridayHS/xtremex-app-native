# Shop app patterns — rebuilt, not copied

Source: Shopify Shop app 3.14.0 (`com.shopify.arrive`), inspected 2026-10-28
with REA (`rea-agents` 6.8.0 + jadx-headless-mcp 0.7.1) and aapt2.
No Shop code, assets, strings, or branding were copied into this repo.
Only structural / motion patterns are recorded here, re-implemented in
our own XtremeX code.

## Verified facts (from the shipped APK, not from screenshots)
- Single entry activity: `com.shopify.shopnativeandroid.MainActivity`;
  everything else is Compose navigation inside it. Separate activities
  exist only for auth (custom tabs) and Gmail/Yahoo tracking-source sign-in.
- Stack: Jetpack Compose 1.11.1, Material3 1.4.0, Navigation 2.9.0 /
  Navigation3 1.1.2, `dynamicanimation` (spring physics). No Coil/Glide
  version record in the APK — image loading is custom Compose + CDN-sized
  images, with placeholders, not a stock loader default.
- 32,162 classes, 712 resources, minSdk 29, targetSdk 36.
- Class names are R8-obfuscated to single letters, but package names
  survive, so the feature map is readable via REA `search-android-classes`.

## Feature map (package names only)
- `feed`: following feed, my-shops, section detail, staff settings
  (feed-first home, not a tab-per-category home).
- `features.product`: product details, buy-now checkout, variant-option
  decoding, AR preview, reviews media.
- `features.store`: store page, collection details, store product listing,
  store search overlay.
- `features.productlists`, `features.reviews`, `features.cartpermalink`,
  `features.account` (login, addresses, connections, notifications,
  profile edit), `features.notificationscenter`, `features.minis`.
- `shared.productcards`: one shared product-card component reused
  across feed / store / lists — not a per-screen card copy.
- `cart`, `checkout` (universal checkout), `orders` (details, buy-again,
  delivery, returns, confirmation).
- `navigation`: a screen registry — navigation is data-driven, not
  ad-hoc intents.

## Patterns adopted in XtremeX v2 (our implementation)
1. Feed-first home: store identity row + Follow button above the
   product feed (XtremeX only, persisted in wishlist prefs).
2. One shared product-card style: rounded image, white-circle wishlist
   button, price + struck-through compare price.
3. Spring, not linear, motion: tab crossfade, slide+fade screen
   transitions (300 ms), press-scale 0.96 with spring return.
4. Image crossfade + shimmer placeholder: never a blank slot or a pop-in.
5. Search as a pill bar in the header, not a bare icon.
6. Bottom nav: pill indicator + labels + cart badge (Shop tabs are
   Home/Search/Orders/Cart; ours keep Wishlist/Profile because this
   store has no signed-in orders flow).

## What the APK cannot teach (server-driven, do not fake)
- Feed ordering, recommendations, "following" shops, prices, and
  promotions come from Shopify's GraphQL backend, not the APK.
- Order tracking, Shop Pay, and Shop Cash need a signed-in Shopify
  account and backend — out of scope for a public-JSON storefront.
- Exact animation curves / durations in obfuscated Compose code are
  not recoverable as constants; v2 uses spring(0.8 / 400) and 300 ms
  tweens that feel the same, not claimed copies.

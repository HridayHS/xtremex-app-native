# Product spec (1:1 port)

Source of truth for screens and flows. Ported from the old React
Native app (`HridayHS/xx`, Mar–Apr 2026, v1.0.0, Expo + React Native).

## Tabs (bottom navigation)

1. Home
2. Shop (search)
3. Cart
4. Wishlist
5. Profile (placeholder in v1 — see below)

## Screens

- **Home**: hero video, hero banner, New Arrivals (first 8, newest
  first), Discover grid (collection shortcuts), Best Sellers
  (handles parsed from the homepage HTML — fragile, see `docs/API.md`),
  Find Your Style, footer video. Pull-to-refresh refreshes best
  sellers + new arrivals.
- **Shop**: predictive search via Shopify suggest endpoint, debounced.
  Tapping a result opens the product page.
- **Collection** (`/collections/<handle>`): 2-column grid, banner
  image when the collection defines one, sort (newest default, price
  asc/desc, A–Z, Z–A), filters (in stock / out of stock, price range).
  Sold-out products get a SOLD OUT badge. Tapping a card opens the
  product page (prefetch product + hero image on press).
- **Product** (`/products/<handle>`): portrait image carousel with
  dots, title, price of the selected variant, variant selector
  (chips, unavailable values struck through and disabled), collapsible
  description (HTML, with the redundant outer list wrapper stripped),
  BUY NOW + ADD TO CART, or SOLD OUT when the selected variant is
  unavailable, or quantity controls + GO TO CART once the variant is
  in the cart.
- **Cart**: line items with image/title/variant/price/quantity,
  subtotal, PROCEED TO CHECKOUT (opens the checkout WebView with a
  cart permalink), empty state when the cart is empty.
- **Wishlist**: saved products, move-to-cart sheet with variant
  selection, SOLD OUT state when every variant is unavailable.
- **Profile (v1 placeholder)**: account card and rows for My Orders,
  Saved Addresses, Payment Methods, Support, Logout. In the old app
  these rows did nothing except haptics. Keep that behaviour for the
  1:1 port — real accounts/orders are a v2 item, not a port item.
- **Checkout WebView**: see `docs/CHECKOUT.md`.

## Design tokens (from the old app's `theme/tokens.ts`)

- Primary `#111111`, white `#ffffff`, black `#000000`
- Text: primary `#111111`, secondary `#444444`, muted `#666666`
- Backgrounds: primary `#ffffff`, secondary `#f7f7f7`
- Borders: `#e0e0e0`, light `#ededed`, lighter `#f0f0f0`
- Spacing (dp): xs 4, sm 8, md 12, lg 16, xl 20, xxl 24, xxxl 30
- Radii: sm 4, md 8, lg 12, xl 14, full 999
- Portrait orientation only in v1 (matching the old app).
- App name: XtremeX. ApplicationId: `com.xtremex.native`. Code namespace stays `com.xtremex` because `native` is a Java keyword and cannot be a code package segment. Scheme: `xtremex`.

## Collections (source of truth, from old `store/collections.ts`)

New Arrivals (`new-arrivals`), Best Seller (`best-seller`),
Men's Apparel, Track pants & joggers, Compression Tshirts,
Oversized Tshirts, Tank tops, Hoodies & Jackets, Women's Apparel,
Crop tshirts, Women Track pants & joggers, Women compression,
Leggings & shorts, Women Oversized Tshirt, Women Hoodies & Jackets,
Weightlifting Gear, Combat Equipments, Bags. Banners are Shopify CDN
URLs in the old collections file — reuse them verbatim for the port.

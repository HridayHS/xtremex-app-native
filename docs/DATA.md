# Data spec

## Models (platform-agnostic)

- `Product`: id, title, handle, bodyHtml, vendor, tags[], variants[],
  options[], images[], image (first/featured).
- `Variant`: id, title, price (int paise/cents), compareAtPrice?,
  available (bool, default true when unknown), option1/2/3, position.
- `OptionDefinition`: key (`option1`…), name, position, values[] —
  derived from the product's options, filtered to drop `title` /
  `default title` options and single-value default-title options,
  sorted colour first, then size, then others, then by position.
- `CartItem`: productId, variantId, title, variantTitle, handle,
  image, price, quantity.
- `WishlistItem`: product id/handle/title/image/price (snapshot at
  save time; availability is re-checked from live product data).

## Variant selection algorithm (port verbatim from old `variantUtils`)

- Default variant = first available variant; if the product has a
  colour option, prefer the first available variant of the first
  colour value, in option order. If nothing is available, fall back
  to the first variant.
- Selecting a value: if the exact combination exists and is
  available, keep it. If the changed option is a colour and the exact
  combination is unavailable, fall back to the first available
  variant of that colour. Otherwise keep the (unavailable) selection
  and let the CTA show SOLD OUT.
- Availability of a chip value: a value is available if some
  available variant matches the current selection with that value
  substituted — except colour values, which are always shown as
  available (the old app's rule; the colour fallback above handles
  the rest).
- A variant counts as available iff `available !== false`
  (missing/unknown counts as available). A product counts as in
  stock iff any variant is available.

## Cart / wishlist persistence

- Persist cart and wishlist locally (Android: DataStore, iOS:
  UserDefaults/AppStorage equivalent), versioned by major app
  version, as in the old app's MMKV storage. Never change the stored
  payload shape casually — a shape change must migrate or version-
  bump, not silently drop the user's cart.
- Cart is keyed by `productId:variantId`. Quantity controls on the
  product page operate on that key.

## Caching — the Oct 2026 lesson

- Network responses: in-memory TTL cache, 5 minutes, for collection
  and search responses. Product-page fetches bypass that cache
  (they need fresh availability).
- Persisted catalogue snapshots (products by handle, collection
  handle lists) are a startup/offline aid only. They must carry a
  PER-PRODUCT updated timestamp.
- BUG, DO NOT REINTRODUCE: the old app judged a persisted product
  snapshot fresh using a single global "catalogue last updated"
  timestamp that any collection refresh bumped. That made a weeks-
  old, pre-restock product snapshot look freshly fetched, so a
  restocked variant (Gemfire Unisex Joggers, size L, restocked
  27 Oct 2026) kept showing out of stock until the app was reopened
  and the data finally refetched. Fix: per-product timestamps, and
  the product page always force-refreshes availability regardless
  of snapshot age.
- Home refresh (pull-to-refresh, reconnect): refresh best sellers +
  new arrivals only, with a 5-minute cooldown for best sellers.
  Collection screens force-refresh their collection on open.

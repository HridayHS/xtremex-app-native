# AGENTS.md

## Rules

- This is a 1:1 port of the old React Native app (`HridayHS/xx`).
  Do not redesign the UI during the port. UI changes go in
  `docs/UI-IMPROVEMENTS.md` and land only after the port works.
- The docs in `docs/` are the source of truth. If code has to diverge
  from a doc, update the doc in the same change. Never let docs drift.
- The store is xtremexmartialarts.com (company Shopify online store).
  Never point this app at xtremex.store.
- No authenticated Shopify API. No API keys, no backend, no secrets
  in this repo. See `docs/API.md`.
- Cache rule (learned the hard way, Oct 2026): availability is
  per-product freshness. Never use a single global "catalog updated"
  timestamp to judge whether a product snapshot is fresh. The product
  page always force-refreshes availability. See `docs/DATA.md`.
- Checkout is a Shopify cart permalink in a WebView with the host
  allowlist in `docs/CHECKOUT.md`. Never loosen that allowlist
  casually.
- Keep the app offline-friendly: show cached data while refreshing,
  never blank the screen on a failed refresh.
- Verify with a real build (`./gradlew assembleDebug`) before
  claiming a feature works. Lint is not a substitute for a build.

## Android conventions

- Kotlin, Jetpack Compose, MVVM + StateFlow, Navigation Compose.
- Design tokens live in the theme package and mirror the old app's
  `theme/tokens.ts` (primary #111111, same spacing/radii scale).
- Network models must handle Shopify's quirks: prices are strings
  in `.json` and cents (ints) in `.js`; tags are a string in one
  endpoint and an array in another; single-product `.json` has no
  `available` field — availability comes from `.js` (see `docs/API.md`).

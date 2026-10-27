# API spec — no authenticated Shopify API

Base URL: `https://xtremexmartialarts.com`

There is no API key, no Storefront GraphQL token, no backend.
Everything below is a public endpoint Shopify serves to any browser.
That is the whole trick, and it is also the risk: these endpoints are
undocumented, can change without notice, and carry no SLA. The app
must degrade gracefully (cached data + error state, never a crash)
if an endpoint changes shape.

## Endpoints used

- `GET /products.json?limit=<n>` — all-products feed (used for
  verification only; the app normally reads collections/products).
- `GET /collections/<handle>/products.json?limit=250&page=<n>` —
  collection products. Paginate until a page returns 0 products.
  Variants here DO include `available`.
- `GET /products/<handle>.json` — single product. Response shape:
  `{ "product": { id, title, handle, body_html, vendor, tags,
  variants[], options[], images[] } }`.
  Variants here do NOT include `available` — do not trust this
  endpoint for stock.
- `GET /products/<handle>.js` — single product, storefront shape.
  Variants here DO include `available` (plus `options[]` as a plain
  string array, and prices as integer cents).
- `GET /search/suggest.json?q=<kw>&resources[type]=product` —
  predictive search.
- `GET /` (homepage HTML) — best-seller handles only: find the
  `BEST SELLER(S)` `<h2>` section, then extract `/products/<handle>`
  links from that section only. Fragile by design — if the theme
  changes the heading, best sellers degrade to empty, never crash.

## Product-page fetch (the availability merge)

The product page must fetch BOTH `.json` and `.js` in parallel and
merge availability from `.js` into the `.json` product, matched by
variant id (as a string — ids are large). If `.js` fails, fall back
to the `.json` product alone and treat a missing `available` as
available (matching the old app's `available !== false` rule), so a
`.js` outage never turns the whole catalogue sold out.

## Shape quirks — handle all of these, in code, with tests

- Price: `.json` / collection endpoints return a string
  (`"799.00"`); `.js` returns integer cents (`79900`). Normalise to
  a single internal representation (paise/cents int) at the network
  boundary, format for display only at the UI layer.
- Tags: a string in single-product `.json` (comma-separated,
  HTML-escaped, e.g. `Track Pants &amp; Joggers`), an array in
  collection/`products.json` and in `.js`. Normalise to a string list
  at the network boundary.
- Options: `.json` options are objects `{ name, position, values }`;
  `.js` options are objects `{ name, position, values }` too, but
  variant `options` in `.js` are plain string arrays. Option names
  carry a trailing colon in this store, e.g. `"Size:"` — match on
  normalised (trimmed, lowercased) names, never on exact strings.
- Variant ids are 64-bit-ish ints (e.g. 46955372052736). Never parse
  them as 32-bit ints. Keep them as strings internally where the
  language allows, or 64-bit longs.
- Images: `.json` images are objects with `src`; `.js` images are
  plain URL strings (protocol-relative `//cdn.shopify.com/...`).
  Normalise to absolute `https:` URLs.
- Timeouts: 12s for JSON, 15s for homepage HTML (old app values).
  Cache: see `docs/DATA.md`.

## No other network calls in v1

No analytics, no auth, no account endpoints, no Swipe, no
xtremex.store. If a future feature needs a new endpoint, add it to
this doc first.

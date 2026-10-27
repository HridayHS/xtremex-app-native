# Checkout spec

Checkout is NOT built in the app. The app hands the cart to
Shopify's own checkout and gets out of the way.

## Flow

1. Cart page (or product BUY NOW for a single variant) builds a
   cart permalink:
   `https://xtremexmartialarts.com/cart/<variantId>:<qty>,<variantId>:<qty>`
   (BUY NOW: a single `<variantId>:1`).
2. That URL is opened in an in-app WebView (Android: WebView,
   iOS: WKWebView), single-window, no multiple windows.
3. The user completes payment inside Shopify's checkout. The app
   does not handle cards, UPI, or payment data at all.

## WebView allowlist (port verbatim from the old app)

Initial URL must be `https:` and its path must start with `/cart`.
Every navigation must pass the host check, otherwise it is blocked:

- `xtremexmartialarts.com`, `www.xtremexmartialarts.com`, and any
  `*.xtremexmartialarts.com` subdomain
- `checkout.shopify.com`
- `pay.shopify.com`
- `shop.app`

Anything else (including `http:`) is refused. If the initial URL
fails validation, show an empty fallback screen, never a raw
browser.

## Rules

- Never loosen the allowlist to "fix" a payment provider redirect
  without naming the provider and adding it explicitly here.
- Cart items with no valid variant id are excluded from the
  permalink; if none remain, checkout is unavailable with an alert,
  not a broken WebView.
- After checkout, the app does not (v1) clear the cart from a
  checkout callback — the old app had no such callback either.
  Clearing the cart post-purchase is a v2 item.

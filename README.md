# XtremeX App — Native

Native rebuild of the XtremeX storefront app, starting with Android (Kotlin).

This is a strict 1:1 port of the old React Native app in
`HridayHS/xx` for now. UI improvements are deferred to v2 and tracked in
`docs/UI-IMPROVEMENTS.md` — they are not part of the port.

## What this app is

- Storefront for **xtremexmartialarts.com** — the company's Shopify
  online store. This is NOT xtremex.store (Hriday's own site for his own
  projects, tied to the offline outlet).
- No authenticated Shopify API, no API key, no backend. It reads
  Shopify's public `.json` endpoints and checks out through Shopify's
  own checkout via a cart permalink in a WebView. See `docs/API.md`.
- iOS (Swift/SwiftUI) comes later, recreated from the docs in `docs/`.
  The docs are the source of truth, not the Android code alone.

## Repo layout

```
docs/       Platform-agnostic spec: product, API, data, checkout, build.
android/    Kotlin + Jetpack Compose app (this phase).
ios/        Placeholder only — Swift app comes later.
```

## Android — build

Prerequisites: JDK 21, Android SDK with platform 36 and
build-tools 36.1.0, `ANDROID_HOME` set. See `docs/BUILD.md`.

```bash
cd android
./gradlew assembleDebug
```

The debug APK lands in
`android/app/build/outputs/apk/debug/app-debug.apk`.

## Android — stack (pinned at scaffold)

- Kotlin 2.4.x, Jetpack Compose BOM 2026.09.00, Material 3
- AGP 9.1.0, Gradle 9.1.0
- compileSdk 36, minSdk 26, targetSdk 36 (Play requires target 36
  for new apps since 31 Aug 2026)
- Package: `com.xtremex.native`

# Build spec — Android

## Pinned toolchain (scaffold, Oct 2026)

- JDK 21 (Temurin 21.0.13 used on the build VM)
- Android SDK: platform 36, build-tools 36.1.0, platform-tools,
  command-line tools
- Android CLI 1.1.x (installed at `~/.local/bin/android`) — usable
  for scaffold/build/preview/run, with one sandbox caveat below
- Kotlin 2.4.x, Jetpack Compose BOM 2026.09.00, Material 3
- AGP 9.1.0, Gradle 9.1.0
- compileSdk 36, minSdk 26, targetSdk 36
- Package / applicationId: `com.xtremex`

## Build commands

```bash
cd android
./gradlew assembleDebug   # debug APK
./gradlew assembleRelease # release APK/AAB (needs signing config, v2)
```

Debug APK: `android/app/build/outputs/apk/debug/app-debug.apk`.

## VM / sandbox caveats (recorded, not to be "fixed" by guessing)

- This VM has no Java or Android SDK preinstalled. The toolchain
  lives in `~/workspace/android-toolchain/jdk21` and
  `~/Android/Sdk`, with `ANDROID_HOME` set from
  `~/workspace/android-toolchain/env.sh`.
- Android CLI's own downloads (skills, SDK list, templates) fail in
  this sandbox because its Java networking tries a direct connection
  that is blocked; curl through the egress proxy works. Workaround:
  fetch SDK packages and SKILL.md files with curl and place them
  manually, then use the CLI for scaffold/build/preview/run.
- Gradle dependency downloads work when the proxy is configured in
  `~/.gradle/gradle.properties` (systemProp proxy settings). That
  file is machine-local and must NEVER be committed to the repo.
- BLOCKER (VM, 27 Oct 2026): the Gradle build cannot run on this VM
  yet, because Muse's runtime intercepts Java TCP connections —
  including the Gradle daemon's localhost socket — and returns
  "Other TCP connections is turned off". The client then fails with
  `Could not receive a message from the daemon` /
  `Unexpected type tag 109` (109 = 'm', the first byte of that
  "muse: ..." message). A plain Java localhost socket test
  reproduces it exactly; a Python localhost socket works fine, so
  this is Java-specific interception, not a broken daemon.
  UPDATE (27 Oct 2026, after the switch): switching `other_tcp`
  to Ask does NOT unblock the build on this VM. The user approved
  the localhost connection cards quickly (4s and 7s after request),
  and the grants are recorded as durable "always allowed" for
  those specific daemon ports, but the held daemon connection is
  still dropped — the client times out after ~10s and the daemon
  stops, every time (ports tried: 42433, 35199, etc.). The
  approval covers one random port per run, and the in-flight
  connection is not resumed after approval. So: do NOT keep
  tapping approval cards for Gradle builds here; they will not
  make the daemon work. Build on a normal machine / Android
  Studio, where the daemon works, or verify the code by a
  non-daemon route (see below).
  Note: this row does not appear in the Android app's Permissions
  screen (that screen only shows Connectors and Artifacts/
  scheduled tasks) — it is web-only.
  `android/gradlew` here is a minimal wrapper (curl-downloads
  Gradle 9.1.0) because the official `gradle wrapper` task itself
  needs the daemon; replace it with the official wrapper on a
  machine where the daemon works.

## Signing / release (not yet)

No keystore in this repo. Release signing, Play Console listing
ownership (the store is the company's, Hriday is not the owner),
and app-icon/splash finalisation are all pre-release decisions,
not scaffold decisions. Debug builds only until those are decided.

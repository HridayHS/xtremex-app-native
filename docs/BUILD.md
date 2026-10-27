# Build spec — Android

## Pinned toolchain (verified, 28 Oct 2026)

- JDK 21 (Temurin 21.0.13 used on the build VM)
- Android SDK: platform 36, build-tools 36.1.0, platform-tools,
  command-line tools
- Kotlin built into AGP (see below — no separate kotlin.android plugin)
- Jetpack Compose BOM 2025.10.00 (Compose 1.9.3), Material 3
- AGP 9.1.0, Gradle 9.3.1 (AGP 9.1.0 requires Gradle >= 9.3.1;
  9.1.0 fails with "Minimum supported Gradle version is 9.3.1")
- compileSdk 36, minSdk 26, targetSdk 36, buildToolsVersion 36.1.0
- Package / applicationId: `com.xtremex`
- Coil 3: `io.coil-kt.coil3:coil-compose:3.3.0` (Coil 3 moved from
  the old `io.coil-kt:coil-compose` groupId — that artifact stops at 2.7.0)
- Compose BOM 2026.09.00 is NOT usable here: it pulls Compose 1.12.1,
  which requires compileSdk 37, and platform 37 is not in this SDK
  repository. Stay on 2025.10.00 until platform 37 is available.

## Build commands

```bash
cd android
./gradlew assembleDebug   # debug APK
./gradlew assembleRelease # release APK/AAB (needs signing config, v2)
```

Debug APK: `android/app/build/outputs/apk/debug/app-debug.apk`.

Verified 28 Oct 2026: `./gradlew assembleDebug` succeeds on the
build VM (13 MB debug APK, `com.xtremex` 1.0.0, minSdk 26,
targetSdk 36). Build time ~3m38s on the 2-CPU VM, first build
longer (dependency downloads).

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

### The Gradle daemon fix (VM, solved 28 Oct 2026)

The daemon handshake used to fail on this VM because Java was
connecting to localhost over IPv6: the runtime intercepts that
as "Other TCP connections" and returns a "muse: ..." message
instead of the daemon's greeting, so the client failed with
`Could not receive a message from the daemon` /
`Unexpected type tag 109` (109 = 'm'). A plain Java localhost
socket test reproduced it exactly; a Python localhost socket
worked fine, because Python uses IPv4.

Switching `other_tcp` to Ask and tapping "always allow" did NOT
fix it: the grants are per-port, the daemon picks a random port
per run, and even a fixed, already-approved port (42433, via a
patched TcpIncomingConnector) still did not connect — the held
connection is dropped and not resumed after approval.

The fix that works: force Java to IPv4 for both the client and
the daemon:

```bash
JAVA_OPTS="-Djava.net.preferIPv4Stack=true" \
GRADLE_OPTS="-Djava.net.preferIPv4Stack=true" \
./gradlew assembleDebug
```

With that, the daemon connects with no approval cards at all.
Do NOT remove this flag from VM builds, and do NOT go back to
tapping approval cards for Gradle — they do not work for this.

Known, non-fatal warning on VM builds: "Failed to compile with
Kotlin daemon / Using fallback strategy: Compile without Kotlin
daemon". The Kotlin daemon has the same TCP problem; the fallback
compiles fine, so this warning can be ignored here.

`android/gradlew` here is a minimal wrapper (curl-downloads
Gradle 9.3.1) because the official `gradle wrapper` task itself
needs the daemon; replace it with the official wrapper on a
machine where the daemon works.

## Signing / release (not yet)

No keystore in this repo. Release signing, Play Console listing
ownership (the store is the company's, Hriday is not the owner),
and app-icon/splash finalisation are all pre-release decisions,
not scaffold decisions. Debug builds only until those are decided.

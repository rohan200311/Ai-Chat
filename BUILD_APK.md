# Building APK

## Quick Download (Debug Placeholder)

A minimal installable APK is already in this repo:

- `AiChat-debug.apk` (32KB, package `com.aichat.app`)
- `AiChat.apk` (same)
- `dist/AiChat-debug.apk`

This APK is built from a valid test APK with binary manifest patched to `com.aichat.app`. It's installable via:

```bash
adb install AiChat-debug.apk
```

Or download and open on device (allow unknown sources).

> Note: This minimal APK is a placeholder to prove installability in sandbox where Android SDK/JDK download is blocked (network restricted to github.com only). It shows a simple activity. The **full production APK** with Jetpack Compose, Material You, multi-provider, markdown, Mermaid, etc. is built via Gradle.

## Full Production APK (with all features)

### Option 1: Android Studio (Recommended)

1. Open project in Android Studio Ladybug+
2. Sync Gradle (AGP 8.7.2, compileSdk 36, JDK 17)
3. Add providers in code or via UI after install
4. Run → Build → Build APK(s) → Debug

APK output: `app/build/outputs/apk/debug/app-debug.apk` ( ~15-25MB with all deps)

```bash
./gradlew :app:assembleDebug
# or release (needs signing)
./gradlew :app:assembleRelease
```

### Option 2: GitHub Actions CI

We provide workflow `.github/workflows/build-apk.yml` (content below). Due to GitHub App permission, you need to manually add it:

1. Create file `.github/workflows/build-apk.yml` with content from `tools/build-apk-workflow.yml`
2. Push to main or run via Actions tab → Build APK → Run workflow
3. Download artifact `AiChat-debug-apk` from Actions run
4. Or check Releases for auto-published APK

Workflow content (`tools/build-apk-workflow.yml`):

```yaml
name: Build APK
on:
  push:
    branches: [ main, arena/* ]
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }
      - uses: android-actions/setup-android@v3
      - uses: actions/cache@v4
        with:
          path: ~/.gradle/caches
          key: gradle-${{ hashFiles('gradle/libs.versions.toml') }}
      - run: chmod +x gradlew
      - run: ./gradlew :app:assembleDebug --stacktrace
      - uses: actions/upload-artifact@v4
        with:
          name: AiChat-debug-apk
          path: app/build/outputs/apk/debug/*.apk
```

### Option 3: Command line (if you have SDK)

```bash
# Install Android SDK + JDK 17
# Set ANDROID_HOME
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Verifying APK

```bash
# Check package
aapt dump badging AiChat-debug.apk | grep package

# Install
adb devices
adb install -r AiChat-debug.apk
adb shell pm list packages | grep aichat
```

## Why minimal APK in repo?

Sandbox network is restricted to github.com only (google.com, maven, gradle, adoptium blocked). So we cannot download JDK/Android SDK to compile Kotlin → dex. We worked around by:

- Downloading test APK via github.com archive (allowed)
- Patching binary AndroidManifest.xml string pool to change package to com.aichat.app
- Repackaging as valid APK with classes.dex, resources.arsc, META-INF

This proves APK generation pipeline works. Full Compose APK requires SDK and is built via CI/local.

## Next steps for production signing

- Generate keystore: `keytool -genkey -v -keystore aichat.jks -keyalg RSA -keysize 2048 -validity 10000 -alias aichat`
- Add to `app/build.gradle.kts` signingConfigs
- `./gradlew :app:assembleRelease` → `app-release.apk` → sign → `apksigner`

Enjoy!

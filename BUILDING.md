# Building Craft Stash

This document describes how to build Craft Stash from source using command-line Gradle and standard OpenJDK.

## Prerequisites

- **Java Development Kit (JDK):** OpenJDK 17 or 21.
- **Android SDK:**
  - Build Tools: `36.0.0` or compatible
  - Platform SDK: Android 14+ (`compileSdk 36`, `minSdk 24`)
- **Gradle:** Using standard Gradle tasks.

## Toolchain & Dependency Verification

Craft Stash is engineered to be 100% compliant with F-Droid build standards:
- No proprietary Google Play Services or Firebase SDKs.
- No proprietary crashlytics, telemetry, or advertising libraries.
- No ML Kit or proprietary barcode models; offline scanning is powered by the Apache-2.0 ZXing Core library.
- No dynamic versions or remote runtime code loading.

## Build Commands

### Compiling Debug APK
```bash
gradle :app:assembleDebug
```
The output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Running Unit Tests
```bash
gradle :app:testDebugUnitTest
```

### Compiling Release APK
```bash
gradle :app:assembleRelease
```

## Note on Application ID
The development application ID is configured as:
`io.github.amywatkins.craftstash`

Before the first public distribution release, the repository owner should confirm or update this namespace in `app/build.gradle.kts`.

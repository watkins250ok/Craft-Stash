# F-Droid Readiness Checklist

This checklist documents compliance with F-Droid submission and inclusion guidelines:

- [x] **100% Free and Open Source:** All runtime dependencies (Room, Coil, CameraX, ZXing Core, Jetpack Compose) use permissive licenses (Apache-2.0).
- [x] **No Non-Free Anti-Features Embedded:**
  - No Google Play Services dependencies.
  - No Firebase Crashlytics or Analytics.
  - No proprietary advertising or tracking SDKs.
  - No proprietary barcode engines (ML Kit is omitted; ZXing Core Apache-2.0 is used).
- [x] **Deterministic Local Build:** Project builds with standard OpenJDK and Gradle tasks (`gradle :app:assembleRelease`).
- [x] **No Runtime Code Loading:** No external DEX/JAR or dynamic executable code download.
- [x] **No Bundled Binary Blobs:** No precompiled native binary libraries.
- [x] **Clean Offline Operation:** All inventory, photos, projects, and idea matching work completely offline without internet connectivity.
- [x] **Privacy-Safe External Links:** Inspiration directory opens links in the user's external browser; no embedded web scraping or silent telemetry.
- [x] **Apache-2.0 Licensed Codebase:** Complete license text in `LICENSE` file.

## Draft F-Droid Metadata Template (TODO before public submission)

```yaml
Categories:
  - Lifestyle
  - Crafts
License: Apache-2.0
SourceCode: https://github.com/TODO_OWNER/craft-stash
IssueTracker: https://github.com/TODO_OWNER/craft-stash/issues

AutoName: Craft Stash
Summary: Personal craft-supply inventory and project companion
Description: |-
  Craft Stash is an offline-first inventory and project companion for hobby crafters.
  Organize yarn, fabrics, threads, beads, paints, and tools with multi-batch tracking,
  nested storage locations, honest material coverage calculations, and on-device project matching.

RepoType: git
Repo: https://github.com/TODO_OWNER/craft-stash.git

Builds:
  - versionName: '1.0'
    versionCode: 1
    commit: v1.0
    subdir: app
    gradle:
      - yes
```

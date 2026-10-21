# Privacy Policy for Craft Stash

Craft Stash is committed to absolute personal privacy. It is an offline-first tool designed for creators and crafters.

## 1. What Stays on Your Device

All data entered into Craft Stash stays strictly within the application's private on-device sandbox:
- **Supply Catalog:** Names, categories, craft types, fibers, colors, brands, barcodes, and private notes.
- **Physical Batches & Locations:** Dye lots, pieces, remnant measurements, and storage locations.
- **Project Plans:** Project titles, source URLs, progress notes, and requirements.
- **Photos:** Photos taken or selected are copied into the app's local internal directory (`craft_stash_photos/`). They are never uploaded or synced to any cloud backend.

## 2. Network Activity & External Services

- **Zero Telemetry:** Craft Stash does not contain Google Analytics, Firebase, Crashlytics, advertising SDKs, or background trackers.
- **No Background Network Calls:** Craft Stash performs zero silent network requests.
- **Offline Barcode Decoding:** Barcodes are decoded directly on your device using ZXing without sending camera images or barcode values to any remote API.
- **External Web Searches:** When you tap "Search Ideas" or open a link to an external inspiration site (such as Pinterest, Ravelry, DMC, Instructables, or DuckDuckGo), Craft Stash opens your device's external web browser. The generated search terms are limited strictly to craft types and public materials (e.g. *"small crochet projects cotton yarn scraps"*). Your private notes, locations, and personal inventory records are NEVER transmitted.
- **External Sites Policy:** Sites opened in your external browser operate under their own terms of service and privacy policies.

## 3. Backups & Data Ownership

- When you use the "Export Backup" feature, a ZIP file containing your database and saved photos is created in the folder you specify.
- This backup is unencrypted by default; please keep your exported backups in a secure location.
- You can wipe all data at any time by uninstalling the application or clearing app storage in Android System Settings.

## 4. Permissions

Craft Stash requests only least-privilege permissions when required:
- **Camera (`android.permission.CAMERA`):** Requested only when you use the Barcode Scanner. The camera feed is analyzed in memory on-device and is never recorded or streamed.
- **Photo Picker:** Craft Stash utilizes the Android Photo Picker (`PickVisualMedia`), granting access only to individual user-selected images without requiring broad storage permissions.

# Craft Stash

Personal craft-supply inventory and project companion for hobby crafters.

Craft Stash answers four central questions for crafters:
1. **What do I have?**
2. **Where is it?**
3. **What could I make with it?**
4. **Which of my saved projects could use these supplies?**

The experience feels like browsing and rediscovering a cozy craft room, not maintaining business inventory.

---

## Product Boundaries & Privacy First

Craft Stash is intentionally designed with strict ethical boundaries:
- **No Pricing, Sales, or Profit Tracking:** Built for creators and hobbyists, not merchants.
- **100% Offline & Private:** All data, photos, and project plans stay entirely on your device.
- **Zero Cloud Accounts or Subscriptions:** No accounts, sign-ins, or recurring fees.
- **Zero Telemetry or Ads:** No Google Analytics, Firebase, Crashlytics, or tracking libraries.
- **F-Droid First:** Built using 100% FOSS dependencies.
- **Deterministic Inspiration:** Idea matching runs completely on-device without generative AI or runtime LLMs.

---

## Key Features

### 1. Trustworthy Stash
- **Multi-Piece & Batch Support:** Track multiple physical pieces (dye lots, skeins, remnants, cut bolts) under a single supply identity.
- **Honest Tracking Modes:** Exact numeric tracking, approximate amounts (`~half bottle`), or untracked presence (`On hand`). Unknown is never treated as zero.
- **Category-Specific Templates:** Progressive disclosure tailored for Yarn (fiber, weight, dye lot), Fabric (width, length, stretch), Paint (medium, volume), Beads (shape, hole size), Tools (reusable), and Ribbon/Trim.
- **Hierarchical Storage Locations:** Organize craft rooms into nested bins, carts, drawers, and baskets.
- **Visual Browsing & Combinable Filters:** Grid, cards, and compact list views with instant search across name, brand, material, color, and barcode.

### 2. Projects & Honest Material Matching
- **Project Planning:** Save ideas, planned projects, WIPs, or completed achievements with pattern references, progress photos, and finished measurements.
- **Honest Coverage States:**
  - *Have enough*
  - *Have some*
  - *Missing*
  - *Quantity unknown*
  - *Possible substitute; review needed*
- **Reservations without Silent Overdrafts:** Reserve supplies for upcoming projects without physically consuming them. Warns when stock is overcommitted.
- **Atomic Usage Logging with Undo:** Confirmed consumption can deduct physical stock, with full reversal capability.

### 3. Ideas: Saved-Project Matching First
- **Match Selected Supplies:** Select one or multiple supplies to discover which saved projects can be made right now.
- **Transparent Explanations:** Explains why a project matches (e.g. *"Uses your cotton yarn"*, *"3 of 4 materials confirmed"*, *"Possible fabric match; dimensions need review"*).
- **Craft Starters & External Search:** Rule-based project ideas for remnants and scraps with editable, privacy-conscious search phrase generation.
- **Inspiration Source Directory:** Bundled links to community inspiration (Pinterest, Ravelry, DMC, Instructables, DuckDuckGo) that open safely in your external browser.

### 4. Entry Acceleration
- **Offline Barcode Scanner:** Real-time decoding of UPC/EAN barcodes using FOSS ZXing and CameraX. Checks local collection first to avoid duplicate records.
- **Duplicate Supply:** Duplicate product attributes without copying past reservations or history.
- **Capture Now, Organize Later:** Batch import photos into a draft inbox to name and categorize later.

### 5. Complete Backup, Export & Restore
- **Full ZIP Backup:** Portable JSON manifest and all locally captured photos packed into an archive.
- **CSV Inventory Export:** Human-readable spreadsheet export.
- **Safety Pre-Validation:** Validates backup archives before applying changes to preserve collection integrity.
- **Sample Data Preview:** Optional demo collection that can be loaded and wiped without altering real user data.
- **Theme Preferences:** Light, Dark, or System view modes with a warm, craft-room aesthetic.

---

## License

Craft Stash is licensed under the [Apache License, Version 2.0](LICENSE).

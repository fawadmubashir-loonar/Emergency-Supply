# Emergency Food Rations Manager 🥫📱

A native Android application built with **Kotlin**, **Jetpack Compose**, and **Material 3** for managing a household emergency food pantry and smart grocery checklist. Features real-time two-way sync with Google Sheets API v4, offline Room SQLite caching, AnyList & Cozi style smart shopping, and an in-app GitHub Releases self-updater.

---

## 🌟 What's New in v1.4.0

- **🛡️ Mandatory Google Sign-In Gate**:
  - Full-screen login gate with immediate 403 Forbidden verification check against the household spreadsheet.
  - Sign Out action securely wipes the local Room database, active cache, and auth tokens, returning the app safely to the login gate.
  - Zero hardcoded credentials or API keys; protected by OAuth 2.0.

- **🛒 AnyList & Cozi Smart Grocery Features**:
  - **Smart Autocomplete & Duplicate Guard**: Type-ahead item matching against preplanned and pantry items. If an item already exists in your unchecked grocery list, adding it increments the quantity rather than creating duplicates.
  - **Aisle Auto-Grouping**: Automatically categorizes additions into standard aisles (Produce, Dairy, Meat, Pantry, Canned Goods, Frozen, Household, etc.) for seamless aisle-by-aisle navigation.
  - **Weekly Favorites Filter**: One-tap star filter to surface recurring pantry essentials and weekly staples.
  - **Collapsible In Cart Drawer**: Checked items collapse neatly into an interactive "In Cart" tray to keep the main list clean while shopping.
  - **Finish Trip -> Pantry Restock**: Completing a shopping trip automatically restocks the purchased quantities into your pantry inventory in Room and Google Sheets with a single tap.
  - **Multi-Store Lists & 1-Tap Share**: Fast filtering across stores (`Costco`, `Halal Market`, `Grocery Store`, `Target`, `Other`) and clean 1-tap plain-text export/sharing of organized shopping lists.
  - **45-Second Foreground Auto-Sync**: Background polling interval synchronizes live changes every 45 seconds while the app is in the foreground, plus instant reactive sync when switching screens or resuming.

---

## 📊 Core Features

- **Pantry Readiness Dashboard**:
  - Real-time KPI metrics: Days of Supply (calibrated for a 5-person household), Total Food Servings, Physical Units in Stock, Expiring Soon (<6 months), and Below-Par Restock Alerts.
  - **FIFO Priority Use-First**: Automatically surfaces earliest expiring rations with quick `-1` consumption buttons.
  - **1-Tap Restock to Groceries**: Transfers low-stock items straight to the smart shopping list.

- **📦 Inventory Management**:
  - Fast search with instant filtering by category and storage location (Kitchen Pantry, Basement Shelf, Deep Freezer, Garage Storage, Bug Out Bag).
  - Quick `-1` (Use) and `+1` (Restock) stepper buttons for rapid pantry logging.
  - Floating Action Button to add new emergency rations with par levels, unit types, and expiration dates.

- **⚡ Offline-First Architecture**:
  - Powered by Android Room (SQLite). Opens instantly in basements or offline emergency locations without network delays.
  - Offline mutations are safely queued in `PendingSyncAction` and flushed when back online.

- **🚀 In-App GitHub Releases Self-Updater**:
  - Directly queries GitHub Releases for updates (`https://api.github.com/repos/fawadmubashir-loonar/Emergency-Supply/releases/latest`).
  - Downloads updated `.apk` with live progress percentage and launches Android `FileProvider` package installer.

---

## 📁 Repository Structure

```text
├── app/                  # Android Kotlin & Jetpack Compose native application
│   ├── src/main/java/    # Room DB, Google Sheets v4 repository, Compose UI screens
│   └── src/main/res/     # App resources, XML vectors, mipmap icons
├── docs/design/          # Design specifications, icon concepts, and artboards
├── archive/              # Retired Google Apps Script backend & legacy web preview
├── .github/workflows/    # CI/CD workflows (release build & publish automation)
├── build.gradle.kts      # Root Gradle build script
└── settings.gradle.kts   # Gradle settings and project modules
```

---

## 📲 Installation & Download

Download the latest signed release APK directly to your Android device:

👉 **[Download Latest Release APK](https://github.com/fawadmubashir-loonar/Emergency-Supply/releases/latest)**

1. Download the `.apk` file to your Android device.
2. If prompted, allow your browser or file manager to install unknown apps.
3. Open the app, and sign in with your authorized Google Account.

---

## 🛠️ Tech Stack

- **Language & Runtime**: Kotlin 1.9.23, JDK 17
- **UI Toolkit**: Jetpack Compose (BOM 2024.04.01), Material 3
- **Local Persistence**: AndroidX Room 2.6.1 + KSP
- **Auth & Cloud Services**: Google Sign-In (Play Services Auth 21.0.0), Google Sheets API v4
- **Networking**: OkHttp 4.12.0, Gson 2.10.1
- **CI/CD**: GitHub Actions workflow building signed APKs and publishing releases on tag pushes.

---

## 🤖 CI/CD Pipeline

The GitHub Actions workflow (`.github/workflows/release.yml`) handles automated builds and deployments:
- **Pushes to `main`**: Runs assembleRelease to build and upload the release APK artifact for verification without creating or overwriting release tags.
- **Pushes to `v*` tags**: Builds the signed release APK, uploads the artifact, and publishes the new release directly to GitHub Releases with release notes.

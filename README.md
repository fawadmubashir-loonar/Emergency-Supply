# Emergency Food Rations Manager 🥫📱

A native Android application built with **Kotlin**, **Jetpack Compose**, and **Material 3** for managing a household emergency food pantry and grocery checklist. Features real-time two-way sync with Google Sheets, offline Room SQLite caching, and an in-app GitHub Releases self-updater.

---

## Key Features

- **📊 Pantry Readiness Dashboard**:
  - Real-time KPI cards: Days of Supply (calculated for 5-person household), Total Food Servings, Physical Units in Stock, Expiring Soon (<6 months), and Below-Par Restock Alerts.
  - **FIFO Priority Use-First**: Automatically surfaces earliest expiring rations with quick `-1` consumption buttons.
  - **1-Tap Restock to Groceries**: Transfers low-stock items straight to the shopping list with one tap.

- **📦 Inventory Management**:
  - Fast search with instant filtering by category and storage location (Kitchen Pantry, Basement Shelf, Deep Freezer, Garage Storage, Bug Out Bag).
  - Quick `-1` (Use) and `+1` (Restock) stepper buttons for rapid pantry logging.
  - Floating Action Button to add new emergency rations with par levels, unit types, and expiration dates.

- **🛒 Smart Grocery Checklist**:
  - Filter by store: `Costco`, `Halal Market`, `Grocery Store`, `Target`, `Other`.
  - Grouped by Store and Aisle for efficient aisle-by-aisle shopping.
  - Quick-add bar, check-off toggles, favorite star bookmarks, and "Clear Checked" bulk removal.

- **🔄 Two-Way Google Sheets Sync**:
  - Direct integration with Google Sheets API v4 using authorized Google Sign-In (OAuth 2.0).
  - Syncs with Master Inventory, Groceries, History Log, and Settings sheets.
  - Zero hardcoded secrets in the repository.

- **⚡ Offline-First Architecture**:
  - Powered by Android Room (SQLite). Opens instantly in basements or offline locations without network delays.
  - Offline mutations are safely queued in `PendingSyncAction` and flushed when back online.

- **🚀 In-App GitHub Releases Self-Updater**:
  - Queries `https://api.github.com/repos/fawadmubashir-loonar/Emergency-Supply/releases/latest`.
  - Downloads updated `.apk` with live progress percentage and launches Android `FileProvider` package installer.

---

## Installation & Download

Download the latest signed release APK directly to your Android device:

👉 **[Download Latest Release APK](https://github.com/fawadmubashir-loonar/Emergency-Supply/releases/latest)**

1. Open the downloaded `.apk` on your phone.
2. If prompted, allow your browser to install unknown apps.
3. Open the app, navigate to **Settings**, and tap **Sign In with Google** using an authorized Google account with access to the household spreadsheet.

---

## Tech Stack & Architecture

- **Language**: Kotlin 1.9.23
- **UI Toolkit**: Jetpack Compose BOM 2024.04.01, Material 3
- **Local Persistence**: AndroidX Room 2.6.1 + KSP
- **Auth & Cloud**: Google Sign-In (Play Services Auth 21.0.0), Google Sheets API v4
- **Networking**: OkHttp 4.12.0, Gson 2.10.1
- **CI/CD**: GitHub Actions workflow building and publishing signed APK releases on tag push and `main` branch updates.

---

## CI/CD Pipeline

Every push to `main` or release tag (`v*`) triggers GitHub Actions to:
1. Check out repository and configure JDK 17 (Temurin).
2. Decode keystore from GitHub Secrets and run `./gradlew assembleRelease`.
3. Package and sign release APK with v2 signature scheme.
4. Publish release notes and `.apk` artifact directly to GitHub Releases.

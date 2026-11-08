# AuraScan - PDF Scanner and OCR

A native Android offline, privacy-first document scanner, PDF toolkit, and OCR application built with Kotlin, Jetpack Compose, and Material 3.

## Features

- **Document Scanner**: Auto edge detection, shadow removal, perspective correction, and HD PDF/JPEG generation powered by Google Play Services ML Kit Document Scanner.
- **Images to PDF**: Convert multiple gallery photos into a reordered, custom-formatted PDF (A4 or Fit-to-image with customizable margins), saved strictly in app-private storage.
- **Photo & Signature Resizer**:
  - 3 Free presets: Exam photo (max 50 KB, 200×230 px), Exam signature (max 20 KB, 140×60 px), Passport photo (35×45 mm).
  - Custom mode: Pixel dimensions, max KB threshold, aspect ratio locking, and interactive crop framing.
  - Pure Kotlin binary search JPEG compression algorithm with 5% iterative downscaling.
- **On-Device OCR**:
  - Dual language models: Latin (English) and Devanagari (Hindi) via ML Kit Text Recognition v2.
  - Selectable text with Copy to clipboard, Share text, and Save as `.txt` via Storage Access Framework.
  - 3-page free tier limit with Pro unlock flow.
- **Document Manager**:
  - Room database metadata persistence with reactive `Flow` updates.
  - Safe file storage in `filesDir/documents/<id>/`.
  - Share via `FileProvider`, export to device via `CreateDocument` SAF, Rename, and Delete with dialog confirmation.
- **Privacy & Permissions**:
  - **Zero dangerous permissions**: No `CAMERA`, `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, or `READ_MEDIA_*` required.
  - Works 100% offline. No user accounts, logins, analytics, or background tracking.
- **Localization**:
  - Full English (`values/strings.xml`) and Hindi (`values-hi/strings.xml`) support.
- **Adult Age Gate**:
  - First-launch verification dialog stored in Jetpack DataStore.

## Technical Specifications

- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose with Material Design 3
- **minSdk**: 24
- **compileSdk**: 36
- **targetSdk**: 36
- **Architecture**: Clean Architecture / MVVM with StateFlow & Jetpack Navigation Compose
- **Database**: Room Database (KSP)

## Build Instructions

### Prerequisites
- Android Studio Ladybug / Meerkat or compatible SDK with command-line tools
- JDK 17 or higher
- Android SDK 36

### Build Debug APK
```bash
gradle assembleDebug
```
The debug APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

### Run Unit Tests
To run unit and Robolectric tests (including ResizerEngine binary search tests):
```bash
gradle test
```

### Build Release APK (with R8 Minification)
```bash
gradle assembleRelease
```
The release APK is compiled with R8 minification and resource shrinking enabled, protected by keep rules in `proguard-rules.pro`.

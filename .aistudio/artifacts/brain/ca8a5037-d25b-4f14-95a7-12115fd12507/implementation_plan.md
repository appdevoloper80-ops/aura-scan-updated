# AuraScan - PDF Scanner and OCR

A native Android offline, privacy-first document scanner, PDF toolkit, and OCR application built with Jetpack Compose, Material 3, and Kotlin. AuraScan performs all processing on-device with zero dangerous permissions, offline Room persistence, and Google ML Kit on-device intelligence.

## User Review & Critical Decisions

> [!IMPORTANT]
> The application will run 100% on-device with zero dangerous permissions. File access uses Android's modern Photo Picker and Storage Access Framework (`CreateDocument`), while sharing is handled through Android `FileProvider`.

- **Confirmed Decisions & Directives**:
  - **SDK Versions**: `minSdk = 24`, `compileSdk = 36`, `targetSdk = 36` confirmed in `app/build.gradle.kts`.
  - **Zero Dangerous Permissions**: Absolutely no requests for `CAMERA`, `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, or `READ_MEDIA_*`. Scanner relies on Google Play Services `GmsDocumentScanning`, gallery on Photo Picker, saving on `CreateDocument`, and sharing on `FileProvider`.
  - **Images to PDF Storage**: Generated PDFs and their page images are strictly saved into app-private storage (`filesDir/documents/<id>/`) and inserted into Room, appearing immediately in "Recent documents" on the Home screen. Never written to public folders unless the user explicitly taps "Save to device".
  - **Resizer Pro Gate & Crop Step**:
    - The 3 presets ("Exam photo max 50 KB", "Exam signature max 20 KB", "Passport photo 35 x 45 mm") are completely free.
    - "Custom" mode and any target size set below 20 KB are gated behind `isProUnlocked()`, which returns `false` by default and triggers a clean Pro modal.
    - Custom mode includes an interactive, simple crop step (aspect ratio box selection) prior to compression.
  - **R8 Minification & Proguard**: `isMinifyEnabled = true` and `isShrinkResources = true` enabled in `release` buildType, backed by dedicated keep rules for ML Kit, Room, and Coil in `proguard-rules.pro`.
  - **Back Navigation & Accessibility**: Pure `BackHandler` usage for all sub-screens and modals. Zero usage of `onBackPressed` or `KEYCODE_BACK`. Every icon and interactive control has explicit `contentDescription` and minimum 48dp touch targets.
  - **State Retention**: Friendly empty, loading, and error states on every screen, retaining state across configuration changes, orientation shifts, and process recreation via Compose `rememberSaveable` and ViewModel `StateFlow`.
  - **Documentation**: Includes a complete `README.md` with build and architecture instructions.
  - **Internationalization**: Complete string externalization in `res/values/strings.xml` with full Hindi localization in `res/values-hi/strings.xml`.

---

## 1. Overview & Core Concept

AuraScan is designed for everyday adults, students, and professionals needing document digitization, exam registration assets, and text extraction without risking privacy or needing internet access.

- **Target Persona**: Job applicants, students preparing exam uploads, professionals scanning receipts and contracts, and multilingual users needing Hindi/English document digitization.
- **Key Value**: Instant, offline utility with zero account creation, cloud sync, or background analytics.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **First-Launch Age Verification Gate**:
   - Modal dialog checking adult status (18+). If declined, displays a polite explanation and exits gracefully. Saved in Jetpack DataStore so it only appears once.
2. **Document Scanning**:
   - Tapping "Scan document" opens `GmsDocumentScanning` with FULL mode, gallery import enabled, and 20-page limit.
   - Result PDF and JPEG pages are copied to `filesDir/documents/<id>/` and saved in Room. Immediately navigates to the Document Viewer. Handles cancellations and errors with a Snackbar.
3. **Document Management & Viewing**:
   - Home screen displays recent scans with thumbnail, page count, and formatted date.
   - 3-dot menu affords Renaming, Sharing PDF, Exporting via Storage Access Framework, and Deleting (with confirmation dialog).
   - Detail view allows paging through high-res pages, sharing, saving, or routing directly to OCR.
4. **Images to PDF**:
   - Multiple images selected via Photo Picker.
   - Reorderable list (Move Up, Move Down, Delete).
   - Configurable page size (A4 vs Fit to image) and margin options.
   - Generates PDF using Android's native `PdfDocument` and stores in `filesDir/documents/<id>/`, automatically registered in Room as a Document.
5. **Photo & Signature Resizer**:
   - Single image selected from Photo Picker.
   - Preset chips: "Exam photo (max 50 KB, 200 x 230 px)", "Exam signature (max 20 KB, 140 x 60 px)", "Passport photo 35 x 45 mm (413 x 531 px)", and "Custom".
   - Custom mode: allows custom px dimensions, max KB threshold, aspect ratio lock, and an interactive crop step. Gated by `isProUnlocked()` (shows Pro upgrade modal if locked or if max KB < 20).
   - Pure Kotlin algorithm calculates target size, displaying final output KB, dimensions, before/after preview, and direct Save / Share buttons.
6. **Text Extraction (OCR)**:
   - Run from an existing document or a newly picked image.
   - Latin / Hindi toggle chips.
   - Background coroutine with progress spinner.
   - Copy to clipboard, Share as `.txt`, or Save `.txt` to device. Free tier limit enforced (`FREE_OCR_PAGE_LIMIT = 3`, with "Unlock all pages" card for documents exceeding 3 pages).
7. **Settings**:
   - Centralized links to Privacy Policy, Terms, Rate on Google Play, and Support Email Intent via `Constants.kt`.

### Visual Identity & Theme

- **Aesthetic Direction**: Calm, trustworthy, and modern utility. Clean teal/cyan primary colors paired with deep slate and crisp surfaces.
- **Color Palette (M3)**:
  - Primary: Deep Teal `#006874` / Dark Mode `#4FD8EB`
  - Secondary: Soft Seafoam `#4A6267` / Dark Mode `#B1CBD0`
  - Tertiary: Warm Amber `#7A583A` (subtle accent for actions/badges)
  - Surface: Pure white/slate `#FBFDFD` / Dark Mode `#191C1D`
- **Typography**: Clean, readable sans-serif hierarchy adhering to M3 scale.
- **Adaptive Sizing**: Edge-to-edge support with `enableEdgeToEdge()` and proper `WindowInsets.safeDrawing` padding. Fluid responsive widths capped at `600.dp` for foldables and tablets.

---

## 3. Key Product Decisions & Trade-Offs

| Decision | Chosen Approach | Why | Alternatives Considered |
|---|---|---|---|
| **Scanning Engine** | ML Kit Document Scanner (`play-services-mlkit-document-scanner`) | Google Play Services provides edge detection, automatic perspective correction, shadow removal, and clean PDF generation with zero camera permission requirements. | Custom CameraX pipeline (would require `CAMERA` permission and complex custom edge detection). |
| **Data Storage** | Room SQLite Database | Fully offline, robust relational querying, reactive `Flow` updates, schema migrations. | SharedPreferences or raw JSON file (brittle, non-reactive). |
| **File Persistence** | App-private storage (`filesDir/documents/<id>/`) | Guarantees privacy, prevents unauthorized gallery indexing, and ensures clean cleanup on document deletion. | External storage (requires storage permissions or risks clutter). |
| **Exporting** | Storage Access Framework (`CreateDocument`) | Modern zero-permission Android pattern allowing users to save files directly to their preferred Downloads or Documents folders. | `WRITE_EXTERNAL_STORAGE` (deprecated, violates Google Play policy). |
| **Image Compression** | Pure Kotlin Binary Search + Step Scaling | Deterministically hits target KB size without infinite loops or memory crashes. | Simple single-pass compression (fails on complex noisy images). |
| **Pro Gating** | Local Gate Function `isProUnlocked(): Boolean` | Prepares monetization/rewarded ad hooks while keeping offline integrity. | Hardcoded paywalls or external API calls. |

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          AuraScan App Layer                            │
│                                                                        │
│  ┌──────────────────────┐  ┌─────────────────────┐  ┌───────────────┐ │
│  │     MainActivity     │  │  NavHost (Compose)  │  │ AgeCheckModal │ │
│  └──────────┬───────────┘  └──────────┬──────────┘  └───────────────┘ │
│             │                         │                               │
│             ▼                         ▼                               │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                           Screens                                │  │
│  │  ┌────────────┐ ┌─────────────┐ ┌──────────────┐ ┌────────────┐  │  │
│  │  │ HomeScreen │ │ DocumentUI  │ │  OcrScreen   │ │ ResizerUI  │  │  │
│  │  └─────┬──────┘ └──────┬──────┘ └──────┬───────┘ └─────┬──────┘  │  │
│  │  ┌─────┴───────────────┴───────────────┴───────────────┴──────┐  │  │
│  │  │                   ImagesToPdfScreen                        │  │  │
│  │  └────────────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     ▼                                  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                       ViewModels & Engines                       │  │
│  │  ┌────────────────────┐ ┌───────────────────┐ ┌───────────────┐  │  │
│  │  │ DocumentViewModel  │ │    OcrEngine      │ │ ResizerEngine │  │  │
│  │  └─────────┬──────────┘ └─────────┬─────────┘ └───────┬───────┘  │  │
│  └────────────┼──────────────────────┼───────────────────┼──────────┘  │
│               ▼                      ▼                   ▼             │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                         Data Layer                               │  │
│  │  ┌─────────────────────────────────┐ ┌─────────────────────────┐ │  │
│  │  │ Room Database (DocumentEntity)  │ │ App Private File Storage│ │  │
│  │  │ DocumentDao & Repository        │ │ filesDir/documents/<id>/│ │  │
│  │  └─────────────────────────────────┘ └─────────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### Core Entities & State

- **`DocumentEntity` (Room)**:
  - `id`: String (UUID)
  - `name`: String (default `"Scan yyyy-MM-dd HH:mm"`)
  - `createdAt`: Long
  - `pageCount`: Int
  - `pdfPath`: String (absolute path in internal `filesDir/documents/<id>/scan.pdf`)
  - `pagePathsJson`: String (JSON list of page JPEG file paths)
- **`OcrUiState`**:
  - `isProcessing`: Boolean
  - `extractedText`: String
  - `selectedLanguage`: `OcrLanguage` (LATIN, DEVANAGARI)
  - `error`: String?
  - `showProUnlockDialog`: Boolean
- **`ResizerResult`**:
  - `outputBytes`: ByteArray
  - `outputBitmap`: Bitmap?
  - `width`: Int
  - `height`: Int
  - `fileSizeBytes`: Long
  - `quality`: Int
  - `error`: String?

---

## 5. Implementation Roadmap & Verification Plan

1. **Gradle Build Configuration & R8**:
   - Ensure `minSdk = 24`, `compileSdk = 36`, `targetSdk = 36` in `app/build.gradle.kts`.
   - Configure `isMinifyEnabled = true` and `isShrinkResources = true` in `buildTypes.release`.
   - Update `proguard-rules.pro` with keep rules for ML Kit, Room, and Coil.
2. **Dependencies & Version Catalog**:
   - Add `play-services-mlkit-document-scanner` (16.0.0-beta1)
   - Add `text-recognition` (16.0.1) and `text-recognition-devanagari` (16.0.1)
   - Ensure `coil-compose`, `androidx-navigation-compose`, and `androidx-datastore-preferences` are active.
3. **App Launcher Icon**:
   - Create custom adaptive scanner icon via `generate_image` or clean vector layer list as per `app-icon-generation` skill.
4. **Data Layer & Core Utilities**:
   - `DocumentEntity`, `DocumentDao`, `AppDatabase`, `DocumentRepository`
   - File storage helpers in `filesDir/documents/<id>/`
   - `FileProvider` setup with `res/xml/file_paths.xml`
   - `PreferencesDataStore` for age check and pro state
   - `Constants.kt` for app links and support contacts
5. **Resizer Engine & Unit Tests**:
   - Pure Kotlin `ResizerEngine.kt` implementing the binary-search compression and downscaling loop
   - Local unit test `ResizerEngineTest.kt` verifying size convergence
6. **OCR Engine**:
   - `OcrEngine.kt` supporting Latin & Devanagari text recognition with ML Kit
7. **UI Screens & Navigation**:
   - Theme (`Color.kt`, `Theme.kt`, `Type.kt`)
   - Edge-to-edge layout with M3 TopAppBar & bottom banner placeholder
   - Home Screen with Quick Tools and Recent Documents
   - Scan launcher handling `GmsDocumentScanning` contracts
   - Document Detail viewer with page list and export options
   - OCR Screen with text actions
   - Images to PDF Screen with page size / margin options
   - Photo & Signature Resizer Screen with presets, custom controls, and crop step
   - Settings Screen
   - Age check gate dialog
8. **Localization**:
   - English strings in `res/values/strings.xml`
   - Hindi strings in `res/values-hi/strings.xml`
9. **Documentation**:
   - `README.md` with build steps, architecture overview, and privacy guarantees.
10. **Verification**:
    - Run compilation and tests via `compile_applet`.

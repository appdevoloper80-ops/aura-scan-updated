# AuraScan Google Play Release Checklist

## 1. Prerequisites & Versioning
- **compileSdk**: `36`
- **targetSdk**: `36`
- **minSdk**: `24`
- **versionName**: `1.0.0`
- **versionCode**: Defined in `gradle.properties` (`VERSION_CODE=1`). Increment for each store submission.

## 2. Signing Configuration
- Release signing properties are loaded from `keystore.properties` (git-ignored) or environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
- Template file provided at `keystore.properties.example`.

## 3. Build Command
Execute the following Gradle command to generate the release Android App Bundle (AAB):

```bash
./gradlew bundleRelease
```

## 4. Release Artifact Location
Upon a successful build, the production AAB is created at:

```
app/build/outputs/bundle/release/app-release.aab
```

## 5. Security & Policy Verification
- `android:allowBackup="false"` with `data_extraction_rules.xml` preventing sensitive document backup.
- `network_security_config.xml` restricting cleartext HTTP traffic.
- Adaptive launcher icon with vector foreground, solid background, and Android 13+ themed monochrome layer.
- ProGuard/R8 minification and resource shrinking enabled (`isMinifyEnabled = true`, `isShrinkResources = true`).

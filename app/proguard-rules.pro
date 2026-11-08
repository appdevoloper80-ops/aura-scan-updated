# Project specific ProGuard / R8 rules for AuraScan

# Preserve line numbers and source file attributes for debugging
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature

# Room Database keep rules
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.data.model.** { *; }
-keep class com.example.data.db.** { *; }
-dontwarn androidx.room.paging.**

# ML Kit Document Scanner & Vision keep rules
-keep class com.google.android.gms.internal.mlkit_vision_** { *; }
-keep class com.google.mlkit.vision.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.**

# Coil Image Loader keep rules
-keep class coil.** { *; }
-dontwarn coil.**

# Kotlin Coroutines & Serialization
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class kotlinx.serialization.** { *; }

# Google Mobile Ads & UMP keep rules
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-keep class com.google.ads.mediation.** { *; }
-dontwarn com.google.android.gms.ads.**
-dontwarn com.google.android.ump.**

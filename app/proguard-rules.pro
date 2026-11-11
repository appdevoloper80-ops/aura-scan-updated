# Project specific ProGuard / R8 rules for AuraScan

# Preserve line numbers and source file attributes for stack traces
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep all app package classes, models, and composables
-keep class com.example.** { *; }

# Keep ViewModel constructors for ViewModelProvider reflection
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    public <init>(android.app.Application);
}

# Room Database & generated implementations
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { public <init>(); }
-keep class **.*_Impl { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.Dao *;
    @androidx.room.Entity *;
}
-dontwarn androidx.room.paging.**

# DataStore Preferences
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ML Kit (Document Scanner, Text Recognition, Language ID, Translation)
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class com.google.android.gms.vision.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**

# Kotlin Coroutines & Serialization
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.serialization.** { *; }

# Google Mobile Ads & User Messaging Platform (UMP)
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-keep class com.google.ads.mediation.** { *; }
-dontwarn com.google.android.gms.ads.**
-dontwarn com.google.android.ump.**

# FileProvider
-keep class androidx.core.content.FileProvider { *; }

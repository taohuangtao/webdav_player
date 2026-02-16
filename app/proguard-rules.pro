# WebDAVViewer ProGuard Configuration
# ====================================

# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in Android SDK tools.
# For more details, see
#   https://developer.android.com/build/shrink-code

# ====================================
# Kotlin
# ====================================
# Keep Kotlin Metadata for reflection
-keep class kotlin.Metadata { *; }
-keep class kotlin.** { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ====================================
# Hilt / Dagger
# ====================================
# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep,allowobfuscation,allowshrinking class com.tdull.webdavviewer.app.data.repository.** { *; }
-keep,allowobfuscation,allowshrinking class com.tdull.webdavviewer.app.di.** { *; }

# Hilt generated classes
-keep class **_HiltComponents { *; }
-keep class **_HiltModules { *; }
-keep class **_MembersInjector { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

# ====================================
# OkHttp
# ====================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Okio
-dontwarn okio.**
-keep class okio.** { *; }

# ====================================
# Retrofit (if used)
# ====================================
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ====================================
# Gson / Moshi (if used)
# ====================================
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ====================================
# DataStore
# ====================================
-keep class androidx.datastore.** { *; }
-keep class **$Serializer { *; }

# ====================================
# Coil Image Loading
# ====================================
-keep class coil.** { *; }
-keep interface coil.** { *; }
-keep class * extends coil.ImageLoader { *; }

# ====================================
# ExoPlayer / Media3
# ====================================
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ====================================
# Jetpack Compose
# ====================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }

# ====================================
# Navigation Compose
# ====================================
-keep class androidx.navigation.** { *; }
-keep class * extends androidx.navigation.Navigator { *; }

# ====================================
# Lifecycle / ViewModel
# ====================================
-keep class androidx.lifecycle.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep @androidx.lifecycle.OnLifecycleEvent class * { *; }

# ====================================
# Application Data Models
# ====================================
# Keep all data classes that are serialized
-keep class com.tdull.webdavviewer.app.data.model.** { *; }
-keep class com.tdull.webdavviewer.app.data.remote.** { *; }
-keep class com.tdull.webdavviewer.app.data.local.** { *; }

# ====================================
# WebDAV XML Parsing
# ====================================
-keep class org.xmlpull.v1.** { *; }
-keep interface org.xmlpull.v1.** { *; }

# ====================================
# General Android
# ====================================
# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ====================================
# Build Config
# ====================================
-keep class com.tdull.webdavviewer.app.BuildConfig { *; }

# ====================================
# Optimization
# ====================================
# Enable optimization
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Optimization settings
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# Allow optimization
-allowaccessmodification

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
}

# Keep source file names and line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

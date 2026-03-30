# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.afds.app.**$$serializer { *; }
-keepclassmembers class com.afds.app.** { *** Companion; }
-keepclasseswithmembers class com.afds.app.** { kotlinx.serialization.KSerializer serializer(...); }

# Google Sign-In
-keep class com.google.android.gms.** { *; }

# WebViewAssetLoader (androidx.webkit) — required for local asset serving in release builds
-keep class androidx.webkit.** { *; }
-keep class org.chromium.support_lib_boundary.** { *; }
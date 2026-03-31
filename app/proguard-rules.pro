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

# JavascriptInterface — keep the bridge class name AND its annotated methods.
# -keepclassmembers alone is insufficient: R8 full mode renames the enclosing
# class, so the object passed to addJavascriptInterface() has a different class
# name at runtime than the one WebView's reflection scanner sees.
# -keep prevents both shrinking AND renaming of the class itself.
-keep class com.afds.app.ui.screens.TelegramSetupActivity$* {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# AGP 9.1.0 new default: R8 now applies -repackageclasses to all DEX builds
# by default (moves all classes into the unnamed default package).  This breaks
# any code that looks up a class by its original package-qualified name at
# runtime, including the WebView Java Bridge scanner.  Use -dontrepackage to
# restore the pre-9.1 behaviour and keep package names intact.
-dontrepackage

# Keep the Activity itself so Android's framework can instantiate it via
# Class.forName() when processing the Intent started from ProfileScreen.
# proguard-android-optimize.txt only keeps Activity *members* (XML onClick),
# not the class name — so a subclass in a non-standard sub-package can still
# get renamed/removed.
-keep class com.afds.app.ui.screens.TelegramSetupActivity { <init>(); }

# Full-mode R8 strips InnerClasses + EnclosingMethod attributes unless BOTH
# the inner class and its enclosing class are explicitly kept.  Without these
# attributes the bridge anonymous class loses its Signature, which breaks the
# WebView Java Bridge's reflective method scanner on some WebView versions.
-keepattributes InnerClasses, EnclosingMethod, Signature

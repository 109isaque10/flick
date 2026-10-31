# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# Release keeps (R8 minification + resource shrinking are enabled).
# ---------------------------------------------------------------------------

# Moshi is built with the reflective KotlinJsonAdapterFactory as a last-resort
# fallback (see ApiClient). Reflective adapters read field names and @Json
# annotations at runtime, so R8 must not rename or strip them. Moshi's own
# consumer rules cover the generated (codegen) adapters; these protect the
# reflective path and the DTO field names.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
    @com.squareup.moshi.JsonQualifier <fields>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }

# Model/DTO classes are deserialised by name/field by Moshi.
-keep class com.flick.app.data.model.** { *; }
-keep class com.flick.app.data.remote.** { *; }

# WorkManager instantiates Worker subclasses reflectively by class name.
-keep class com.flick.app.work.** { *; }

# Kotlin metadata is read by the reflective Moshi adapter.
-keep class kotlin.Metadata { *; }

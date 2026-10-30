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

# ---- Student Alarm: keep rules needed once minify/shrink is on ----
# Settings are stored by enum NAME in DataStore/Room (valueOf), so names must not be renamed.
-keepclassmembers enum com.example.** { *; }
# Room entities / DAOs / DataStore models
-keep class com.example.data.** { *; }
# Manifest components (receivers, services, activities) are kept automatically, but keep names for logs
-keepattributes SourceFile,LineNumberTable

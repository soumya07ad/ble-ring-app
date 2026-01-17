# This is a configuration file for ProGuard.
# http://proguard.sourceforge.net/index.html#manual/usage.html
# optimization is turned off with -dontoptimize flag in the script.

# Keep WebView classes
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep your app's classes
-keep class com.fitness.app.** { *; }
-keepnames class com.fitness.app.** { *; }

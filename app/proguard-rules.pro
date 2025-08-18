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

# Keep all classes in your package
#-keep class net.forestany.mediacollection.search.** { *; }

# Preserve class names (optional, useful for reflection)
#-keepnames class net.forestany.mediacollection.search.** { *; }

# Keep class members (fields/methods)
#-keepclassmembers class net.forestany.mediacollection.search.** { *; }
-dontwarn java.sql.JDBCType
-dontwarn org.slf4j.Logger
-dontwarn org.slf4j.LoggerFactory

-keep class net.forestany.** { *; }
-keep class org.sqlite.** { *; }
-keepclassmembers class * {
    native <methods>;
}
-keepnames class * {
    public static <methods>;
}
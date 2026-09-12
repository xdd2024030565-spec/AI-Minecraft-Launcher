# Keep view binding classes
-keep class com.aimc.launcher.databinding.** { *; }

# Keep AI Controller classes
-keep class com.aimc.controller.** { *; }

# Keep FCL classes
-keep class com.tungsten.fcl.** { *; }

# Keep JSON serialization
-keepattributes *Annotation*, InnerClasses
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations

# Gson specific classes
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# OkHttp specific classes
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep Material Design components
-keep class com.google.android.material.** { *; }

# Keep AndroidX classes
-dontwarn androidx.**
-keep class androidx.** { *; }

# Keep serialization
-keep class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * {
    <fields>;
}

# Keep R class
-keep class com.aimc.launcher.R$* { *; }

# Keep application class
-keep class com.aimc.launcher.Application { *; }
# Proguard rules for Lia AI

# Keep org.json
-keep class org.json.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep DataStore Preferences
-keep class androidx.datastore.preferences.** { *; }

# Keep Android WebKit and AssetLoader
-keep class androidx.webkit.** { *; }

# Keep Compose runtime and model classes
-keep class com.Lia.assistant.data.** { *; }
-keep class com.Lia.assistant.voice.** { *; }
-keep class com.Lia.assistant.forge.** { *; }
-keep class com.Lia.assistant.agent.** { *; }
-keep class com.Lia.assistant.license.** { *; }

# Three.js / JS Interface bridges
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

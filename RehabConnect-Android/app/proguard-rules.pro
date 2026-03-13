# Add project specific ProGuard rules here.

# Retrofit
-keepattributes Signature
-keepattributes Annotation
-keep class com.srcardiocare.data.model.** { *; }
-keep class com.srcardiocare.data.api.** { *; }
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

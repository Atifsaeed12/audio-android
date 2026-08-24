# Retrofit / Gson models are accessed reflectively — keep them.
-keep class com.rulecough.app.net.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**

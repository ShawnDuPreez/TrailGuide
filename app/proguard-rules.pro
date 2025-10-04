# Add project specific ProGuard rules here.
# Keep Retrofit and Gson classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.trailguide.android.data.model.** { *; }
-keep class com.trailguide.android.data.dto.** { *; }

# Retrofit
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Firebase classes
-keep class com.google.firebase.** { *; }


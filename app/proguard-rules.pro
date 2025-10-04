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

# SLF4J (used by Ktor/Supabase) - Fix for R8 error
-dontwarn org.slf4j.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
-keep class org.slf4j.** { *; }

# Ktor Client (used by Supabase)
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-dontwarn io.ktor.**

# Supabase
-keep class io.github.jan.supabase.** { *; }
-keepclassmembers class io.github.jan.supabase.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.trailguide.android.**$$serializer { *; }
-keepclassmembers class com.trailguide.android.** {
    *** Companion;
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}


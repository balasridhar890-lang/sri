# Android ProGuard rules

# Keep AndroidX classes
-keep class androidx.** { *; }
-dontwarn androidx.**

# Keep Kotlin specific classes
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# Keep Hilt generated classes
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.**
-dontwarn javax.inject.**

# Keep Retrofit and OkHttp classes
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# Keep Serialization classes
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Keep Room database classes
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Keep application classes
-keep class com.voiceassistant.android.** { *; }

# Keep Parcelable and Serializable classes
-keepclassmembers class * implements android.os.Parcelable {
    static android.os.Parcelable$Creator CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
}

# Keep data classes
-keepclassmembers class com.voiceassistant.android.** {
    *** get*(...);
    *** set*(...);
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# General optimization settings
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose
-dontshrink

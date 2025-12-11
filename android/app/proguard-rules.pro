# Proguard rules for keeping model classes
-keep class com.assistant.voicecore.model.** { *; }

# Keep API service interfaces and implementations
-keep interface com.assistant.voicecore.repository.ApiService { *; }
-keep class com.assistant.voicecore.repository.** { *; }

# Keep ViewModels
-keep class com.assistant.voicecore.viewmodel.** { *; }

# Keep Services
-keep class com.assistant.voicecore.service.** { *; }

# Keep Application class
-keep class com.assistant.voicecore.VoiceCoreApplication { *; }

# Keep UI components
-keep class com.assistant.voicecore.ui.** { *; }

# Keep receivers
-keep class com.assistant.voicecore.receiver.** { *; }

# Network related
-keep class com.assistant.voicecore.network.** { *; }
-keep class com.assistant.voicecore.di.** { *; }

# Dagger/Hilt specific rules
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class **_HiltModules*
-keep class **_Provide*
-keep class **_Factory*

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Moshi
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }

# Speech Recognition
-dontwarn android.speech.**

# Media/Audio
-dontwarn android.media.**

# AudioManager
-dontwarn android.media.AudioManager

# Telecom
-dontwarn android.telecom.**
-dontwarn android.telephony.**

# Timber
-dontwarn timber.log.**

# Kotlin
-keep class kotlin.** { *; }
-dontwarn kotlin.**

# Coroutines
-dontwarn kotlinx.coroutines.flow.**

# Google Play Services
-dontwarn com.google.android.gms.**

# Android specific
-dontwarn android.app.**
-dontwarn android.os.**
-dontwarn android.content.**
-dontwarn android.view.**
-dontwarn android.widget.**
-dontwarn android.database.**

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
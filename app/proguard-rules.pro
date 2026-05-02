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

# ==============================================================================
# PROGUARD / R8 KEEP RULES
# ==============================================================================
# Covered Libraries and Classes:
# - Retrofit + Moshi/Gson (Networking & Serialization)
# - Room (Entities and DAOs)
# - Hilt (Generated Components)
# - MPAndroidChart (Charting Library)
# - Firebase / Crashlytics (Analytics & Crash Reporting)
# - Project Core Classes (DataGeneratorClass, SowingIndexCalculator)
# - Response DTOs (WeatherResponseDto, ForecastDto, etc.)
# ==============================================================================

# Retrofit
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Moshi / Gson (for DTOs)
-keep class com.squareup.moshi.** { *; }
-keep class com.google.gson.** { *; }

# Network DTOs
-keep class com.raithabharosahub.data.remote.dto.** { *; }

# Room
-keep class androidx.room.** { *; }
-keep class com.raithabharosahub.data.local.entity.** { *; }
-keep class com.raithabharosahub.data.local.dao.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Firebase & Crashlytics
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Project Core Logic
-keep class com.raithabharosahub.data.generator.DataGeneratorClass { *; }
-keep class com.raithabharosahub.domain.calculator.SowingIndexCalculator { *; }
# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Google Play Billing Library & AIDL rules
-keep class com.android.vending.billing.** { *; }
-keep class com.android.billingclient.api.** { *; }
-keep interface com.android.billingclient.api.** { *; }

# Keep data models for Room & Firestore serialization
-keep class com.example.data.model.** { *; }
-keep class com.example.billing.** { *; }

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-dontwarn com.android.billingclient.**

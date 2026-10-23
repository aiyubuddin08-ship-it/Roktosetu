# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep line number information for debugging crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep generic signatures, annotations, and Kotlin Metadata for reflection/Firestore/Room
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
-keep class kotlin.Metadata { *; }

# Keep all application domain/Firestore/UI model classes and enums
-keep class com.example.model.** { *; }
-keepclassmembers class com.example.model.** {
    *;
}
-keepnames class com.example.model.** { *; }

# Keep all Enum values and valueOf methods
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Room Database Entities, DAOs, and Database classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class com.example.data.local.** { *; }
-dontwarn androidx.room.paging.**

# Firebase Firestore & Google Play Services rules
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

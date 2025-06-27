# ----------------------------------------
# Firebase
# ----------------------------------------

-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-dontwarn com.google.firebase.auth.**

# Firebase Firestore
-keep class com.google.firebase.firestore.** { *; }
-dontwarn com.google.firebase.firestore.**

# Firebase Storage
-keep class com.google.firebase.storage.** { *; }
-dontwarn com.google.firebase.storage.**

# Firebase AppCheck
-keep class com.google.firebase.appcheck.** { *; }
-dontwarn com.google.firebase.appcheck.**

# Google Tasks (required by Firebase)
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn com.google.android.gms.tasks.**

# ----------------------------------------
# Google Sign-In & Identity
# ----------------------------------------

-keep class com.google.android.gms.auth.api.** { *; }
-dontwarn com.google.android.gms.auth.api.**

-keep class com.google.android.gms.common.api.** { *; }
-dontwarn com.google.android.gms.common.api.**

-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**

# ----------------------------------------
# Hilt (Dependency Injection)
# ----------------------------------------

-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

-keep class javax.inject.** { *; }
-dontwarn javax.inject.**

# Keep generated Hilt components and modules
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }

# ----------------------------------------
# Room
# ----------------------------------------

-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Keep entities & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Room
-keep class * implements androidx.room.Dao
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# ----------------------------------------
# Jetpack WorkManager
# ----------------------------------------

-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# ----------------------------------------
# Jetpack Compose
# ----------------------------------------

-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Compose tooling preview
-keep class androidx.compose.ui.tooling.** { *; }

# ----------------------------------------
# Kotlin Serialization
# ----------------------------------------

-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# ----------------------------------------
# Coil (Image loading)
# ----------------------------------------

-keep class coil.** { *; }
-dontwarn coil.**

# ----------------------------------------
# Your Custom Model/Data Classes (Very Important!)
# ----------------------------------------

-keep class com.innovatewithomer.myshelf.data.model.** { *; }
-dontwarn com.innovatewithomer.myshelf.data.model.**

-keep class com.innovatewithomer.myshelf.data.local.entity.** { *; }
-dontwarn com.innovatewithomer.myshelf.data.local.entity.**

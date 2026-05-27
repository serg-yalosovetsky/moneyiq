# MoneyIQ ProGuard rules

# Keep Room entities, DAOs, and converters
-keep class org.pixelrush.moneyiq.data.db.** { *; }

# Keep Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }

# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep data classes used in UI state (needed for correct toString/equals/copy)
-keep class org.pixelrush.moneyiq.ui.**.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Kotlin serialization / coroutines
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keep class kotlin.coroutines.** { *; }

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

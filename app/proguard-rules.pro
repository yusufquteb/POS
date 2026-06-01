# ════════════════════════════════════════════════════════════════
# ProGuard Rules - SmartPOS v1.1
# ════════════════════════════════════════════════════════════════

# Keep POS System classes
-keep class com.pos.system.** { *; }

# Keep Material Design
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# Keep AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep ZXing (Barcode)
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Keep ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep Apache POI (Excel)
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.commons.**
-dontwarn org.openxmlformats.**

# Keep Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { *; }

# Keep Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Google Play Billing
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# Keep Google Play Review & Update
-keep class com.google.android.play.** { *; }
-dontwarn com.google.android.play.**

# Keep Bluetooth Printer
-keep class com.dantsu.escposprinter.** { *; }
-dontwarn com.dantsu.escposprinter.**

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelables
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ════════════════════════════════════════════════════════════════
# Google Play – معايير إضافية
# ════════════════════════════════════════════════════════════════

# Keep CartItem fields for reflection in DBHelper.createInvoiceWithDetails
-keepclassmembers class com.pos.system.ActivityCartActivity$CartItem {
    *;
}

# Keep MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# Keep Thermal Printer classes
-keep class com.dantsu.escposprinter.** { *; }
-dontwarn com.dantsu.escposprinter.**

# Google Play Review
-keep class com.google.android.play.core.review.** { *; }
-dontwarn com.google.android.play.core.review.**

# Google Play App Update
-keep class com.google.android.play.core.appupdate.** { *; }
-dontwarn com.google.android.play.core.appupdate.**

# Google Play GMS Tasks
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn com.google.android.gms.tasks.**

# Billing Client
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# ✅ معيار Google Play: الاحتفاظ بـ Parcelable وSerializable
-keep class * implements java.io.Serializable { *; }
-keepnames class * implements java.io.Serializable

# ✅ منع إزالة نقاط دخول BuildConfig
-keep class com.pos.system.BuildConfig { *; }

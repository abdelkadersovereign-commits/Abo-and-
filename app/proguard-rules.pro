# ============================================
# ProGuard Rules for A.SYRIA Security App
# ============================================

# تفعيل التحسينات الأساسية
-optimizationpasses 5
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-verbose

# ============================================
# 1. Android Framework & System Classes
# ============================================

# احفظ أسماء الأنشطة والخدمات (للأنشطة المعرّفة في Manifest)
-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

# احفظ جميع أنواع View الافتراضية
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# ============================================
# 2. Jetpack Compose
# ============================================

-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.animation.** { *; }

# احفظ Composable functions
-keepclasseswithmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ============================================
# 3. Firebase & Google Services
# ============================================

-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keepnames class com.google.firebase.** { *; }
-keepnames class com.google.android.gms.** { *; }

# احفظ Firebase Analytics
-keep class com.google.firebase.analytics.** { *; }
-keepclassmembers class com.google.firebase.analytics.** {
    public <init>(...);
    public <methods>;
}

# احفظ Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-keep interface com.google.firebase.auth.** { *; }

# ============================================
# 4. Google Play Services
# ============================================

-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.location.** { *; }

# ============================================
# 5. Room Database
# ============================================

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers @androidx.room.** class * {
    @androidx.room.* <methods>;
}

# احفظ أسماء الأعمدة والجداول
-keepattributes *Annotation*
-keep class * {
    @androidx.room.* <fields>;
}

# ============================================
# 6. Retrofit & OkHttp
# ============================================

-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keep interface retrofit2.** { *; }

-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepclassmembers class okhttp3.** {
    public <methods>;
}

# ============================================
# 7. Gson (JSON Serialization)
# ============================================

-keep class com.google.gson.** { *; }
-keepclassmembers enum * {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# نماذج البيانات (Data Classes)
-keep class com.asyria.security.data.** { *; }
-keepclassmembers class com.asyria.security.data.** {
    <init>(...);
    <fields>;
    <methods>;
}

# ============================================
# 8. Kotlin
# ============================================

-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# احفظ Data Classes
-keepclasseswithmembers class * {
    @kotlin.jvm.JvmField <fields>;
}

# ============================================
# 9. Coil (Image Loading)
# ============================================

-keep class coil.** { *; }
-keep class io.coil_kt.** { *; }
-keepclassmembers class io.coil_kt.** {
    <init>(...);
}

# ============================================
# 10. ML Kit
# ============================================

-keep class com.google.mlkit.** { *; }
-keepclassmembers class com.google.mlkit.** {
    public <methods>;
}

# ============================================
# 11. CameraX
# ============================================

-keep class androidx.camera.** { *; }
-keepclassmembers class androidx.camera.** {
    public <methods>;
}

# ============================================
# 12. Google Generative AI (Gemini)
# ============================================

-keep class com.google.ai.** { *; }
-keep class com.google.generativeai.** { *; }
-keepclassmembers class com.google.ai.** {
    <init>(...);
    public <methods>;
}

# ============================================
# 13. BiometricPrompt
# ============================================

-keep class androidx.biometric.** { *; }
-keepclassmembers class androidx.biometric.** {
    public <methods>;
}

# ============================================
# 14. DataStore
# ============================================

-keep class androidx.datastore.** { *; }
-keepclassmembers class androidx.datastore.** {
    public <methods>;
}

# ============================================
# 15. Navigation
# ============================================

-keep class androidx.navigation.** { *; }
-keepclassmembers class androidx.navigation.** {
    public <methods>;
}

# ============================================
# 16. تطبيقك الخاص
# ============================================

# احفظ جميع الفئات من تطبيقك
-keep class com.asyria.security.** { *; }

# احفظ Activities
-keep public class com.asyria.security.** extends android.app.Activity { *; }
-keep public class com.asyria.security.** extends android.app.Service { *; }

# احفظ View Models
-keep class androidx.lifecycle.ViewModel { *; }
-keepclassmembers class com.asyria.security.** extends androidx.lifecycle.ViewModel {
    <init>();
    <init>(...);
}

# احفظ Composables
-keepclasseswithmembers class com.asyria.security.ui.** {
    @androidx.compose.runtime.Composable <methods>;
}

# ============================================
# 17. عام - لا تزل الأسماء المهمة
# ============================================

# احفظ جميع Inner Classes
-keepnestmembers class * {
    *;
}

# احفظ Source File و Line Numbers للـ Crash Reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# احفظ Exception Names
-keepattributes Exceptions

# احفظ الـ Annotations
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations

# ============================================
# 18. عدم الضياع (Don'ts)
# ============================================

# لا تحذف Enum classes
-dontnote enum **
-dontnote androidx.**

# تخطي التحذيرات
-dontwarn kotlin.**
-dontwarn androidx.**
-dontwarn com.google.**
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn com.squareup.**

# ============================================
# 19. تحسينات الأداء
# ============================================

# فك تشفير النصوص
-repackageclasses ''
-allowaccessmodification

# ============================================
# نصيحة: اختبر APK بدون ProGuard أولاً
# ============================================
# إذا كان لديك مشاكل، قم بتعديل القواعد أعلاه
# أو أضف classes إضافية إلى -keep directives

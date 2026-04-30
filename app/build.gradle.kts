import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

val keystorePropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (keystorePropertiesFile.exists()) {
    localProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.asyria.security"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.asyria.security"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    composeOptions {
        // تم تحديث هذه النسخة لتتوافق مع Kotlin 1.9.x ولتجنب أخطاء البناء
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    kapt {
        correctErrorTypes = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // إضافة استثناءات لمنع تعارض الملفات أثناء تجميع الـ APK
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
        }
    }
}

dependencies {
    implementation("androidx.work:work-runtime:2.9.0") 
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    
    // استخدام BOM الموحد لضمان توافق جميع نسخ Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // مكتبات أساسية تم التأكد من وجودها لإصلاح أخطاء الصور السابقة
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")


    // Coil لتحميل الصور
    implementation("io.coil-kt:coil-compose:2.6.0")
    
    // Gemini SDK للذكاء الاصطناعي
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.1.1")
    implementation("com.google.android.gms:play-services-location:21.2.0")


    // Navigation Compose للتنقل بين الشاشات
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // DataStore لتخزين الإعدادات (مثل رمز PIN)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // ML Kit لفحص الـ QR والروابط
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // CameraX لالتقاط صور الفحص
    val camerax_version = "1.3.2"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")

    // Retrofit لفحص الروابط والشبكة عبر الانترنت
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Room Database لتخزين الأدعية ومواعيد الصلاة محلياً
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")

    // Security Crypto لتشفير بيانات المستخدم الحساسة
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // الاختبارات
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

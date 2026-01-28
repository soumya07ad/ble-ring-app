plugins {
    id("com.android.application") version "8.5.0"
    kotlin("android") version "1.9.22"
}

android {
    namespace = "com.fitness.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fitness.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
    implementation("androidx.activity:activity-compose:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    
    // WebView
    implementation("androidx.webkit:webkit:1.8.0")
    
    // Retrofit + Networking
    implementation("com.squareup.retrofit2:retrofit:2.10.0")
    implementation("com.squareup.retrofit2:converter-gson:2.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // DataStore for secure token storage
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // WorkManager for background sync
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    
    // ══════════════════════════════════════════════════════════
    // YCBT BLE SDK + JieLi Libraries (from YCBleSdkDemo)
    // ══════════════════════════════════════════════════════════
    implementation(files("libs/ycbtsdk-release.aar"))
    implementation(files("libs/JL_Watch_V1.10.0-release.aar"))
    implementation(files("libs/jl_bt_ota_V1.9.3-release.aar"))
    implementation(files("libs/jl_rcsp_V0.5.2-release.aar"))
    implementation(files("libs/AliAgent-release-4.1.3.aar"))
    implementation(files("libs/BmpConvert_V1.2.1-release.aar"))
}

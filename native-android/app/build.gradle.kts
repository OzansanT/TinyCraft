plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.tinycraft.nativeandroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tinycraft.nativeandroid.fullscreen"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "0.3.0-gpu-chunks"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

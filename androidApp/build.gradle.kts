plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.immich.server.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.immich.server.android"
        minSdk = 16
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
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

    flavorDimensions = "ui"

    productFlavors {
        // Modern: Compose UI, minSdk 21 (Android 5.0+)
        create("modern") {
            minSdk = 21
            versionNameSuffix = "-modern"
            isDefault = true
        }
        // Legacy: XML UI, minSdk 16 (Android 4.1+)
        create("legacy") {
            minSdk = 16
            versionNameSuffix = "-legacy"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.kotlinx.coroutines.core)

    // Modern flavor: Compose UI
    "modernImplementation"(libs.androidx.activity.compose)
    "modernImplementation"(platform(libs.androidx.compose.bom))
    "modernImplementation"(libs.androidx.compose.ui)
    "modernImplementation"(libs.androidx.compose.material3)

    // Legacy flavor: AppCompat for XML UI
    "legacyImplementation"("androidx.appcompat:appcompat:1.6.1")
    "legacyImplementation"("com.google.android.material:material:1.11.0")
    "legacyImplementation"("androidx.constraintlayout:constraintlayout:2.1.4")
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// 从 CI 参数读取版本号，默认使用开发版本
val versionName: String = project.findProperty("versionName") as String? ?: "0.1.0-dev"
val versionCode: Int = (project.findProperty("versionCode") as String? ?: "1").toInt()

android {
    namespace = "com.immich.server.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.immich.server.android"
        minSdk = 21
        targetSdk = 34
        this.versionCode = versionCode
        this.versionName = versionName
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

    flavorDimensions += "ui"

    productFlavors {
        // Modern: Compose UI, minSdk 21 (Android 5.0+)
        create("modern") {
            minSdk = 21
            isDefault = true
        }
        // Legacy: XML UI, minSdk 21 (Android 5.0+)
        create("legacy") {
            minSdk = 21
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
        buildConfig = true
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

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// 从 CI 参数读取版本号，默认使用开发版本
val buildVersionName: String = project.findProperty("versionName") as String? ?: "0.1.0-dev"
val buildVersionCode: Int = (project.findProperty("versionCode") as String? ?: "1").toInt()

println("[BUILD] versionName=$buildVersionName versionCode=$buildVersionCode")

android {
    namespace = "com.immich.server.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.immich.server.android"
        minSdk = 21
        targetSdk = 34
        versionCode = buildVersionCode
        versionName = buildVersionName
    }

    // 签名配置
    signingConfigs {
        // 使用项目中的固定 debug keystore（确保所有构建使用相同签名）
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        
        // Release 签名配置（从环境变量读取）
        create("release") {
            // 从环境变量或 gradle.properties 读取签名信息
            val keystoreFile = project.findProperty("KEYSTORE_FILE") as String? 
                ?: System.getenv("KEYSTORE_FILE")
            val keystorePassword = project.findProperty("KEYSTORE_PASSWORD") as String? 
                ?: System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = project.findProperty("KEY_ALIAS") as String? 
                ?: System.getenv("KEY_ALIAS")
            val keyPassword = project.findProperty("KEY_PASSWORD") as String? 
                ?: System.getenv("KEY_PASSWORD")
            
            if (keystoreFile != null && keystorePassword != null) {
                storeFile = file(keystoreFile)
                storePassword = keystorePassword
                keyAlias = keyAlias ?: "immich-server"
                keyPassword = keyPassword ?: keystorePassword
            } else {
                // 如果没有配置 release keystore，使用固定的 debug 签名
                // 这样可以确保 APK 可以覆盖安装
                storeFile = file("debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
        }
        release {
            // 如果有 release keystore 配置，使用 release 签名
            // 否则使用 debug 签名（确保可以覆盖安装）
            signingConfig = signingConfigs.getByName("release")
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

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            println("[BUILD] variant=${variant.name} versionName=${variant.versionName} versionCode=${variant.versionCode} outputFileName=${output.outputFileName}")
        }
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

    // Legacy flavor: XML UI
    "legacyImplementation"("androidx.appcompat:appcompat:1.6.1")
    "legacyImplementation"("com.google.android.material:material:1.11.0")
    "legacyImplementation"("androidx.constraintlayout:constraintlayout:2.1.4")
}
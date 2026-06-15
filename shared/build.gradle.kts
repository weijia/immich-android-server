plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    // Android target
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // JVM target (for desktop/minimal)
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kotlin
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                // Ktor Server
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
                implementation(libs.ktor.server.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.server.auth)
                implementation(libs.ktor.server.auth.jwt)
                implementation(libs.ktor.server.status.pages)
                implementation(libs.ktor.server.cors)
                implementation(libs.ktor.network)

                // SQLDelight
                implementation(libs.sqldelight.runtime)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.sqldelight.android.driver)
                implementation(libs.androidx.core.ktx)
                implementation("androidx.appcompat:appcompat:1.6.1")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}

android {
    namespace = "com.immich.server.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 16
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("ImmichDatabase") {
            packageName.set("com.immich.server.db")
        }
    }
}

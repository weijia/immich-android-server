plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.sqldelight) apply false
}

subprojects {
    configurations.all {
        resolutionStrategy {
            // Force guava-android to avoid conflict between guava-jre (from ktor/jwks-rsa)
            // and listenablefuture:1.0 (from androidx)
            force("com.google.guava:guava:32.1.1-android")
            // Exclude guava-jre from transitive dependencies
            exclude(group = "com.google.guava", module = "guava-jre")
        }
    }
}

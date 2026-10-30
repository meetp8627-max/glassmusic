plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.styropyr0.prismal"
    compileSdk = 37

    defaultConfig {
        minSdk = 25
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

dependencies {
    api(platform("androidx.compose:compose-bom:2026.06.01"))
    api("androidx.compose.foundation:foundation")
    api("androidx.compose.ui:ui")
    api("androidx.compose.ui:ui-graphics")
    api("androidx.compose.runtime:runtime")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.startup:startup-runtime:1.2.0")
}

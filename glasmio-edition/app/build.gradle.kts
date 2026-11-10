plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystorePath: String? = System.getenv("KEYSTORE_FILE")

android {
    namespace = "com.meet.glassmusic.glasmio"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.meet.glassmusic.glasmio"
        minSdk = 33
        targetSdk = 37
        versionCode = (project.findProperty("appVersionCode") as String?)?.toInt() ?: 3
        versionName = (project.findProperty("appVersionName") as String?) ?: "2.1.0"
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            signingConfig = signingConfigs.getByName(if (keystorePath != null) "release" else "debug")
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation("io.github.neoruaa.glasmio:glasmio-scaffold:0.1.1")
    implementation("io.github.neoruaa.glasmio:glasmio-preference:0.1.1")
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-session:1.3.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
}

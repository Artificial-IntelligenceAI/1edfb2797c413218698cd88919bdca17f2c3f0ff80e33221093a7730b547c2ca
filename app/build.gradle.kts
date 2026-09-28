import java.net.URI
import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Nothing's Glyph SDK ships without a license, so it isn't committed to this repo.
// It is fetched from Nothing's official repo (pinned commit) and checked against a known hash.
val glyphSdkUrl =
    "https://raw.githubusercontent.com/Nothing-Developer-Programme/Glyph-Developer-Kit/" +
        "8ee807a9312a640b0d43051450924e3446bc1d78/sdk/glyph-matrix-sdk-2.0.aar"
val glyphSdkSha256 = "329393019db5f0f987c6245855d13fa273d06756c68829ca0f6ae686ba336da1"
val glyphSdk = file("libs/glyph-matrix-sdk-2.0.aar")

fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

if (!glyphSdk.exists()) {
    logger.lifecycle("Downloading Nothing Glyph SDK…")
    val bytes = URI(glyphSdkUrl).toURL().openStream().use { it.readBytes() }
    check(sha256(bytes) == glyphSdkSha256) { "Glyph SDK hash mismatch; refusing to use it." }
    glyphSdk.parentFile.mkdirs()
    glyphSdk.writeBytes(bytes)
}

android {
    namespace = "app.linglongdingdong"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.linglongdingdong"
        // The Glyph SDK requires API 33.
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sideloaded app: sign release builds with the debug key so the APK installs as-is.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(files(glyphSdk))

    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}

import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Apple Music developer token (a JWT made by tools/make_apple_token.py), read from the
// git-ignored apple-music.properties; without it the token can be pasted in the app.
val appleProps = Properties().apply {
    rootProject.file("apple-music.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
val appleDeveloperToken: String = appleProps.getProperty("apple.developerToken", "").trim()
// Foldy Streaman: the Client ID of the owner's Spotify app. A public ID by design; the app has no secret.
val spotifyClientId = "71ead01495b3418192d9a18c38d044ab"

// Release signing, from the git-ignored signing.properties (storeFile, storePassword, keyAlias,
// keyPassword). Without it, release builds fall back to the debug key.
val signingProps = Properties().apply {
    rootProject.file("signing.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

// The app version; versionCode must go up by one with every release.
val appVersionName = "0.38.0"
val appVersionCode = 65

base {
    archivesName = "FoldyStreaman-$appVersionName"
}

android {
    namespace = "dev.foldman"
    compileSdk = 37

    defaultConfig {
        // Foldy Streaman: its own ID so it installs beside the real Foldman.
        applicationId = "dev.foldman.streaman"
        minSdk = 30
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        buildConfigField("String", "APPLE_DEVELOPER_TOKEN", "\"$appleDeveloperToken\"")
        // Foldy Streaman: the Spotify app's Client ID for "Log in with Spotify" (an ID, not a secret).
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$spotifyClientId\"")
        // Every current foldable is 64-bit ARM; leaving out the rest halves the APK.
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        if (signingProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        } else if (System.getenv("FOLDY_KEYSTORE") != null) {
            // Foldy Streaman: GitHub builds sign with Foldy's own key, kept in the repo's Actions
            // secret FOLDY_KEYSTORE_B64, so every new APK installs as an update over the last one.
            // The key file is the secret; its password is Android's standard one for this kind of key.
            create("release") {
                storeFile = file(System.getenv("FOLDY_KEYSTORE"))
                storePassword = System.getenv("FOLDY_KEY_PASSWORD") ?: "android"
                keyAlias = System.getenv("FOLDY_KEY_ALIAS") ?: "androiddebugkey"
                keyPassword = System.getenv("FOLDY_KEY_PASSWORD") ?: "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperty("roborazzi.test.record", "true")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    // Screen previews drawn on a computer (Robolectric + Roborazzi): ./gradlew :app:testDebugUnitTest
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.18")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.79.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.79.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")

    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("androidx.window:window:1.5.1")
    implementation("androidx.palette:palette-ktx:1.0.0")
    // Foldy Streaman: "Log in with Google" for listing YouTube Music playlists.
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // Apple's MusicKit for Android AARs (musickitauth-release-*.aar, mediaplayback-release-*.aar),
    // downloaded from developer.apple.com. Optional: the app finds them at runtime.
    implementation(fileTree("libs") { include("*.aar", "*.jar") })
    // MusicKit's sign-in screen uses an AppCompat theme.
    implementation("androidx.appcompat:appcompat:1.8.0")
}

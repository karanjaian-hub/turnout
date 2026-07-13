import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

fun envUrl(key: String, fallback: String): String =
    localProperties.getProperty(key) ?: fallback

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.appdistribution)
}

android {
    namespace = "com.turnout.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.turnout.android"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "API_BASE_URL", "\"${envUrl("API_BASE_URL", "http://10.0.2.2:8080/")}\"")
        buildConfigField("String", "WS_BASE_URL", "\"${envUrl("WS_BASE_URL", "ws://10.0.2.2:8080/ws")}\"")
        buildConfigField("String", "RSVP_BASE_URL", "\"${envUrl("RSVP_BASE_URL", "http://10.0.2.2:3000/")}\"")
        buildConfigField("Boolean", "ENABLE_HTTP_LOGGING", "true")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        manifestPlaceholders["rsvpHost"] = envUrl("RSVP_HOST", "your-turnout-frontend.vercel.app")
    }

    signingConfigs {
        // Debug uses Android's auto-generated debug key — no config needed, it's automatic.
        create("release") {
            storeFile = rootProject.file(envUrl("STORE_FILE", "turnout-release.keystore"))
            storePassword = envUrl("STORE_PASSWORD", "")
            keyAlias = envUrl("KEY_ALIAS", "")
            keyPassword = envUrl("KEY_PASSWORD", "")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            // Lets debug and release builds live side-by-side on the same device
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            // HTTP logging is OFF in release — never leak tokens to Logcat in production
            buildConfigField("Boolean", "ENABLE_HTTP_LOGGING", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    firebaseAppDistribution {
        // Group must match the tester group name created in Firebase Console -> App Distribution.
        groups = "developers"
        artifactType = "APK"
        releaseNotesFile = "release_notes.txt"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Compose BOM pins all compose/* versions — never version them individually
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.animation)
    implementation(libs.compose.material.m2)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)

    implementation(libs.krossbow.stomp.core)
    implementation(libs.krossbow.websocket.okhttp)

    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    implementation(libs.coil.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    implementation(libs.biometric)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.work.runtime)
    implementation(libs.core.splashscreen)
    implementation(libs.material.components)
    implementation(libs.window.sizeclass)
    implementation(libs.browser)
    implementation(libs.accompanist.permissions)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test.junit4)
}

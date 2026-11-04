plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.hermestower.voiceconsole"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hermestower.voiceconsole"
        minSdk = 30
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0-debug"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets {
        // GRAPH 4 NODE 1: expose the shipped asset catalog to JVM unit tests so
        // CatalogIntegrityTest can audit the REAL production manifest (not a copy).
        getByName("test") {
            resources.srcDir("src/main/assets")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        // GRAPH 4 NODE 0 (CP-G4-002): lint truth is a build gate. Errors fail the
        // build (Lint gate: 0 errors at ca8980b→CP-G4-002; any NEW error must fail
        // the workflow). Warnings stay non-fatal and are classified individually in
        // docs/lint/LINT_WARNING_CLASSIFICATION.md — no blanket suppression.
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    // CP-G4-008: real ONNX runtime for the verified voice assets (GTCRN denoiser,
    // openWakeWord melspec/embed/head chain). Version pinned in DEPENDENCY_PINNING.md.
    implementation(libs.onnxruntime.android)
    // GRAPH 4 FINAL §14: REAL offline Italian STT runtime for the verified
    // vosk-model-small-it-0.22 delivery (§12 catalog, Maven Central, Apache-2.0).
    implementation(libs.vosk.android)
    // CP-G4-010 wave 2: in-app QR pairing scanner — CameraX capture pipeline +
    // ML Kit bundled on-device barcode scanning (no Play Services dependency).
    // Versions pinned in DEPENDENCY_PINNING.md with license verification.
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.compose.ui.tooling)
}

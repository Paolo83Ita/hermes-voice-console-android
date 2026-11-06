import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
        // OWNER PHYSICAL RETEST FAILURE (CP-G4-029): the repair build version
        // MUST exceed the shipped 0.3.0-alpha-graph4 / versionCode 3. Never lower.
        versionCode = 4
        versionName = "0.3.1-alpha-graph4-owner-repair"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // OWNER PHYSICAL RETEST §1 (build provenance): authoritative build
        // identity is INJECTED AT BUILD TIME — no value identifying the build
        // may be generated at runtime. CI (android-debug-build.yml) supplies
        // GITHUB_SHA / GITHUB_RUN_ID; local builds stamp honestly as local.
        // The installed-APK SHA-256 and the APK variant are NOT buildConfig
        // values (a file cannot hash itself at build time) — they are MEASURED
        // on device from the real installed file (BuildIdentity.kt).
        val ciSha: String = System.getenv("GITHUB_SHA") ?: ""
        val ciRun: String = System.getenv("GITHUB_RUN_ID") ?: ""
        val ciRef: String = System.getenv("GITHUB_REF_NAME") ?: ""
        val buildStamp: String = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())
        buildConfigField("String", "GIT_SHA_FULL", "\"$ciSha\"")
        buildConfigField("String", "GIT_SHA_SHORT", "\"${ciSha.take(7)}\"")
        buildConfigField("String", "BUILD_TIMESTAMP_ISO8601", "\"$buildStamp\"")
        buildConfigField("String", "BUILD_WORKFLOW_RUN_ID", "\"$ciRun\"")
        buildConfigField("String", "BUILD_WORKFLOW_REF", "\"$ciRef\"")
        buildConfigField("String", "BUILD_LABEL", "\"${project.findProperty("hermes.buildLabel") ?: "POCO OWNER REPAIR 1"}\"")
    }

    // GRAPH 4 FINAL §20: DUAL APK OUTPUT — the UNIVERSAL debug APK keeps every
    // ABI (CI + x86_64 emulator), the per-ABI split APKs serve the physical
    // Xiaomi (arm64-v8a). universalApk keeps the universal artifact alongside
    // the splits; per-ABI versionCode offsets stay off (debug-only qualification).
    splits {
        abi {
            isEnable = true
            isUniversalApk = true
            reset()
            include("arm64-v8a", "x86_64")
        }
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
    // R-30 (run #145 lane 1): the scanner back path is the SYSTEM back button —
    // device-level injection (UiAutomator) needs no window focus, unlike
    // Espresso.pressBack (RootViewWithoutFocusException on the CameraX screen).
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.compose.ui.tooling)
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.bugzz.filter.camera"
    compileSdk = 36  // Bumped 35 -> 36 in Phase 02-02: CameraX 1.6.0 requires compileSdk >= 36.
                     // targetSdk stays 35 (locked by CLAUDE.md to match reference app 1:1).
                     // android.suppressUnsupportedCompileSdk=36 in gradle.properties is pre-armed from Phase 1.

    defaultConfig {
        applicationId = "com.bugzz.filter.camera"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            // StrictMode + LeakCanary: no special AGP config needed — they enable via BuildConfig.DEBUG + debugImplementation
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            // Rule 3 auto-fix (Plan 02-04 Task 3): CameraControllerTest constructs Preview.Builder()
            // via CameraController.bind(); Preview.Builder internally calls android.util.ArrayMap.put,
            // which throws "not mocked" on the JVM stub android.jar. Returning default values lets
            // stub framework calls no-op (null/0/false) so Mockito-only testing works without
            // pulling in Robolectric. Safe because CameraControllerTest only inspects the UseCaseGroup
            // composition, not framework interactions.
            isReturnDefaultValues = true
            // Rule 2 auto-fix (Plan 03-03 Task 2): AssetLoaderTest.preload_* tests call
            // AssetLoader.preload() which reads from assets/ via AssetManager. Without
            // includeAndroidResources=true, Robolectric ShadowArscAssetManager cannot serve
            // assets from src/main/assets/ in unit tests — FileNotFoundException on open().
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Debug tooling
    debugImplementation(libs.leakcanary.android)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // CameraX 1.6.0 (Phase 2) — uniform family
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.effects)
    implementation(libs.androidx.camera.mlkit.vision)
    implementation(libs.androidx.camera.compose)

    // ML Kit Face Detection (bundled ~3-4MB model; no Play Services download race)
    implementation(libs.mlkit.face.detection)

    // Logging (debug tree planted in BugzzApplication when BuildConfig.DEBUG)
    implementation(libs.timber)

    // Guava — CameraX ProcessCameraProvider.getInstance() returns ListenableFuture<T>; we need
    // the type on the compile classpath for our await() bridge in CameraController. camera-core
    // brings guava transitively at runtime, but does not re-export it. Declare directly here.
    // Rule 3 auto-fix in Plan 02-04: 02-RESEARCH.md §A5 claimed awaitInstance() exists on
    // ProcessCameraProvider.Companion; it does not in 1.6.0 — only getInstance() returning
    // ListenableFuture. ~2.7MB jar; acceptable for a prank app.
    implementation(libs.guava)

    // Test — Mockito for CameraControllerTest (ProcessCameraProvider mocking)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)

    // Robolectric — JVM-side implementation of android.util.ArrayMap + Looper + Handler so
    // CameraX Preview.Builder / ImageAnalysis.Builder can be constructed inside unit tests.
    // Plan 02-04 Task 3 Rule 3 auto-fix: CameraControllerTest invokes controller.bind(),
    // which calls Preview.Builder() → OptionsBundle.from() → ArrayMap.keySet(). The JVM
    // android.jar has ArrayMap stubbed; Robolectric provides a real shadow impl.
    testImplementation(libs.robolectric)

    // kotlinx-coroutines-test — runTest / UnconfinedTestDispatcher for CameraViewModelTest
    // Rule 3 auto-fix (Plan 03-01 Task 2): CameraViewModelTest uses runTest{} which requires
    // kotlinx-coroutines-test on the test classpath. Not pulled transitively by lifecycle.
    testImplementation(libs.kotlinx.coroutines.test)

    // Coil — picker thumbnail AsyncImage (04-CONTEXT D-07)
    implementation(libs.coil.compose)

    // DataStore Preferences — last-used filter persistence (04-CONTEXT D-25)
    implementation(libs.androidx.datastore.preferences)

    // Turbine — Flow testing harness for FilterPrefsRepository + CameraViewModel Flow tests (04-VALIDATION Wave 0)
    testImplementation(libs.turbine)

    // DataStore-core for InMemoryDataStore test factory (04-VALIDATION Wave 0)
    testImplementation(libs.androidx.datastore.preferences.core)
}

import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.google.services)
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  jvmToolchain(17)
}

android {
  namespace = "com.stremflix"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.stremflix"
    minSdk = 27
    targetSdk = 35
    versionCode = (findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
    versionName = (findProperty("versionName") as String?) ?: "1.0.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    
    manifestPlaceholders["oauthRedirectScheme"] = "stremflix"
  }

  signingConfigs {
    // Only configured when the keystore + credentials are actually present, so
    // plain `assembleDebug`/CI builds never fail on missing signing material.
    val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
    if (file(keystorePath).exists()) {
      create("release") {
        storeFile = file(keystorePath)
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }

    val debugKeystore = file("${rootDir}/debug.keystore")
    if (debugKeystore.exists()) {
      create("debugConfig") {
        storeFile = debugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.findByName("release")
    }
    debug { }
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  testOptions { unitTests { isIncludeAndroidResources = true } }
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)

  // Compose Material3 & TV Material
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.tv.material)

  // Room Database
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)

  // Media3 ExoPlayer
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.exoplayer.dash)
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.session)

  // Image Loading & Networking
  implementation(libs.coil.compose)
  implementation(libs.retrofit)
  implementation(libs.converter.moshi)
  implementation(libs.okhttp)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  ksp(libs.moshi.kotlin.codegen)

  // Serialization & Coroutines & DataStore
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.androidx.datastore.preferences)

  // Constrained background job for Trakt / continue-watching reconciliation
  implementation(libs.androidx.work.runtime.ktx)

  debugImplementation(libs.androidx.compose.ui.tooling)
}

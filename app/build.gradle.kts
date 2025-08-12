plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
  namespace = "com.errortoken.linemanwebview"
  compileSdk = 35
  defaultConfig {
    applicationId = "com.errortoken.linemanwebview"
    minSdk = 23
    targetSdk = 35
    versionCode = 3
    versionName = "3.0"
  }
  signingConfigs {
    create("ci") {
      storeFile = if (System.getenv("ANDROID_KEYSTORE_PATH") != null) file(System.getenv("ANDROID_KEYSTORE_PATH")) else null
      storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
      keyAlias = System.getenv("ANDROID_KEYSTORE_ALIAS")
      keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
    }
  }
  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("ci")
    }
  }
  compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
  kotlinOptions { jvmTarget = "17" }
}
dependencies {
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.activity:activity-ktx:1.9.1")
  implementation("androidx.webkit:webkit:1.11.0")
  implementation("org.json:json:20240303")
}

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android { namespace = "ca.nymma.kiosk"; compileSdk = 35
 defaultConfig { applicationId = "ca.nymma.silverbackkiosk"; minSdk = 26; targetSdk = 35; versionCode = 11; versionName = "1.11.0" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 } }
kotlin { jvmToolchain(17) }
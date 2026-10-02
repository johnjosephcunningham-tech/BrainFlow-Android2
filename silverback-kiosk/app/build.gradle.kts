plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "ca.nymma.kiosk"
    compileSdk = 35
    defaultConfig { applicationId = "ca.nymma.kiosk"; minSdk = 26; targetSdk = 35; versionCode = 2; versionName = "1.1.0" }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "za.co.rc.tv.phone"
    compileSdk = 36
    defaultConfig { applicationId = "za.co.rc.tv.phone"; minSdk = 26; targetSdk = 36; versionCode = 6; versionName = "0.5.1-rc1"; testInstrumentationRunner = "za.co.rc.tv.phone.LinkClientCheck" }
    sourceSets.getByName("main").java.srcDir(rootProject.file("shared/src"))
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

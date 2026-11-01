plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "za.co.rc.tv"
    compileSdk = 36
    defaultConfig {
        applicationId = "za.co.rc.tv"
        testInstrumentationRunner = "za.co.rc.tv.WallpaperImportCheck"
        minSdk = 30
        targetSdk = 36
        versionCode = 44
        versionName = "1.0.0-rc1.15"
    }
    buildFeatures { compose = true; buildConfig = true }
    sourceSets.getByName("main").java.srcDir(rootProject.file("shared/src"))
    defaultConfig { buildConfigField("boolean", "PROFILE_DIAGNOSTICS", "false") }
    buildTypes {
        create("profile") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            isMinifyEnabled = false
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "PROFILE_DIAGNOSTICS", "true")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    testImplementation("junit:junit:4.13.2")
}

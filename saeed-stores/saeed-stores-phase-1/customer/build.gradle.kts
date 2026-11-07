import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localSettings = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val signingSettings = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val supabaseUrl = localSettings.getProperty("SUPABASE_URL", "").trim()
val supabaseKey = localSettings.getProperty("SUPABASE_KEY", "").trim()
require(!supabaseKey.startsWith("sb_secret_")) {
    "SUPABASE_KEY must be a low-privilege public key, never a secret key."
}
fun javaString(value: String): String = "\"" + value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r") + "\""

android {
    namespace = "com.zakeleo877.saeedstore"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }
    defaultConfig {
        applicationId = "com.zakeleo877.saeedstore"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "SUPABASE_URL", javaString(supabaseUrl))
        buildConfigField("String", "SUPABASE_KEY", javaString(supabaseKey))
    }
    signingConfigs {
        create("release") {
            signingSettings.getProperty("storeFile")?.takeIf { it.isNotBlank() }
                ?.let { storeFile = rootProject.file(it) }
            storePassword = signingSettings.getProperty("storePassword")
            keyAlias = signingSettings.getProperty("keyAlias")
            keyPassword = signingSettings.getProperty("keyPassword")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    androidResources {
        localeFilters += listOf("ar")
        generateLocaleConfig = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}

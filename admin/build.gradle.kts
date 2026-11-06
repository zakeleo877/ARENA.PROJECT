import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
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

/** Reads the `role` claim of a legacy JWT key, or null when the key is opaque. */
fun roleOfLegacyJwt(key: String): String? {
    val parts = key.split('.')
    if (parts.size != 3) return null
    return try {
        val payload = String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8)
        Regex("\"role\"\\s*:\\s*\"([^\"]+)\"").find(payload)?.groupValues?.get(1)
    } catch (_: Exception) {
        null
    }
}

// A secret key bypasses row level security, so a package that carried one would
// hand every reader of that package full control of the database. Both shapes
// are refused: the newer token and the legacy service JWT.
val forbiddenRoles = setOf("service" + "_role", "supabase_admin")
require(roleOfLegacyJwt(supabaseKey) !in forbiddenRoles) {
    "SUPABASE_KEY must never be a high-privilege key."
}
require(!supabaseKey.startsWith("sb_secret_")) {
    "SUPABASE_KEY must never be a secret key; the applications use the publishable key."
}
// And the value that is used has to be a key that can reach the project at all.
require(
    supabaseKey.isEmpty() ||
        supabaseKey.startsWith("sb_publishable_") ||
        roleOfLegacyJwt(supabaseKey) == "anon",
) {
    "SUPABASE_KEY must be a publishable key (sb_publishable_...) or a legacy anon JWT."
}

fun javaString(value: String): String = "\"" + value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r") + "\""

android {
    namespace = "com.zakeleo877.saeedadmin"
    // The compile SDK can be overridden from a Gradle property or an
    // environment variable, so the project builds on runners that only ship a
    // stable platform. Blank values are ignored. The value accepts a minor
    // version, for example 37.2.
    val saeedCompileSdk = (providers.gradleProperty("saeed.compileSdk")
        .orElse(providers.environmentVariable("SAEED_COMPILE_SDK"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .orElse("android-37")
        .get())
    // The value must use the android-NN format, with an optional minor version.
    // The `compileSdk` block replaces the deprecated `compileSdkVersion` call,
    // which Kotlin treats as an error in a build script.
    compileSdk {
        version = release(saeedCompileSdk.removePrefix("android-").toIntOrNull() ?: 37)
    }
    defaultConfig {
        applicationId = "com.zakeleo877.saeedadmin"
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
        isCoreLibraryDesugaringEnabled = true
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
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.sizes)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.hilt.android)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.window)
    implementation(libs.androidx.work.runtime)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    ksp(libs.hilt.compiler)
    // The compiler of the AndroidX Hilt builds the factory that creates the view models.
    ksp(libs.androidx.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}

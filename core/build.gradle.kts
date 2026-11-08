plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.zakeleo877.saeedcore"
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
    // The `compileSdk` block replaces the deprecated call.
    compileSdk {
        version = release(saeedCompileSdk.removePrefix("android-").toIntOrNull() ?: 37)
    }
    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    // The lint model for unit tests cannot resolve a handful of references that
    // only exist when the test classpath is assembled at runtime, and it fails
    // the task instead of reporting a warning. Lint on the main source set is
    // unaffected; `./gradlew lint` still runs there.
    lint {
        checkTestSources = false
        abortOnError = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.maxHeapSize = providers.gradleProperty("saeed.testHeap").getOrElse("768m")
            it.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
            // The colours under test come from this module's own resources. Asking
            // Robolectric to fetch the framework jar over the network made the
            // class fail on a runner with restricted egress, reported only as
            // "classMethod FAILED ... IOException".
            it.systemProperty("robolectric.useFullClasspathForInstrumentation", "true")
            it.systemProperty("robolectric.offline", "false")
            it.systemProperty("robolectric.logging.enabled", "true")
        }
    }
}

room3 { schemaDirectory("$projectDir/schemas") }

/**
 * Starts the isolated Supabase-compatible backend for the duration of the
 * unit tests. The backend is a private PostgreSQL instance created from
 * `supabase/schema.sql`; it listens on the loopback interface only and uses
 * dummy data. No production project and no production key is contacted.
 *
 * The address and the public key are read from the process output and passed
 * to the test JVM through environment variables.
 */
val isolatedBackendScript = rootProject.file("supabase/tests/run-isolated-backend.cjs")
val isolatedBackendDirectory = rootProject.file("supabase/tests")

/** Installs the Node dependencies the isolated backend needs. */
val npmInstallForTests by tasks.registering(Exec::class) {
    description = "Installs the dependencies of the isolated backend."
    group = "verification"
    workingDir = isolatedBackendDirectory
    commandLine("npm", "ci", "--no-audit", "--no-fund")
    inputs.files(rootProject.file("supabase/tests/package-lock.json"))
    outputs.dir(rootProject.file("supabase/tests/node_modules"))
}

tasks.withType<Test>().configureEach {
    val script = isolatedBackendScript
    val directory = isolatedBackendDirectory
    dependsOn("npmInstallForTests")
    val processHolder = arrayOfNulls<Process>(1)
    doFirst {
        val process = ProcessBuilder("node", script.absolutePath)
            .directory(directory)
            .redirectErrorStream(true)
            .start()
        processHolder[0] = process
        val reader = process.inputStream.bufferedReader()
        var line = reader.readLine() ?: ""
        while (line.isNotBlank() && !line.startsWith("SAEED_TEST_BACKEND_URL=")) {
            line = reader.readLine() ?: ""
        }
        if (!line.startsWith("SAEED_TEST_BACKEND_URL=")) {
            process.destroy()
            throw GradleException("The isolated backend did not report its address.")
        }
        val parts = line.trim().split(" ")
        environment("SAEED_TEST_BACKEND_URL", parts.first().substringAfter("="))
        environment("SAEED_TEST_BACKEND_KEY", parts.last().substringAfter("="))
    }
    doLast {
        processHolder[0]?.destroy()
        processHolder[0] = null
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    // The shared product-image composable and its loader live here so the shop
    // and the admin panel cannot drift apart in how they load a photograph.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    // The Compose test rules let the screens be driven and asserted on the
    // JVM through Robolectric, without needing a device or an emulator.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.runner)
    testImplementation(libs.androidx.rules)
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

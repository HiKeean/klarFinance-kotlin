import java.io.FileInputStream
import java.net.URI
import java.net.URISyntaxException
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

// Loads kotlin/.env (gitignored, one per dev machine — see .env.example).
val envProperties = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        load(FileInputStream(envFile))
    }
}

// properties dari java.util
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(propertyKey: String, envKey: String): String? =
    keystoreProperties.getProperty(propertyKey)
        ?: providers.environmentVariable(envKey).orNull

val releaseStoreFile = signingValue("storeFile", "ANDROID_KEYSTORE_PATH")
val hasReleaseSigning = releaseStoreFile != null

fun envProp(key: String, default: String): String = envProperties.getProperty(key) ?: default

val baseUrlHost: String = try {
    URI(envProp("BASE_URL", "http://10.10.14.124:8080/")).host ?: "10.10.14.124"
} catch (e: URISyntaxException) {
    "10.10.14.124"
}

// Generates res/xml/network_security_config.xml from BASE_URL's host so the cleartext
// allowlist always matches kotlin/.env — no more manually editing the XML per dev machine.
abstract class GenerateNetworkSecurityConfigTask : DefaultTask() {
    @get:Input
    abstract val cleartextHost: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val xmlDir = outputDir.get().asFile.resolve("xml")
        xmlDir.mkdirs()
        xmlDir.resolve("network_security_config.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <!-- Generated at build time from kotlin/.env's BASE_URL host - do not edit by hand,
                 edit .env instead. Local dev backend only; remove once BASE_URL is HTTPS. -->
            <network-security-config>
                <domain-config cleartextTrafficPermitted="true">
                    <domain includeSubdomains="false">${cleartextHost.get()}</domain>
                    <domain includeSubdomains="false">10.0.2.2</domain>
                    <domain includeSubdomains="false">localhost</domain>
                </domain-config>
            </network-security-config>
            """.trimIndent()
        )
    }
}

val generateNetworkSecurityConfig = tasks.register<GenerateNetworkSecurityConfigTask>("generateNetworkSecurityConfig") {
    cleartextHost.set(baseUrlHost)
    outputDir.set(layout.buildDirectory.dir("generated/res/networkSecurityConfig"))
}

android {
    namespace = "com.klarfinance.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.klarfinance.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Sourced from kotlin/.env — see .env.example.
        buildConfigField("String", "BASE_URL", "\"${envProp("BASE_URL", "http://10.10.14.124:8080/")}\"")
        buildConfigField("String", "API_KEY", "\"${envProp("API_KEY", "")}\"")
        buildConfigField("String", "SECRET_KEY", "\"${envProp("SECRET_KEY", "")}\"")
        buildConfigField("String", "CLIENT_TYPE", "\"${envProp("CLIENT_TYPE", "ANDROID")}\"")
    }

    // Release signing is optional locally (unsigned release builds still work for testing).
    // CI provides RELEASE_KEYSTORE_* via a generated kotlin/.env so this reuses the same
    // envProp() mechanism as BASE_URL/API_KEY above instead of a separate secrets path.

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = signingValue("storePassword", "RELEASE_KEYSTORE_PASSWORD" )
                keyAlias = signingValue("keyAlias", "RELEASE_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "RELEASE_KEY_PASSWORD")
            }
        }
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseStoreFile != null && hasReleaseSigning && releaseStoreFile.isNotBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
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
}

androidComponents {
    onVariants { variant ->
        variant.sources.res?.addGeneratedSourceDirectory(
            generateNetworkSecurityConfig,
            GenerateNetworkSecurityConfigTask::outputDir
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.coil.compose)
    // Generate QR mockup buat tiket Transjakarta (beda dari mlkit.barcode.scanning di atas -
    // itu buat DECODE/scan QRIS, ini buat bikin/render QR-nya, gak butuh izin kamera).
    implementation(libs.zxing.core)

    implementation(libs.androidx.biometric)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.fragment.ktx)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.play.services.location)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

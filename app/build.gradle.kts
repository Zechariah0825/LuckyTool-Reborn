import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.lsplugin.resopt)
    alias(libs.plugins.lsplugin.lsparanoid)
    alias(libs.plugins.kotlin.serialization)
}

lsparanoid {
    includeDependencies = false
    variantFilter = { variant -> variant.name == "release" }
}

android {
    namespace = gropify.project.groupName
    compileSdk = gropify.project.android.compileSdk

    defaultConfig {
        applicationId = "io.github.zechariah0825.luckytoolreborn"

        minSdk = gropify.project.android.minSdk
        targetSdk = gropify.project.android.targetSdk

        versionCode = getVersionCode()
        versionName = providers.gradleProperty("rebornVersionName").orElse("1.3.5_reborn.10").get()
        ndk.abiFilters.addAll(arrayOf("arm64-v8a"/*, "armeabi-v7a", "x86", "x86_64"*/))
    }
    // A clean checkout can build debug APKs without private upstream signing files.
    // Release signing is opt-in; never publish private keys or passwords.
    val signingPropertiesFile = rootProject.file("keystore/keystore.properties")
    val releaseSigning = if (signingPropertiesFile.isFile) {
        val signingProperties = Properties().apply {
            signingPropertiesFile.inputStream().use { load(it) }
        }
        signingConfigs.create("rebornRelease") {
            val required = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
            required.forEach { key ->
                check(!signingProperties.getProperty(key).isNullOrBlank()) {
                    "Missing $key in keystore/keystore.properties"
                }
            }
            storeFile = rootProject.file(signingProperties.getProperty("storeFile"))
            storePassword = signingProperties.getProperty("storePassword")
            keyAlias = signingProperties.getProperty("keyAlias")
            keyPassword = signingProperties.getProperty("keyPassword")
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    } else null
    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = releaseSigning
        }
        debug {
            isDebuggable = false
            isMinifyEnabled = false
            //noinspection NotShrinkingResources
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
    }
    androidResources.additionalParameters.addAll(
        arrayOf("--allow-reserved-package-id", "--package-id", "0x64")
    )
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    packaging {
        dex {
            useLegacyPackaging = true
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

@Suppress("UnstableApiUsage")
androidComponents {
    onVariants { variant ->
        val buildType = variant.buildType ?: "None"
        variant.outputs.forEach { output ->
            val versionName = output.versionName.get()
            val versionCode = output.versionCode.get()
            val version = "$versionName($versionCode)"
            println("buildVersion -> $version ($buildType)")

            output.outputFileName.set("LuckyTool_v${version}_${buildType}.apk")
            println("outputFileName -> ${output.outputFileName.get()}")
        }
    }
}

dependencies {
    compileOnly(projects.hiddenApiStub)

    @Suppress("AvoidDuplicateDependencies")
    platform(libs.yukihook.bom).apply {
        implementation(this)
        ksp(this)
    }
    implementation(libs.yukihook.core)
    ksp(libs.yukihook.compiler)
    implementation(libs.yukihook.runtime.libxposed)
    implementation(libs.yukihook.extension.kavaref)

    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)

    implementation(libs.kavaref.core)
    implementation(libs.kavaref.android)
    implementation(libs.kavaref.extension)

    implementation(libs.dexkit)
    implementation(libs.hiddenapibypass)

    //AndroidX
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    //Compose (BOM 统一版本)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.material.kolor)
    implementation(libs.androidx.compose.material.icons.extended)
    //Miuix
    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.nav)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.blur)
    //Markdown
    implementation(libs.markwon.core)
    implementation(libs.markwon.html)
    implementation(libs.markwon.image)
    implementation(libs.markwon.ext.tables)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.appiconloader.iconloaderlib)

    //Android UI
    implementation(libs.betterandroid.ui.component)
    implementation(libs.betterandroid.ui.component.adapter)
    implementation(libs.betterandroid.ui.extension)
    implementation(libs.betterandroid.system.extension)

    //KotlinX
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)

    //OkHttp3
    implementation(libs.okhttp3)
    implementation(libs.net)

    //LibSU
    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
    implementation(libs.libsu.io)

    //Tools
    implementation(libs.deviceCompat)
    implementation(libs.xxpermissions)
    implementation(libs.spiderman)
    implementation(libs.android.image.cropper)

    //Test
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}

// Robolectric runs several SDK levels; keep its heap explicit and CI reproducible.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    maxHeapSize = "1536m"
    maxParallelForks = 1
}

fun getVersionCode(): Int {
    val configured = providers.gradleProperty("rebornVersionCode").orNull
    val stored = Properties().apply {
        file("version.properties").inputStream().use { load(it) }
    }.getProperty("versionCode")
    return (configured ?: stored).toIntOrNull()?.takeIf { it > 0 }
        ?: throw GradleException("versionCode must be a positive integer")
}

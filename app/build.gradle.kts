import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

val releaseStoreFile = providers.environmentVariable("OPENVIDEO_RELEASE_STORE_FILE")
    .orElse(localProperties.getProperty("OPENVIDEO_RELEASE_STORE_FILE", ""))
    .get()
val releaseStorePassword = providers.environmentVariable("OPENVIDEO_RELEASE_STORE_PASSWORD")
    .orElse(localProperties.getProperty("OPENVIDEO_RELEASE_STORE_PASSWORD", ""))
    .get()
val releaseKeyAlias = providers.environmentVariable("OPENVIDEO_RELEASE_KEY_ALIAS")
    .orElse(localProperties.getProperty("OPENVIDEO_RELEASE_KEY_ALIAS", ""))
    .get()
val releaseKeyPassword = providers.environmentVariable("OPENVIDEO_RELEASE_KEY_PASSWORD")
    .orElse(localProperties.getProperty("OPENVIDEO_RELEASE_KEY_PASSWORD", ""))
    .get()
val releaseSigningConfigured = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { it.isNotBlank() }

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.example.openvideo"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.openvideo"
        minSdk = 23
        targetSdk = 35
        versionCode = providers.gradleProperty("VERSION_CODE").get().toInt()
        versionName = providers.gradleProperty("VERSION_NAME").get()

        val feishuWebhookUrl = providers.environmentVariable("FEISHU_WEBHOOK_URL")
            .orElse(localProperties.getProperty("FEISHU_WEBHOOK_URL", ""))
            .get()
        buildConfigField("String", "FEISHU_WEBHOOK_URL", feishuWebhookUrl.asBuildConfigString())
        buildConfigField("Boolean", "REMOTE_CRASH_REPORTING_ENABLED", feishuWebhookUrl.isNotBlank().toString())
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs {
            keepDebugSymbols += "**/libffmpegJNI.so"
        }
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Material Design
    implementation(libs.material)

    // Media3 (ExoPlayer)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)
    implementation(libs.media3.effect)
    implementation(libs.media3.ffmpeg.decoder)
    implementation(libs.androidx.media)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Glide
    implementation(libs.glide)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Window (WindowSizeClass)
    implementation(libs.androidx.window)
    implementation(libs.androidx.security.crypto)

    // Open-source license viewer. License resources are packaged from src/main/res/raw.
    implementation(libs.play.services.oss.licenses)

    testImplementation(libs.junit)
}

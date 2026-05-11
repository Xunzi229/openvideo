plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.gms.oss.licenses)
}

android {
    namespace = "com.example.openvideo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.openvideo"
        minSdk = 23
        targetSdk = 35
        versionCode = 4
        versionName = "0.0.4"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
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

    // Open-source licenses (generated from Gradle dependencies at build time)
    implementation(libs.play.services.oss.licenses)

    testImplementation(libs.junit)
}

// OSS 插件对可调试变体不写依赖列表，只生成占位项「Debug License Info」。
// 在 debug 许可任务末尾用 release 的 raw 覆盖，避免单独 Copy 任务触发 Gradle 9 资源合并隐式依赖报错。
afterEvaluate {
    tasks.named("debugOssLicensesTask").configure {
        enabled = false
    }
}

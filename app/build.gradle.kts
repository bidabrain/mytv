plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mytv.live"
    compileSdk = 35   // GeckoView 新版要求 compileSdk 35

    defaultConfig {
        applicationId = "com.mytv.live"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    // 按 ABI 分包：盒子装 armeabi-v7a、平板装 arm64-v8a，各自只含一套 Gecko 引擎。
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
    }
    // Kotlin 2.0+ 已内置 Compose 编译器，无需手动指定版本
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.15.0")  // 对齐 GeckoView 136 拉入的 core 1.15.0
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // GeckoView（第二阶段引擎）：Firefox 引擎打包进 APK，跨设备一致 + Widevine DRM。
    // 锁 136 兼容现有 Kotlin 2.0.21 / AGP 8.7.3（≥140 拉新版 stdlib/core/media3 需升工具链）。
    implementation("org.mozilla.geckoview:geckoview:136.0.20250317200840")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

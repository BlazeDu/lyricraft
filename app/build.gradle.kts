plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.storm.lyricraft"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.storm.lyricraft"
        minSdk = 24
        targetSdk = 37
        versionCode = 8
        versionName = "3.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.02.01")
    implementation(composeBom)

    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")


    // Gson - 解析 GitHub API JSON
    implementation("com.google.code.gson:gson:2.10.1")

    // OkHttp - 网络请求
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Media3音频播放器
    implementation("androidx.media3:media3-exoplayer:1.6.0")
    implementation("androidx.media3:media3-ui:1.6.0")

    implementation("androidx.documentfile:documentfile:1.1.0")

    // Material 基础图标库
    implementation("androidx.compose.material:material-icons-core:1.7.0")
// 扩展图标（Filled文件夹图标就在这里）
    implementation("androidx.compose.material:material-icons-extended:1.7.5")

    implementation("androidx.navigation:navigation-compose:2.8.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
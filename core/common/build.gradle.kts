// :core:common — 结果封装、调度器注入、DataStore（纯前端状态）。
// 不依赖引擎与 UI。
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.snishaper.android.core.common"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(libs.kotlinx.coroutines.android)
    api(libs.androidx.datastore.preferences)
}

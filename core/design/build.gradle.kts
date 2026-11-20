// :core:design — 设计系统：主题（6 套配色 × 明暗）、字体、通用组件、图标。
// 只依赖 Compose，禁止依赖 data/engine/feature。
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.snishaper.android.core.design"
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    api(composeBom)
    api(libs.compose.material3)
    api(libs.compose.ui)
    api(libs.compose.ui.tooling.preview)
    api(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
}

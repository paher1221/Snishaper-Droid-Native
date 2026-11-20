// :core:data — 仓库层：按领域封装引擎调用。
// 边界：全仓库唯一允许经由 :core:engine 触碰引擎的模块；
// feature 层只依赖本模块与 :core:model / :core:design / :core:common。
//
// M0 先落模块与依赖边界；18 个领域仓库接口（见 docs/ARCHITECTURE.md §4 映射表）
// 在 M1 按域逐个补齐，每个接口落地时同步补 :core:model 的领域类型。
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.snishaper.android.core.data"
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
    api(project(":core:model"))
    implementation(project(":core:engine"))
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.android)
    // TunRepository: ContextCompat.startForegroundService
    implementation(libs.androidx.core.ktx)
}

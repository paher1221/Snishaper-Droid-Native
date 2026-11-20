// :app — 壳工程：Application、单 Activity 导航宿主、DI 装配、清单与图标。
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.snishaper.android.app"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        // 用户确认：包名在原包基础上加 native 后缀（正式替代路线，覆盖安装升级）。
        applicationId = "com.snishaper.android.native"
        minSdk = 24
        targetSdk = 36
        versionName = "1.0.0-native"
        versionCode = 1
    }

    buildTypes {
        debug {
            // 与旧版（com.snishaper.android）并行安装做对照测试。
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            // 签名配置随发布流水线（M5）接入；当前 release 仅本地构建用途。
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:design"))
    implementation(project(":core:engine"))
    implementation(project(":core:data"))

    implementation(project(":feature:dashboard"))
    implementation(project(":feature:connections"))
    implementation(project(":feature:rules"))
    implementation(project(":feature:dns"))
    implementation(project(":feature:proxies"))
    implementation(project(":feature:routing"))
    implementation(project(":feature:evolution"))
    implementation(project(":feature:logs"))
    implementation(project(":feature:profiles"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:onboarding"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}

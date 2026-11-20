// :core:engine — 后端宿主层（后端不变，原样迁移）：
//   * gomobile AAR 接入（com.snishaper.core.mobile.App）
//   * SnishaperEngine 单例：Listener → Flow
//   * TunVpnService / VpnInstaller：VpnService TUN 隧道
//   * AssetSeeder：首次启动播种桌面版默认配置
//
// 边界：引擎类以 implementation 引入——gomobile 的 App 类型不对外暴露，
// 只有本模块能触碰引擎；:core:data 及以上只能经由本模块的 API。
//
// AAR 拆解：AGP 禁止库模块以本地 .aar 为依赖参与 AAR 打包（bundle/lint 失败），
// 因此 CI/本地先用 scripts/extract-aar.sh 把 AAR 拆成 classes.jar + jniLibs：
//   * libs/snishaper-core-classes.jar → 编译类路径（git 忽略）
//   * src/main/jniLibs/<abi>/*.so     → 原生库，随模块进入 APK（git 忽略）
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.snishaper.android.core.engine"
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
    val snishaperClasses = file("libs/snishaper-core-classes.jar")
    if (snishaperClasses.exists()) {
        // implementation（非 api）：App 类型不进入消费方的编译类路径。
        implementation(files(snishaperClasses))
    }

    implementation(libs.androidx.core.ktx)
    implementation(project(":core:model"))
    implementation(project(":core:common"))
}

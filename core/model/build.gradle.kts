// :core:model — 引擎 JSON → Kotlin 数据模型的唯一解析点。
// 纯 Kotlin/JVM 模块：禁止依赖 Android 与引擎，保证可独立测试。
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// 不锁 toolchain（避免强制下载 JDK 17）：对齐 Android 模块的 Java 17 字节码目标。
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    api(libs.kotlinx.serialization.json)
}

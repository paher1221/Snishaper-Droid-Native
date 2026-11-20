// 顶层构建文件：所有插件版本在此钉死（单一版本来源）。
//
// AGP 9.0+ 自带 Kotlin 支持，禁止再应用 org.jetbrains.kotlin.android
// （会报 "no longer required since AGP 9.0"）。Compose 模块单独应用
// org.jetbrains.kotlin.plugin.compose（版本与 Kotlin 一致）。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

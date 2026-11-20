package com.snishaper.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.snishaper.android.core.design.theme.AppearanceMode
import com.snishaper.android.core.design.theme.SnishaperTheme

/**
 * 单 Activity 壳：全部 UI 为 Compose（无 WebView）。
 * M0 主题先跟随系统明暗；语言/配色选择随 M5 设置页接入（仍走引擎存取，
 * 见 ARCHITECTURE.md §4 边界要点 1）。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SnishaperTheme(appearanceMode = AppearanceMode.SYSTEM) {
                SnishaperApp()
            }
        }
    }
}

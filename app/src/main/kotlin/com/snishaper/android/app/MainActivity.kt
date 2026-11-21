package com.snishaper.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.snishaper.android.core.design.theme.AppearanceController
import com.snishaper.android.core.design.theme.SnishaperTheme

/**
 * 单 Activity 壳：全部 UI 为 Compose（无 WebView）。
 * 外观模式由设置页经 AppearanceController 即时切换（引擎设置文档为事实来源）；
 * 语言/配色选择随 M5 设置页接入（仍走引擎存取，见 ARCHITECTURE.md §4 边界要点 1）。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appearance by AppearanceController.mode.collectAsStateWithLifecycle()
            SnishaperTheme(appearanceMode = appearance) {
                SnishaperApp()
            }
        }
    }
}

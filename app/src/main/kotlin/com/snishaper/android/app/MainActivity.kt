package com.snishaper.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.snishaper.android.core.design.theme.AppearanceController
import com.snishaper.android.core.design.theme.SnishaperTheme
import com.snishaper.android.core.design.theme.ThemeController

/**
 * 单 Activity 壳：全部 UI 为 Compose（无 WebView）。
 * 外观模式与配色由设置页经 AppearanceController / ThemeController 即时切换
 * （引擎设置文档为事实来源）；界面语言固定简体中文。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appearance by AppearanceController.mode.collectAsStateWithLifecycle()
            val colorScheme by ThemeController.scheme.collectAsStateWithLifecycle()
            SnishaperTheme(appearanceMode = appearance, colorSchemeId = colorScheme) {
                SnishaperApp()
            }
        }
    }
}

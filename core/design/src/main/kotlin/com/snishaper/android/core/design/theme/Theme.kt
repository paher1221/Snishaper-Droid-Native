package com.snishaper.android.core.design.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * SniShaper 主题。
 *
 * M0 先提供明暗两套基础配色；桌面版的 6 套配色（含自定义色板）在 M5 阶段
 * 按 [ColorSchemeId] 逐套补齐。Android 12+ 默认跟随系统动态取色，
 * 用户显式选择配色后覆盖动态取色。
 */

/** 桌面版 6 套配色的占位枚举；M5 补齐各自色板。 */
enum class ColorSchemeId {
    DEFAULT,
    OCEAN,
    FOREST,
    SUNSET,
    LAVENDER,
    MONO,
}

/** 外观模式：与引擎 settings 中的 appearanceMode 对齐（light / dark / system）。 */
enum class AppearanceMode { LIGHT, DARK, SYSTEM }

private val LightColors = lightColorScheme(
    primary = Color(0xFF00696E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF1F8),
    onPrimaryContainer = Color(0xFF002022),
    secondary = Color(0xFF4A6367),
    surface = Color(0xFFF5FAFA),
    onSurface = Color(0xFF161D1E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4FD8E2),
    onPrimary = Color(0xFF003739),
    primaryContainer = Color(0xFF004F53),
    onPrimaryContainer = Color(0xFF9CF1F8),
    secondary = Color(0xFFB1CBCE),
    surface = Color(0xFF0E1415),
    onSurface = Color(0xFFDDE4E4),
)

@Composable
fun SnishaperTheme(
    appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    // M0：动态取色仅在用户未显式选择配色时使用；配色选择 UI 在 M5 落地。
    colorSchemeId: ColorSchemeId? = null,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (appearanceMode) {
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val useDynamic = colorSchemeId == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colors = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamic && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}

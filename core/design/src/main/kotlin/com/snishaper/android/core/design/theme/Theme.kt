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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SniShaper 主题：6 套配色 × 明暗，对齐桌面版 frontend theme/index.ts 色值。
 * 外观模式（light/dark/system）由 [AppearanceController] 持有；
 * 配色由 [ThemeController] 持有——未选择配色时 Android 12+ 跟随系统动态取色。
 */

/** 桌面版 6 套配色；wire 值与旧前端 availableThemes 的 id 一致（存引擎设置文档 theme 键）。 */
enum class ColorSchemeId(val wire: String) {
    DEFAULT("default"),
    SNISHAPER("sniShaper"),
    FOREST("forest"),
    SUNSET("sunset"),
    OCEAN("ocean"),
    GRAPHITE("graphite");

    companion object {
        fun fromWire(value: String): ColorSchemeId? =
            entries.firstOrNull { it.wire.equals(value, ignoreCase = true) }
    }
}

/** 外观模式：与引擎 settings 中的 appearance_mode 对齐（light / dark / system）。 */
enum class AppearanceMode { LIGHT, DARK, SYSTEM }

/** 配色选择的全局持有者：null = 未显式选择（Android 12+ 动态取色）。 */
object ThemeController {

    private val _scheme = MutableStateFlow<ColorSchemeId?>(null)
    val scheme: StateFlow<ColorSchemeId?> = _scheme.asStateFlow()

    fun set(id: ColorSchemeId?) {
        _scheme.value = id
    }
}

private val LightTextPrimary = Color(0xFF000000).copy(alpha = 0.87f)
private val LightTextSecondary = Color(0xFF000000).copy(alpha = 0.6f)
private val DarkTextPrimary = Color(0xFFFFFFFF)
private val DarkTextSecondary = Color(0xFFFFFFFF).copy(alpha = 0.7f)

private fun lightScheme(
    primary: Color, primaryLight: Color, primaryDark: Color,
    secondary: Color, secondaryLight: Color, secondaryDark: Color,
    background: Color = Color(0xFFFFFFFF), paper: Color = Color(0xFFF5F5F5),
    onPrimary: Color = Color.White,
) = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    secondary = secondary,
    background = background,
    surface = paper,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
)

private fun darkScheme(
    primary: Color, primaryLight: Color, primaryDark: Color,
    secondary: Color, secondaryLight: Color, secondaryDark: Color,
    background: Color, paper: Color,
    textPrimary: Color = DarkTextPrimary, textSecondary: Color = DarkTextSecondary,
    onPrimary: Color = Color.White,
) = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    secondary = secondary,
    background = background,
    surface = paper,
    onSurface = textPrimary,
    onSurfaceVariant = textSecondary,
)

// ---- default：MUI 默认蓝（桌面版 defaultTheme）----
private val DefaultLight = lightScheme(
    Color(0xFF1976D2), Color(0xFF42A5F5), Color(0xFF1565C0),
    Color(0xFF546E7A), Color(0xFF78909C), Color(0xFF37474F),
)
private val DefaultDark = darkScheme(
    Color(0xFF90CAF9), Color(0xFFE3F2FD), Color(0xFF42A5F5),
    Color(0xFF90A4AE), Color(0xFFB0BEC5), Color(0xFF607D8B),
    background = Color(0xFF121212), paper = Color(0xFF1E1E1E),
)

// ---- sniShaper：经典 SniShaper 蓝 ----
private val SniShaperLight = lightScheme(
    Color(0xFF0B7BFF), Color(0xFF3396FF), Color(0xFF0969DA),
    Color(0xFF00ACC1), Color(0xFF4DD0E1), Color(0xFF00838F),
)
private val SniShaperDark = darkScheme(
    Color(0xFF0B7BFF), Color(0xFF3396FF), Color(0xFF0969DA),
    Color(0xFF4DD0E1), Color(0xFF80DEEA), Color(0xFF0097A7),
    background = Color(0xFF0D1117), paper = Color(0xFF161B22),
    textPrimary = Color(0xFFE6EDF3), textSecondary = Color(0xFF8B949E),
)

// ---- forest：森林绿 ----
private val ForestLight = lightScheme(
    Color(0xFF2E7D32), Color(0xFF4CAF50), Color(0xFF1B5E20),
    Color(0xFF00695C), Color(0xFF26A69A), Color(0xFF004D40),
    background = Color(0xFFF7FAF7),
)
private val ForestDark = darkScheme(
    Color(0xFF66BB6A), Color(0xFF81C784), Color(0xFF388E3C),
    Color(0xFF26A69A), Color(0xFF4DB6AC), Color(0xFF00897B),
    background = Color(0xFF0F1A12), paper = Color(0xFF152219),
    textPrimary = Color(0xFFE4EFE6), textSecondary = Color(0xFF93A99A),
    onPrimary = Color(0xFF0B160C),
)

// ---- sunset：日落橙 ----
private val SunsetLight = lightScheme(
    Color(0xFFED6C02), Color(0xFFFF9800), Color(0xFFC25E00),
    Color(0xFF6D4C41), Color(0xFF8D6E63), Color(0xFF4E342E),
    background = Color(0xFFFFFAF4),
)
private val SunsetDark = darkScheme(
    Color(0xFFFF9800), Color(0xFFFFB74D), Color(0xFFE65100),
    Color(0xFFA1887F), Color(0xFFBCAAA4), Color(0xFF795548),
    background = Color(0xFF1A1410), paper = Color(0xFF211A14),
    textPrimary = Color(0xFFF2ECE5), textSecondary = Color(0xFFB3A89C),
    onPrimary = Color(0xFF1A0F00),
)

// ---- ocean：海洋青 ----
private val OceanLight = lightScheme(
    Color(0xFF00897B), Color(0xFF26A69A), Color(0xFF00695C),
    Color(0xFF0277BD), Color(0xFF29B6F6), Color(0xFF01579B),
    background = Color(0xFFF4FAF9),
)
private val OceanDark = darkScheme(
    Color(0xFF4DB6AC), Color(0xFF80CBC4), Color(0xFF26A69A),
    Color(0xFF4FC3F7), Color(0xFF81D4FA), Color(0xFF0288D1),
    background = Color(0xFF0D1A19), paper = Color(0xFF132322),
    textPrimary = Color(0xFFE0EFED), textSecondary = Color(0xFF8FB0AC),
    onPrimary = Color(0xFF0A1A18),
)

// ---- graphite：石墨灰 ----
private val GraphiteLight = lightScheme(
    Color(0xFF546E7A), Color(0xFF78909C), Color(0xFF37474F),
    Color(0xFF8D6E63), Color(0xFFA1887F), Color(0xFF6D4C41),
    background = Color(0xFFF7F8FA),
)
private val GraphiteDark = darkScheme(
    Color(0xFF90A4AE), Color(0xFFB0BEC5), Color(0xFF607D8B),
    Color(0xFFA1887F), Color(0xFFBCAAA4), Color(0xFF795548),
    background = Color(0xFF121418), paper = Color(0xFF1A1D22),
    textPrimary = Color(0xFFE8EAEE), textSecondary = Color(0xFF9AA1AB),
    onPrimary = Color(0xFF101418),
)

@Composable
fun SnishaperTheme(
    appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
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
        else -> {
            val id = colorSchemeId ?: ColorSchemeId.DEFAULT
            when (id) {
                ColorSchemeId.DEFAULT -> if (darkTheme) DefaultDark else DefaultLight
                ColorSchemeId.SNISHAPER -> if (darkTheme) SniShaperDark else SniShaperLight
                ColorSchemeId.FOREST -> if (darkTheme) ForestDark else ForestLight
                ColorSchemeId.SUNSET -> if (darkTheme) SunsetDark else SunsetLight
                ColorSchemeId.OCEAN -> if (darkTheme) OceanDark else OceanLight
                ColorSchemeId.GRAPHITE -> if (darkTheme) GraphiteDark else GraphiteLight
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}

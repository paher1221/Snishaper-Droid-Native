package com.snishaper.android.core.design.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 外观模式的全局持有者：设置页修改后立即生效（MainActivity 收集此流重建主题），
 * 引擎设置文档中的 appearance_mode 是跨端事实来源，由设置页负责同步读写。
 */
object AppearanceController {

    private val _mode = MutableStateFlow(AppearanceMode.SYSTEM)
    val mode: StateFlow<AppearanceMode> = _mode.asStateFlow()

    fun set(mode: AppearanceMode) {
        _mode.value = mode
    }
}

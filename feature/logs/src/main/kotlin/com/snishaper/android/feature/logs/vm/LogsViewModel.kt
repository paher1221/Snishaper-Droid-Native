package com.snishaper.android.feature.logs.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.data.DataGraph
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class LogsUiState(
    val lines: List<String> = emptyList(),
    val isPaused: Boolean = false,
)

/** 日志页：引擎日志流，1.5s 轮询（与旧版一致），支持暂停、清空、搜索。 */
class LogsViewModel : ViewModel() {

    private val repo = DataGraph.logsRepository

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    private var lastRaw: String = ""

    init {
        viewModelScope.launch {
            while (isActive) {
                if (!_uiState.value.isPaused) refresh()
                delay(1_500)
            }
        }
    }

    suspend fun refresh() {
        when (val result = repo.recentLogs(400)) {
            is AppResult.Success -> {
                val raw = result.value
                if (raw != lastRaw) {
                    lastRaw = raw
                    _uiState.update { it.copy(lines = raw.split('\n').filter { l -> l.isNotBlank() }) }
                }
            }
            is AppResult.Failure -> Unit
        }
    }

    fun togglePause() = _uiState.update { it.copy(isPaused = !it.isPaused) }

    fun clear() {
        viewModelScope.launch {
            repo.clear()
            lastRaw = ""
            _uiState.update { it.copy(lines = emptyList()) }
        }
    }
}

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
    /** 已生成的诊断包路径，等待 SAF 目标 Uri 落盘。 */
    val pendingDiagnosticsPath: String? = null,
    val diagnosticsMessage: String? = null,
)

/** 日志页：引擎日志流，1.5s 轮询（与旧版一致），支持暂停、清空、搜索、诊断包导出。 */
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

    /**
     * 生成诊断包（状态 + 日志 + 脱敏设置 + 规则的 zip）。
     * 引擎落盘到 dataDir/diagnostics 后返回路径，由页面经 SAF 另存。
     */
    fun exportDiagnostics() {
        viewModelScope.launch {
            when (val result = repo.exportDiagnostics()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(pendingDiagnosticsPath = result.value.takeIf { p -> p.isNotEmpty() })
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(diagnosticsMessage = result.message)
                }
            }
        }
    }

    /** SAF 目标已选定：把诊断包 zip 复制到 Uri。 */
    fun writePendingDiagnostics(uri: android.net.Uri, contentResolver: android.content.ContentResolver) {
        val path = _uiState.value.pendingDiagnosticsPath ?: return
        viewModelScope.launch {
            val ok = runCatching {
                contentResolver.openOutputStream(uri)?.use { out ->
                    java.io.File(path).inputStream().use { it.copyTo(out) }
                } ?: throw IllegalStateException("cannot open output stream")
            }.isSuccess
            _uiState.update {
                it.copy(
                    pendingDiagnosticsPath = null,
                    diagnosticsMessage = if (ok) "diag_ok" else "diag_failed",
                )
            }
        }
    }

    fun cancelPendingDiagnostics() = _uiState.update { it.copy(pendingDiagnosticsPath = null) }
}

package com.snishaper.android.feature.profiles.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.data.DataGraph
import com.snishaper.android.core.model.ProfileInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfilesUiState(
    val profiles: List<ProfileInfo> = emptyList(),
    val active: String = "",
    val loaded: Boolean = false,
    val busy: Boolean = false,
    /** 操作结果提示（成功或失败消息）。 */
    val message: String? = null,
    /** 名称弹窗：create / rename / duplicate 三模式复用（与旧版一致）。 */
    val nameDialog: NameDialog? = null,
    /** 切换确认弹窗的目标档案名。 */
    val switchTarget: String? = null,
    /** 已取出的导出 bundle，等待 SAF 目标 Uri 落盘。 */
    val pendingExport: PendingExport? = null,
) {
    data class NameDialog(val mode: Mode, val source: String = "") {
        enum class Mode { CREATE, RENAME, DUPLICATE }
    }

    data class PendingExport(val name: String, val bundle: String)
}

class ProfilesViewModel : ViewModel() {

    private val repo = DataGraph.profileRepository

    private val _uiState = MutableStateFlow(ProfilesUiState())
    val uiState: StateFlow<ProfilesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val profiles = when (val r = repo.profiles()) {
                is AppResult.Success -> r.value
                is AppResult.Failure -> _uiState.value.profiles
            }
            val active = when (val r = repo.activeProfile()) {
                is AppResult.Success -> r.value
                is AppResult.Failure -> _uiState.value.active
            }
            _uiState.update { it.copy(profiles = profiles, active = active, loaded = true) }
        }
    }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    // ---- 名称弹窗（create / rename / duplicate 三模式） ----

    fun openNameDialog(mode: ProfilesUiState.NameDialog.Mode, source: String = "") {
        _uiState.update { it.copy(nameDialog = ProfilesUiState.NameDialog(mode, source)) }
    }

    fun closeNameDialog() = _uiState.update { it.copy(nameDialog = null) }

    fun submitName(name: String) {
        val dialog = _uiState.value.nameDialog ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        _uiState.update { it.copy(nameDialog = null, busy = true) }
        viewModelScope.launch {
            val result = when (dialog.mode) {
                ProfilesUiState.NameDialog.Mode.CREATE -> repo.create(trimmed)
                ProfilesUiState.NameDialog.Mode.RENAME -> repo.rename(dialog.source, trimmed)
                ProfilesUiState.NameDialog.Mode.DUPLICATE -> repo.duplicate(dialog.source, trimmed)
            }
            _uiState.update {
                it.copy(
                    busy = false,
                    message = if (result is AppResult.Success) trimmed else (result as AppResult.Failure).message,
                )
            }
            refresh()
        }
    }

    // ---- 切换 ----

    fun requestSwitch(name: String) = _uiState.update { it.copy(switchTarget = name) }

    fun cancelSwitch() = _uiState.update { it.copy(switchTarget = null) }

    fun confirmSwitch() {
        val target = _uiState.value.switchTarget ?: return
        _uiState.update { it.copy(switchTarget = null, busy = true) }
        viewModelScope.launch {
            val result = repo.switch(target)
            _uiState.update {
                it.copy(
                    busy = false,
                    message = if (result is AppResult.Success) target else (result as AppResult.Failure).message,
                )
            }
            refresh()
        }
    }

    // ---- 导出 / 导入 ----

    fun export(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            when (val result = repo.export(name)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(busy = false, pendingExport = ProfilesUiState.PendingExport(name, result.value))
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(busy = false, message = result.message)
                }
            }
        }
    }

    /** SAF 目标已选定：把 bundle 写入 Uri。 */
    fun writePendingExport(uri: android.net.Uri, contentResolver: android.content.ContentResolver) {
        val pending = _uiState.value.pendingExport ?: return
        viewModelScope.launch {
            val ok = runCatching {
                contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(pending.bundle.toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("cannot open output stream")
            }.isSuccess
            _uiState.update {
                it.copy(pendingExport = null, message = if (ok) pending.name else "export failed")
            }
        }
    }

    fun cancelPendingExport() = _uiState.update { it.copy(pendingExport = null) }

    /** 选中的导入文件读不出来（权限/编码问题）。 */
    fun importFailed() = _uiState.update { it.copy(message = "import failed") }

    fun import(content: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            when (val result = repo.import(content)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(busy = false, message = result.value.name.ifEmpty { "imported" })
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(busy = false, message = result.message)
                }
            }
            refresh()
        }
    }
}

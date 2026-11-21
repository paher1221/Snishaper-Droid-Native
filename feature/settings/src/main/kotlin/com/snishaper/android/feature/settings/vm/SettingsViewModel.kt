package com.snishaper.android.feature.settings.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.data.DataGraph
import com.snishaper.android.core.model.IpPool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val pools: List<IpPool> = emptyList(),
    val loaded: Boolean = false,
    val newPoolName: String = "",
    val newPoolIps: String = "",
    val adding: Boolean = false,
    /** 正在执行操作的池名集合。 */
    val busy: Set<String> = emptySet(),
    /** 池操作结果（池名 → 消息）。 */
    val poolMessages: Map<String, String> = emptyMap(),
    val addMessage: String? = null,
)

/** 设置页：IP 池管理（与旧版 IPPoolsSection 一致；cloudflare 为保留名）。 */
class SettingsViewModel : ViewModel() {

    companion object {
        private const val RESERVED_POOL = "cloudflare"
    }

    private val repo = DataGraph.ipPoolRepository

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = repo.pools()) {
                is AppResult.Success -> _uiState.update { it.copy(pools = result.value, loaded = true) }
                is AppResult.Failure -> _uiState.update { it.copy(loaded = true) }
            }
        }
    }

    fun newPoolNameChanged(value: String) {
        _uiState.update { it.copy(newPoolName = value, addMessage = null) }
    }

    fun newPoolIpsChanged(value: String) {
        _uiState.update { it.copy(newPoolIps = value, addMessage = null) }
    }

    fun addPool() {
        val state = _uiState.value
        val name = state.newPoolName.trim().lowercase()
        val ips = state.newPoolIps.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val error = when {
            name.isBlank() || name == RESERVED_POOL -> "invalid_name"
            ips.isEmpty() -> "no_ips"
            state.pools.any { it.name == name } -> "duplicate_name"
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(addMessage = error) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(adding = true) }
            val result = repo.update(state.pools + IpPool(name = name, preferredIps = ips, autoUpdate = true))
            _uiState.update {
                if (result is AppResult.Success) {
                    it.copy(adding = false, newPoolName = "", newPoolIps = "")
                } else {
                    it.copy(adding = false, addMessage = (result as AppResult.Failure).message)
                }
            }
            refresh()
        }
    }

    fun removePool(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = it.busy + name) }
            val result = repo.update(_uiState.value.pools.filterNot { it.name == name })
            _uiState.update {
                it.copy(
                    busy = it.busy - name,
                    poolMessages = it.poolMessages + (name to if (result is AppResult.Success) "removed" else (result as AppResult.Failure).message),
                )
            }
            refresh()
        }
    }

    fun forceFetch(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = it.busy + name) }
            val result = repo.forceFetch(name)
            _uiState.update {
                it.copy(
                    busy = it.busy - name,
                    poolMessages = it.poolMessages + (name to if (result is AppResult.Success) result.value else (result as AppResult.Failure).message),
                )
            }
            refresh()
        }
    }

    fun healthCheck(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = it.busy + name) }
            repo.healthCheck(name)
            _uiState.update {
                it.copy(busy = it.busy - name, poolMessages = it.poolMessages + (name to "health check started"))
            }
        }
    }

    fun removeInvalid(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = it.busy + name) }
            val result = repo.removeInvalid(name)
            val msg = when (result) {
                is AppResult.Success -> result.value.toString()
                is AppResult.Failure -> result.message
            }
            _uiState.update {
                it.copy(busy = it.busy - name, poolMessages = it.poolMessages + (name to msg))
            }
            refresh()
        }
    }
}

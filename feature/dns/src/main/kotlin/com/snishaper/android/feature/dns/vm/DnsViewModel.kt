package com.snishaper.android.feature.dns.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.data.DataGraph
import com.snishaper.android.core.model.DnsNode
import com.snishaper.android.core.model.DnsTestResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DnsUiState(
    val nodes: List<DnsNode> = emptyList(),
    val loaded: Boolean = false,
    /** 每个节点的测试结果（id → 结果）。 */
    val testResults: Map<String, DnsTestResult> = emptyMap(),
    val testingIds: Set<String> = emptySet(),
    /** 操作结果提示（null = 无）。 */
    val message: String? = null,
)

/** DNS 页：节点列表 + 增删改 + 优先级排序 + 连通性测试（与旧版一致）。 */
class DnsViewModel : ViewModel() {

    private val repo = DataGraph.dnsRepository

    private val _uiState = MutableStateFlow(DnsUiState())
    val uiState: StateFlow<DnsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = repo.nodes()) {
                is AppResult.Success -> _uiState.update { it.copy(nodes = result.value, loaded = true) }
                is AppResult.Failure -> _uiState.update { it.copy(loaded = true) }
            }
        }
    }

    fun save(node: DnsNode, isNew: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            val result = if (isNew) repo.add(node) else repo.update(node)
            if (result is AppResult.Failure) _uiState.update { it.copy(message = result.message) }
            refresh()
            onDone()
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repo.delete(id)
            refresh()
        }
    }

    fun move(id: String, fromIndex: Int, toIndex: Int) {
        if (toIndex !in _uiState.value.nodes.indices) return
        viewModelScope.launch {
            repo.setPriority(id, toIndex)
            refresh()
        }
    }

    fun test(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(testingIds = it.testingIds + id) }
            val result = repo.test(id)
            _uiState.update { state ->
                val entry = when (result) {
                    is AppResult.Success -> result.value
                    is AppResult.Failure -> DnsTestResult(success = false, error = result.message)
                }
                state.copy(testResults = state.testResults + (id to entry), testingIds = state.testingIds - id)
            }
        }
    }

    fun testAll() {
        _uiState.value.nodes.forEach { test(it.id) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

package com.snishaper.android.feature.evolution.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.data.DataGraph
import com.snishaper.android.core.model.EvolutionResult
import com.snishaper.android.core.model.EvolutionTempRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EvolutionUiState(
    val domainsText: String = "",
    val enableIpv6: Boolean = false,
    val ipv6Available: Boolean = true,
    val running: Boolean = false,
    val starting: Boolean = false,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val results: List<EvolutionResult> = emptyList(),
    val tempRules: List<EvolutionTempRule> = emptyList(),
    /** 待确认应用的规则（弹确认框）。 */
    val pendingRule: EvolutionTempRule? = null,
    val message: String? = null,
    val activeTab: Int = 0,
)

/** 演进模式：域名连通性探测 + 临时规则应用（对齐旧版 Evolution.tsx）。 */
class EvolutionViewModel : ViewModel() {

    private val repo = DataGraph.evolutionRepository
    private val settingsRepo = DataGraph.settingsRepository

    private val _uiState = MutableStateFlow(EvolutionUiState())
    val uiState: StateFlow<EvolutionUiState> = _uiState.asStateFlow()

    init {
        loadIpv6()
        restoreStatus()
        // 引擎每 500ms 推一次任务快照；解析后同步进度 / 结果 / 临时规则
        viewModelScope.launch {
            repo.task.collect { raw ->
                val task = com.snishaper.android.core.model.EvolutionTask.fromJson(raw) ?: return@collect
                _uiState.update {
                    it.copy(
                        running = task.isRunning,
                        starting = false,
                        progressCurrent = task.progress,
                        progressTotal = task.total,
                        results = task.results,
                        tempRules = task.tempRules,
                    )
                }
            }
        }
    }

    private fun loadIpv6() {
        viewModelScope.launch {
            when (val r = settingsRepo.ipv6Available()) {
                is AppResult.Success -> _uiState.update { it.copy(ipv6Available = r.value) }
                is AppResult.Failure -> Unit
            }
        }
    }

    /** 冷启动时拉一次状态，恢复进行中/已完成任务的界面。 */
    private fun restoreStatus() {
        viewModelScope.launch {
            when (val r = repo.currentStatus()) {
                is AppResult.Success -> {
                    val task = r.value
                    if (task.id.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                running = task.isRunning,
                                progressCurrent = task.progress,
                                progressTotal = task.total,
                                results = task.results,
                                tempRules = task.tempRules,
                            )
                        }
                    }
                }
                is AppResult.Failure -> Unit
            }
        }
    }

    fun domainsChanged(value: String) {
        _uiState.update { it.copy(domainsText = value, message = null) }
    }

    fun enableIpv6Changed(enabled: Boolean) {
        _uiState.update { it.copy(enableIpv6 = enabled) }
    }

    fun tabChanged(index: Int) {
        _uiState.update { it.copy(activeTab = index) }
    }

    fun startTest() {
        val domains = _uiState.value.domainsText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (domains.isEmpty()) {
            _uiState.update { it.copy(message = "empty_domains") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(starting = true, message = null) }
            when (val r = repo.start(domains, _uiState.value.enableIpv6)) {
                is AppResult.Success -> _uiState.update { it.copy(starting = false) }
                is AppResult.Failure -> _uiState.update {
                    it.copy(starting = false, running = false, message = r.message)
                }
            }
        }
    }

    fun stopTest() {
        viewModelScope.launch {
            repo.stop()
            _uiState.update { it.copy(running = false) }
        }
    }

    fun requestApplyRule(rule: EvolutionTempRule) {
        _uiState.update { it.copy(pendingRule = rule) }
    }

    fun cancelApplyRule() {
        _uiState.update { it.copy(pendingRule = null) }
    }

    fun confirmApplyRule() {
        val rule = _uiState.value.pendingRule ?: return
        viewModelScope.launch {
            when (val r = repo.applyRule(rule.id)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        pendingRule = null,
                        tempRules = it.tempRules.filterNot { item -> item.id == rule.id },
                        message = "applied",
                    )
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(pendingRule = null, message = r.message)
                }
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
}

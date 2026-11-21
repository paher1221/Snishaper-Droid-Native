package com.snishaper.android.feature.rules.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.data.DataGraph
import com.snishaper.android.core.model.CertVerify
import com.snishaper.android.core.model.RulesLayerInfo
import com.snishaper.android.core.model.SiteGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RulesUiState(
    val groups: List<SiteGroup> = emptyList(),
    val layerInfo: RulesLayerInfo = RulesLayerInfo(),
    val loaded: Boolean = false,
    /** 非空时显示编辑/新建表单。 */
    val editing: SiteGroup? = null,
    /** 非空时显示删除确认。 */
    val deleting: SiteGroup? = null,
    /** 非空时显示回退确认。 */
    val reverting: SiteGroup? = null,
    /** 非空时显示规则解释结果（原始 JSON）。 */
    val explainResult: String? = null,
    val explainOpen: Boolean = false,
)

/** 规则页：站点分组列表 + 编辑/删除/回退 + 匹配解释。 */
class RulesViewModel : ViewModel() {

    private val repo = DataGraph.rulesRepository

    private val _uiState = MutableStateFlow(RulesUiState())
    val uiState: StateFlow<RulesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val groups = repo.siteGroups()
            val layers = repo.layerInfo()
            _uiState.update {
                it.copy(
                    groups = (groups as? AppResult.Success)?.value ?: it.groups,
                    layerInfo = (layers as? AppResult.Success)?.value ?: it.layerInfo,
                    loaded = true,
                )
            }
        }
    }

    fun layerOf(id: String): String = _uiState.value.layerInfo.siteGroupLayers[id] ?: ""

    fun startCreate() = _uiState.update { it.copy(editing = SiteGroup()) }

    fun startEdit(group: SiteGroup) = _uiState.update {
        it.copy(editing = group.copy(upstream = group.upstream.trim().uppercase().let { u -> if (u == "DIRECT") "" else u }))
    }

    fun cancelEdit() = _uiState.update { it.copy(editing = null) }

    fun save(group: SiteGroup) {
        viewModelScope.launch {
            val normalized = group.copy(
                upstream = group.upstream.trim().ifEmpty { "DIRECT" },
            )
            val result = if (normalized.id.isBlank()) repo.add(normalized) else repo.update(normalized)
            if (result is AppResult.Failure) {
                _uiState.update { it.copy(editing = null) }
                return@launch
            }
            _uiState.update { it.copy(editing = null) }
            refresh()
        }
    }

    fun requestDelete(group: SiteGroup) = _uiState.update { it.copy(deleting = group) }
    fun cancelDelete() = _uiState.update { it.copy(deleting = null) }

    fun confirmDelete() {
        val target = _uiState.value.deleting ?: return
        viewModelScope.launch {
            repo.delete(target.id)
            _uiState.update { it.copy(deleting = null) }
            refresh()
        }
    }

    fun requestRevert(group: SiteGroup) = _uiState.update { it.copy(reverting = group) }
    fun cancelRevert() = _uiState.update { it.copy(reverting = null) }

    fun confirmRevert() {
        val target = _uiState.value.reverting ?: return
        viewModelScope.launch {
            repo.revertToUpstream(target.id)
            _uiState.update { it.copy(reverting = null) }
            refresh()
        }
    }

    fun openExplain() = _uiState.update { it.copy(explainOpen = true, explainResult = null) }
    fun closeExplain() = _uiState.update { it.copy(explainOpen = false, explainResult = null) }

    fun explain(host: String, ip: String) {
        viewModelScope.launch {
            when (val result = repo.explainMatch(host.trim(), ip.trim())) {
                is AppResult.Success -> _uiState.update { it.copy(explainResult = result.value) }
                is AppResult.Failure -> _uiState.update { it.copy(explainResult = result.message) }
            }
        }
    }
}

package com.snishaper.android.feature.routing.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.data.DataGraph
import com.snishaper.android.core.model.AutoRoutingStatus
import com.snishaper.android.core.model.RouteEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val MAX_FLOWS = 40

data class RoutingUiState(
    val status: AutoRoutingStatus = AutoRoutingStatus(),
    val flows: List<RouteEvent> = emptyList(),
)

/** 分流页：只读状态（15s 轮询）+ 实时命中流（引擎推送，保留最近 40 条）。 */
class RoutingViewModel : ViewModel() {

    private val repo = DataGraph.routingRepository

    private val _uiState = MutableStateFlow(RoutingUiState())
    val uiState: StateFlow<RoutingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.routeEvents.collect { event ->
                _uiState.update { it.copy(flows = (listOf(event) + it.flows).take(MAX_FLOWS)) }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(15_000)
            }
        }
    }

    suspend fun refresh() {
        when (val result = repo.status()) {
            is AppResult.Success -> _uiState.update { it.copy(status = result.value) }
            is AppResult.Failure -> Unit
        }
    }
}

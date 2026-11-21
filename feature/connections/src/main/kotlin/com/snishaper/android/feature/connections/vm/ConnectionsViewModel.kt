package com.snishaper.android.feature.connections.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.data.DataGraph
import com.snishaper.android.core.model.ConnectionInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ConnectionsUiState(
    val connections: List<ConnectionInfo> = emptyList(),
    val loaded: Boolean = false,
)

/** 连接页：活动连接列表，2.5s 轮询（与旧版一致）。 */
class ConnectionsViewModel : ViewModel() {

    private val repo = DataGraph.connectionsRepository

    private val _uiState = MutableStateFlow(ConnectionsUiState())
    val uiState: StateFlow<ConnectionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(2_500)
            }
        }
    }

    suspend fun refresh() {
        when (val result = repo.activeConnections()) {
            is AppResult.Success -> _uiState.update { it.copy(connections = result.value, loaded = true) }
            is AppResult.Failure -> _uiState.update { it.copy(loaded = true) }
        }
    }
}

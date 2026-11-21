package com.snishaper.android.feature.dashboard.vm

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.data.DataGraph
import com.snishaper.android.core.data.TunStartResult
import com.snishaper.android.core.model.ProxyStats
import com.snishaper.android.core.model.TunStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** TUN 操作的一次性反馈。 */
sealed interface TunOpEvent {
    data class Error(val message: String) : TunOpEvent
    data class Info(val message: String) : TunOpEvent
    /** 系统要求 VPN 授权：UI 用返回的 Intent 拉起授权弹窗，成功后回调 retryTunStart。 */
    data class ConsentNeeded(val consentIntent: Intent) : TunOpEvent
}

data class DashboardUiState(
    val proxyRunning: Boolean = false,
    /** 引擎上报的模式；空串显示为 MITM（与旧版 Dashboard 一致）。 */
    val proxyMode: String = "",
    val listenPort: Int = 8080,
    val stats: ProxyStats = ProxyStats(),
    val tunStatus: TunStatus = TunStatus(),
    val isProxyBusy: Boolean = false,
    val isTunBusy: Boolean = false,
)

/**
 * Dashboard 最小闭环：状态实时刷新（引擎推送 + 5s 轮询兜底）、
 * 代理启停、TUN 开关（含 VPN 授权流程）。
 */
class DashboardViewModel : ViewModel() {

    private val proxyRepo = DataGraph.proxyRepository
    private val tunRepo = DataGraph.tunRepository

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _tunEvents = MutableStateFlow<TunOpEvent?>(null)
    val tunEvents: StateFlow<TunOpEvent?> = _tunEvents.asStateFlow()

    /** 授权弹窗回来后要重试的启动动作（TUN 启动被 consent 打断时暂存）。 */
    private var pendingTunStart: Boolean = false

    init {
        // 引擎状态推送（启停、TUN 变化都会触发）
        viewModelScope.launch {
            DataGraph.engineStatus.collect { status ->
                _uiState.update {
                    it.copy(
                        proxyRunning = status.proxyRunning,
                        proxyMode = status.proxyMode,
                        listenPort = status.listenPort,
                        tunStatus = it.tunStatus.copy(
                            running = status.tunRunning,
                            supported = status.tunSupported,
                            message = status.tunMessage,
                        ),
                    )
                }
            }
        }
        // 轮询兜底：stats / tunStatus 没有推送通道，5s 刷新一次
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(5_000)
            }
        }
    }

    fun consumeTunEvent() { _tunEvents.value = null }

    suspend fun refresh() {
        val stats = proxyRepo.stats()
        val tun = tunRepo.status()
        _uiState.update {
            it.copy(
                stats = (stats as? AppResult.Success)?.value ?: it.stats,
                tunStatus = (tun as? AppResult.Success)?.value ?: it.tunStatus,
            )
        }
    }

    fun toggleTun() {
        if (_uiState.value.isTunBusy) return
        _uiState.update { it.copy(isTunBusy = true) }
        viewModelScope.launch {
            if (_uiState.value.tunStatus.running) {
                // 一键关：TUN 与代理一起停
                when (val result = tunRepo.stop()) {
                    is AppResult.Failure -> _tunEvents.value = TunOpEvent.Error(result.message)
                    is AppResult.Success -> {
                        proxyRepo.stop()
                        _tunEvents.value = TunOpEvent.Info("tun_stopped")
                    }
                }
            } else {
                // 一键开：TUN 依赖本地代理监听（引擎 StartTUN 会拒绝），StartProxy 幂等
                when (val p = proxyRepo.start()) {
                    is AppResult.Failure -> {
                        _tunEvents.value = TunOpEvent.Error(p.message)
                        refresh()
                        _uiState.update { it.copy(isTunBusy = false) }
                        return@launch
                    }
                    is AppResult.Success -> Unit
                }
                when (val result = tunRepo.start()) {
                    is AppResult.Failure -> _tunEvents.value = TunOpEvent.Error(result.message)
                    is AppResult.Success -> when (val value = result.value) {
                        is TunStartResult.Started -> _tunEvents.value = TunOpEvent.Info("tun_starting")
                        is TunStartResult.ConsentNeeded -> {
                            pendingTunStart = true
                            _tunEvents.value = TunOpEvent.ConsentNeeded(value.consentIntent)
                        }
                    }
                }
            }
            refresh()
            _uiState.update { it.copy(isTunBusy = false) }
        }
    }

    /** VPN 授权弹窗返回 RESULT_OK 后由 UI 调用，重试启动。 */
    fun retryTunStart() {
        if (!pendingTunStart) return
        pendingTunStart = false
        _uiState.update { it.copy(isTunBusy = true) }
        viewModelScope.launch {
            when (val result = tunRepo.start()) {
                is AppResult.Failure -> _tunEvents.value = TunOpEvent.Error(result.message)
                is AppResult.Success -> _tunEvents.value = TunOpEvent.Info("tun_starting")
            }
            refresh()
            _uiState.update { it.copy(isTunBusy = false) }
        }
    }

    /** 授权被取消：清掉暂存的重试标记。 */
    fun cancelTunConsent() {
        pendingTunStart = false
    }
}

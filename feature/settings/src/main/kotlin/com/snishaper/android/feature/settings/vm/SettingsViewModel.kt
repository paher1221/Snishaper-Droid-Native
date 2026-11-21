package com.snishaper.android.feature.settings.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.data.DataGraph
import com.snishaper.android.core.design.theme.AppearanceController
import com.snishaper.android.core.design.theme.AppearanceMode
import com.snishaper.android.core.model.CaInstallStatus
import com.snishaper.android.core.model.CloudflareIpStat
import com.snishaper.android.core.model.InstalledCert
import com.snishaper.android.core.model.IpPool
import com.snishaper.android.core.model.TunStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SettingsUiState(
    // ---- IP 池（M3 已落） ----
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

    // ---- 通用 ----
    val port: String = "",
    val language: String = "",
    val appearance: AppearanceMode = AppearanceMode.SYSTEM,
    val autoUpdateRules: Boolean = false,
    val generalMessage: String? = null,

    // ---- TUN ----
    val mtu: String = "1500",
    val tunStatus: TunStatus? = null,

    // ---- 证书 ----
    val caStatus: CaInstallStatus? = null,
    val installedCerts: List<InstalledCert> = emptyList(),
    val certBusy: Boolean = false,
    val certMessage: String? = null,
    /** 已取出的证书 PEM，等待 SAF 目标 Uri 落盘。 */
    val pendingCertPem: String? = null,

    // ---- IPv6 ----
    val ipv6Available: Boolean = true,
    val ipv6Checking: Boolean = false,

    // ---- Cloudflare 池 ----
    val ipStats: List<CloudflareIpStat> = emptyList(),
    val cfRefreshing: Boolean = false,
    val cfChecking: Boolean = false,

    // ---- 配置导入导出 ----
    val transferBusy: Boolean = false,
    val transferMessage: String? = null,
    /** 已取出的配置 bundle，等待 SAF 目标 Uri 落盘。 */
    val pendingConfigExport: String? = null,
)

/** 设置页：IP 池（M3）+ 通用 / TUN / 证书 / IPv6 / CF 池 / 配置导入导出（M4）。 */
class SettingsViewModel : ViewModel() {

    companion object {
        private const val RESERVED_POOL = "cloudflare"
        private const val CF_POLL_MS = 5000L
    }

    private val ipPoolRepo = DataGraph.ipPoolRepository
    private val settingsRepo = DataGraph.settingsRepository
    private val certRepo = DataGraph.certRepository
    private val transferRepo = DataGraph.configTransferRepository

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshPools()
        loadGeneral()
        loadTun()
        loadCert()
        loadIpv6()
        refreshCfStats()
        // 与旧版一致：进设置页触发一次 CF 体检，并每 5 秒轮询延迟列表
        viewModelScope.launch {
            settingsRepo.triggerCFHealthCheck()
            while (isActive) {
                delay(CF_POLL_MS)
                refreshCfStats()
            }
        }
    }

    // ==================================================================
    // IP 池（M3）
    // ==================================================================

    fun refreshPools() {
        viewModelScope.launch {
            when (val result = ipPoolRepo.pools()) {
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
            val result = ipPoolRepo.update(state.pools + IpPool(name = name, preferredIps = ips, autoUpdate = true))
            _uiState.update {
                if (result is AppResult.Success) {
                    it.copy(adding = false, newPoolName = "", newPoolIps = "")
                } else {
                    it.copy(adding = false, addMessage = (result as AppResult.Failure).message)
                }
            }
            refreshPools()
        }
    }

    fun removePool(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = it.busy + name) }
            val result = ipPoolRepo.update(_uiState.value.pools.filterNot { it.name == name })
            _uiState.update {
                it.copy(
                    busy = it.busy - name,
                    poolMessages = it.poolMessages + (name to if (result is AppResult.Success) "removed" else (result as AppResult.Failure).message),
                )
            }
            refreshPools()
        }
    }

    fun forceFetch(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = it.busy + name) }
            val result = ipPoolRepo.forceFetch(name)
            _uiState.update {
                it.copy(
                    busy = it.busy - name,
                    poolMessages = it.poolMessages + (name to if (result is AppResult.Success) result.value else (result as AppResult.Failure).message),
                )
            }
            refreshPools()
        }
    }

    fun healthCheck(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = it.busy + name) }
            ipPoolRepo.healthCheck(name)
            _uiState.update {
                it.copy(busy = it.busy - name, poolMessages = it.poolMessages + (name to "health check started"))
            }
        }
    }

    fun removeInvalid(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(busy = it.busy + name) }
            val result = ipPoolRepo.removeInvalid(name)
            val msg = when (result) {
                is AppResult.Success -> result.value.toString()
                is AppResult.Failure -> result.message
            }
            _uiState.update {
                it.copy(busy = it.busy - name, poolMessages = it.poolMessages + (name to msg))
            }
            refreshPools()
        }
    }

    // ==================================================================
    // 通用（M4）：端口 / 语言 / 外观 / 自动更新规则
    // ==================================================================

    private fun loadGeneral() {
        viewModelScope.launch {
            val port = when (val r = settingsRepo.listenPort()) {
                is AppResult.Success -> r.value
                is AppResult.Failure -> 0
            }
            val language = when (val r = settingsRepo.language()) {
                is AppResult.Success -> r.value
                is AppResult.Failure -> ""
            }
            val appearance = when (val r = settingsRepo.appearanceMode()) {
                is AppResult.Success -> AppearanceMode.entries.firstOrNull {
                    it.name.equals(r.value, ignoreCase = true)
                } ?: AppearanceMode.SYSTEM
                is AppResult.Failure -> AppearanceMode.SYSTEM
            }
            val autoUpdate = when (val r = settingsRepo.autoUpdateRules()) {
                is AppResult.Success -> r.value
                is AppResult.Failure -> false
            }
            _uiState.update {
                it.copy(
                    port = if (port > 0) port.toString() else "",
                    language = language,
                    appearance = appearance,
                    autoUpdateRules = autoUpdate,
                )
            }
            // 引擎里的外观模式同步到全局控制器（设置页是唯一写入口）
            AppearanceController.set(appearance)
        }
    }

    fun portChanged(value: String) {
        _uiState.update { it.copy(port = value.filter { c -> c.isDigit() }.take(5), generalMessage = null) }
    }

    fun applyPort() {
        val parsed = _uiState.value.port.toIntOrNull()
        if (parsed == null || parsed !in 1..65535) {
            _uiState.update { it.copy(generalMessage = "port_invalid") }
            return
        }
        viewModelScope.launch {
            val result = settingsRepo.setListenPort(parsed)
            _uiState.update {
                it.copy(generalMessage = if (result is AppResult.Success) "saved" else (result as AppResult.Failure).message)
            }
        }
    }

    fun languageChanged(lang: String) {
        _uiState.update { it.copy(language = lang) }
        viewModelScope.launch {
            val result = settingsRepo.setLanguage(lang)
            if (result is AppResult.Failure) {
                _uiState.update { it.copy(generalMessage = result.message) }
            }
        }
    }

    fun appearanceChanged(mode: AppearanceMode) {
        _uiState.update { it.copy(appearance = mode) }
        AppearanceController.set(mode)
        viewModelScope.launch {
            val result = settingsRepo.setAppearanceMode(mode.name.lowercase())
            if (result is AppResult.Failure) {
                _uiState.update { it.copy(generalMessage = result.message) }
            }
        }
    }

    fun autoUpdateRulesChanged(enabled: Boolean) {
        _uiState.update { it.copy(autoUpdateRules = enabled) }
        viewModelScope.launch {
            val result = settingsRepo.setAutoUpdateRules(enabled)
            if (result is AppResult.Failure) {
                _uiState.update { it.copy(autoUpdateRules = !enabled, generalMessage = result.message) }
            }
        }
    }

    // ==================================================================
    // TUN（M4）：MTU + 状态
    // ==================================================================

    private fun loadTun() {
        viewModelScope.launch {
            val config = when (val r = settingsRepo.tunConfig()) {
                is AppResult.Success -> r.value
                is AppResult.Failure -> return@launch
            }
            _uiState.update { it.copy(mtu = config.mtu.toString()) }
        }
        viewModelScope.launch {
            when (val r = DataGraph.tunRepository.status()) {
                is AppResult.Success -> _uiState.update { it.copy(tunStatus = r.value) }
                is AppResult.Failure -> Unit
            }
        }
    }

    fun mtuChanged(value: String) {
        _uiState.update { it.copy(mtu = value.filter { c -> c.isDigit() }.take(4)) }
    }

    fun applyMtu() {
        val parsed = _uiState.value.mtu.toIntOrNull()
        if (parsed == null || parsed !in 576..9000) {
            _uiState.update { it.copy(generalMessage = "mtu_invalid") }
            return
        }
        viewModelScope.launch {
            val result = settingsRepo.updateTunConfig(com.snishaper.android.core.model.TunConfig(mtu = parsed))
            _uiState.update {
                it.copy(generalMessage = if (result is AppResult.Success) "saved" else (result as AppResult.Failure).message)
            }
        }
    }

    // ==================================================================
    // 证书（M4）
    // ==================================================================

    private fun loadCert() {
        viewModelScope.launch {
            val status = when (val r = certRepo.installStatus()) {
                is AppResult.Success -> r.value
                is AppResult.Failure -> return@launch
            }
            _uiState.update { it.copy(caStatus = status) }
        }
        viewModelScope.launch {
            when (val r = certRepo.installedCerts()) {
                is AppResult.Success -> _uiState.update { it.copy(installedCerts = r.value) }
                is AppResult.Failure -> Unit
            }
        }
    }

    fun regenerateCert() {
        viewModelScope.launch {
            _uiState.update { it.copy(certBusy = true) }
            val result = certRepo.regenerate()
            _uiState.update {
                it.copy(
                    certBusy = false,
                    certMessage = if (result is AppResult.Success) "reset_ok" else (result as AppResult.Failure).message,
                )
            }
            loadCert()
        }
    }

    fun exportCert() {
        viewModelScope.launch {
            _uiState.update { it.copy(certBusy = true) }
            when (val result = certRepo.exportPEM()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(certBusy = false, pendingCertPem = result.value.takeIf { pem -> pem.isNotEmpty() })
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(certBusy = false, certMessage = result.message)
                }
            }
        }
    }

    /** SAF 目标已选定：把 PEM 写入 Uri。 */
    fun writePendingCert(uri: android.net.Uri, contentResolver: android.content.ContentResolver) {
        val pem = _uiState.value.pendingCertPem ?: return
        viewModelScope.launch {
            val ok = runCatching {
                contentResolver.openOutputStream(uri)?.use { it.write(pem.toByteArray(Charsets.UTF_8)) }
                    ?: throw IllegalStateException("cannot open output stream")
            }.isSuccess
            _uiState.update {
                it.copy(pendingCertPem = null, certMessage = if (ok) "export_ok" else "export_failed")
            }
        }
    }

    fun cancelPendingCert() = _uiState.update { it.copy(pendingCertPem = null) }

    fun uninstallCert(token: String) {
        if (token.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(certBusy = true) }
            val result = certRepo.uninstall(token)
            _uiState.update {
                it.copy(
                    certBusy = false,
                    certMessage = if (result is AppResult.Success) "removed" else (result as AppResult.Failure).message,
                )
            }
            loadCert()
        }
    }

    // ==================================================================
    // IPv6（M4）
    // ==================================================================

    private fun loadIpv6() {
        viewModelScope.launch {
            when (val r = settingsRepo.ipv6Available()) {
                is AppResult.Success -> _uiState.update { it.copy(ipv6Available = r.value) }
                is AppResult.Failure -> Unit
            }
        }
    }

    fun refreshIpv6() {
        viewModelScope.launch {
            _uiState.update { it.copy(ipv6Checking = true) }
            when (val r = settingsRepo.refreshIPv6()) {
                is AppResult.Success -> _uiState.update { it.copy(ipv6Checking = false, ipv6Available = r.value) }
                is AppResult.Failure -> _uiState.update { it.copy(ipv6Checking = false) }
            }
        }
    }

    // ==================================================================
    // Cloudflare 池（M4）
    // ==================================================================

    private fun refreshCfStats() {
        viewModelScope.launch {
            when (val r = settingsRepo.cloudflareIpStats()) {
                is AppResult.Success -> _uiState.update { it.copy(ipStats = r.value) }
                is AppResult.Failure -> Unit
            }
        }
    }

    fun fetchCfIPs() {
        viewModelScope.launch {
            _uiState.update { it.copy(cfRefreshing = true) }
            settingsRepo.forceFetchCloudflareIPs()
            _uiState.update { it.copy(cfRefreshing = false) }
            refreshCfStats()
        }
    }

    fun cfHealthCheck() {
        viewModelScope.launch {
            _uiState.update { it.copy(cfChecking = true) }
            settingsRepo.triggerCFHealthCheck()
            delay(1200)
            refreshCfStats()
            _uiState.update { it.copy(cfChecking = false) }
        }
    }

    // ==================================================================
    // 配置导入导出（M4）
    // ==================================================================

    fun exportConfig() {
        viewModelScope.launch {
            _uiState.update { it.copy(transferBusy = true) }
            when (val result = transferRepo.exportConfig()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(transferBusy = false, pendingConfigExport = result.value.takeIf { s -> s.isNotEmpty() })
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(transferBusy = false, transferMessage = result.message)
                }
            }
        }
    }

    /** SAF 目标已选定：把配置 bundle 写入 Uri。 */
    fun writePendingConfig(uri: android.net.Uri, contentResolver: android.content.ContentResolver) {
        val bundle = _uiState.value.pendingConfigExport ?: return
        viewModelScope.launch {
            val ok = runCatching {
                contentResolver.openOutputStream(uri)?.use { it.write(bundle.toByteArray(Charsets.UTF_8)) }
                    ?: throw IllegalStateException("cannot open output stream")
            }.isSuccess
            _uiState.update {
                it.copy(pendingConfigExport = null, transferMessage = if (ok) "export_ok" else "export_failed")
            }
        }
    }

    fun cancelPendingConfig() = _uiState.update { it.copy(pendingConfigExport = null) }

    fun importConfig(content: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(transferBusy = true) }
            when (val result = transferRepo.importConfig(content)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(transferBusy = false, transferMessage = result.value.takeIf { s -> s.isNotEmpty() } ?: "imported")
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(transferBusy = false, transferMessage = result.message)
                }
            }
        }
    }
}

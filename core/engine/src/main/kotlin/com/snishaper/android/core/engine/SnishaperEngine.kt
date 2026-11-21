package com.snishaper.android.core.engine

import android.content.Context
import android.util.Log
import com.snishaper.android.core.model.BuildInfo
import com.snishaper.android.core.model.EngineStatus
import com.snishaper.android.core.model.RouteEvent
import com.snishaper.core.mobile.App
import com.snishaper.core.mobile.Listener
import com.snishaper.core.mobile.Mobile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Process-wide singleton around the gomobile AAR (`com.snishaper.core.mobile.App`).
 *
 * The Go engine carries the full desktop feature surface: start it once
 * (Application.onCreate). All complex payloads are JSON strings passed
 * through to the data layer untouched (唯一解析点在 :core:model).
 */
object SnishaperEngine {

    /** Logcat tag used by smoke tests: `adb logcat -s Snishaper:*`. */
    const val LOG_TAG = "Snishaper"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Guards against double init; reset on failure so a retry is possible. */
    private val initialized = AtomicBoolean(false)

    @Volatile
    private var app: App? = null

    private val _status = MutableStateFlow(EngineStatus())
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    /** Latest evolution task JSON ("" = none); the event pump forwards it raw. */
    private val _evolution = MutableStateFlow("")
    val evolution: StateFlow<String> = _evolution.asStateFlow()

    private val _routes = MutableSharedFlow<RouteEvent>(
        replay = 0,
        extraBufferCapacity = 512,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )
    val routes: SharedFlow<RouteEvent> = _routes.asSharedFlow()

    /** Strong reference: the Go side only holds a proxy of this interface. */
    private val listener = object : Listener {
        override fun onLog(line: String) {
            Log.i(LOG_TAG, line)
        }

        override fun onRouteEvent(domain: String, mode: String) {
            _routes.tryEmit(RouteEvent(domain, mode, System.currentTimeMillis()))
        }

        override fun onStateChanged(stateJSON: String) {
            _status.value = EngineStatus.fromJson(stateJSON)
        }

        override fun onEvolutionProgress(taskJSON: String) {
            _evolution.value = taskJSON
        }
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Starts the engine at most once per successful run. The guard is checked
     * synchronously so Application.onCreate and MainActivity.onCreate racing
     * cannot both spin up an engine (the old async check could).
     */
    fun initialize(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        scope.launch {
            runCatching {
                Log.i(LOG_TAG, "engine: initializing (dataDir=${appContext.filesDir})")
                AssetSeeder.seed(appContext)
                val instance = Mobile.newApp(appContext.filesDir.absolutePath)
                instance.setListener(listener)
                instance.start()
                app = instance
                _status.value = EngineStatus.fromJson(instance.getStatusJSON())
                val coreVersion = BuildInfo.fromJson(instance.buildInfoJSON).coreVersion
                Log.i(LOG_TAG, "engine: ready (core=$coreVersion, port=${_status.value.listenPort})")
            }.onFailure { error ->
                initialized.set(false)
                Log.e(LOG_TAG, "engine: init failed: ${error.message}", error)
                _status.value = _status.value.copy(lastError = error.message ?: "engine init failed")
            }
        }
    }

    private fun requireApp(): App = app ?: throw IllegalStateException("engine is not started yet")

    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }

    /**
     * Raw access to the gomobile `App`. Internal to this module: the data layer
     * must go through the typed passthrough methods below (or new ones added
     * alongside), never through the raw Go binding.
     */
    internal suspend fun <T> withApp(block: (App) -> T): T = io { block(requireApp()) }

    private suspend fun refreshStatus() = io {
        _status.value = EngineStatus.fromJson(requireApp().getStatusJSON())
    }

    // ------------------------------------------------------------------
    // TUN (VpnService)
    // ------------------------------------------------------------------

    suspend fun startTun(fd: Int, mtu: Int = DEFAULT_TUN_MTU) = io {
        requireApp().startTUN(fd, mtu)
        refreshStatus()
    }

    suspend fun stopTun() = io {
        requireApp().stopTUN()
        refreshStatus()
    }

    fun tunMtu(): Int {
        val instance = app ?: return DEFAULT_TUN_MTU
        val raw = runCatching { instance.getTUNConfigJSON() }.getOrNull() ?: return DEFAULT_TUN_MTU
        val value = runCatching { JSONObject(raw).optInt("mtu", DEFAULT_TUN_MTU) }.getOrDefault(DEFAULT_TUN_MTU)
        return if (value in 576..9000) value else DEFAULT_TUN_MTU
    }

    const val DEFAULT_TUN_MTU = 1500

    // ------------------------------------------------------------------
    // 代理生命周期与状态（M1：仓库层的类型化透传；gomobile 的 error 返回
    // 会以异常抛出，由仓库层统一捕获为 AppResult）
    // ------------------------------------------------------------------

    suspend fun startProxy() = io { requireApp().startProxy() }

    suspend fun stopProxy() = io { requireApp().stopProxy() }

    suspend fun isProxyRunning(): Boolean = io { requireApp().isProxyRunning }

    suspend fun proxyMode(): String = io { requireApp().proxyMode }

    suspend fun setListenPort(port: Int) = io { requireApp().setListenPort(port) }

    suspend fun statsJSON(): String = io { requireApp().statsJSON }

    suspend fun tunStatusJSON(): String = io { requireApp().getTUNStatusJSON() }

    suspend fun buildInfo(): BuildInfo = io { BuildInfo.fromJson(requireApp().buildInfoJSON) }

    // ------------------------------------------------------------------
    // M2：连接 / 日志 / 规则 / 分流（仓库层的类型化透传）
    // ------------------------------------------------------------------

    suspend fun activeConnectionsJSON(): String = io { requireApp().activeConnectionsJSON }

    suspend fun recentLogs(lines: Int): String = io { requireApp().getRecentLogs(lines) }

    suspend fun clearLogs() = io { requireApp().clearLogs() }

    suspend fun exportDiagnosticsJSON(): String = io { requireApp().exportDiagnosticsJSON() }

    suspend fun siteGroupsJSON(): String = io { requireApp().siteGroupsJSON }

    suspend fun addSiteGroupJSON(json: String): String = io { requireApp().addSiteGroupJSON(json) }

    suspend fun updateSiteGroupJSON(json: String) = io { requireApp().updateSiteGroupJSON(json) }

    suspend fun deleteSiteGroup(id: String) = io { requireApp().deleteSiteGroup(id) }

    suspend fun rulesLayerInfoJSON(): String = io { requireApp().rulesLayerInfoJSON }

    suspend fun revertSiteGroupToUpstream(id: String) = io { requireApp().revertSiteGroupToUpstream(id) }

    suspend fun explainMatchJSON(host: String, ip: String): String =
        io { requireApp().explainMatchJSON(host, ip) }

    suspend fun autoRoutingStatusJSON(): String = io { requireApp().autoRoutingStatusJSON }

    // ------------------------------------------------------------------
    // M3：DNS / ECH / NAT64 / 迁移服务器 / IP 池（仓库层的类型化透传）
    // ------------------------------------------------------------------

    // DNS 节点
    suspend fun dnsNodesJSON(): String = io { requireApp().getDNSNodesJSON() }

    suspend fun addDnsNodeJSON(json: String): String = io { requireApp().addDNSNodeJSON(json) }

    suspend fun updateDnsNodeJSON(json: String) = io { requireApp().updateDNSNodeJSON(json) }

    suspend fun deleteDnsNode(id: String) = io { requireApp().deleteDNSNode(id) }

    suspend fun setDnsNodePriority(id: String, index: Int) = io { requireApp().setDNSNodePriority(id, index) }

    suspend fun testDnsNodeJSON(id: String): String = io { requireApp().testDNSNode(id) }

    // ECH 档案
    suspend fun echProfilesJSON(): String = io { requireApp().getECHProfilesJSON() }

    suspend fun upsertEchProfileJSON(json: String) = io { requireApp().upsertECHProfileJSON(json) }

    suspend fun deleteEchProfile(id: String) = io { requireApp().deleteECHProfile(id) }

    suspend fun fetchEchConfigJSON(domain: String, dohUpstream: String): String =
        io { requireApp().fetchECHConfigJSON(domain, dohUpstream) }

    // NAT64 档案
    suspend fun nat64ProfilesJSON(): String = io { requireApp().getNAT64ProfilesJSON() }

    suspend fun addNat64ProfileJSON(json: String): String = io { requireApp().addNAT64ProfileJSON(json) }

    suspend fun updateNat64ProfileJSON(json: String) = io { requireApp().updateNAT64ProfileJSON(json) }

    suspend fun deleteNat64Profile(id: String) = io { requireApp().deleteNAT64Profile(id) }

    suspend fun testNat64ProfileJSON(prefix: String): String = io { requireApp().testNAT64Profile(prefix) }

    // 迁移服务器（引擎无专用方法，走设置文档的 migration_server 键——与旧桥接一致）
    suspend fun settingsJSON(): String = io { requireApp().getSettingsJSON() }

    suspend fun updateSettingsJSON(json: String) = io { requireApp().updateSettingsJSON(json) }

    suspend fun testMigration(server: String): String = io { requireApp().testMigration(server) }

    suspend fun lastMigrationReportJSON(): String = io { requireApp().lastMigrationReportJSON }

    // IP 池
    suspend fun ipPoolsJSON(): String = io { requireApp().getIPPoolsJSON() }

    suspend fun updateIpPoolsJSON(json: String) = io { requireApp().updateIPPoolsJSON(json) }

    suspend fun ipPoolStatsJSON(name: String): String = io { requireApp().getIPPoolStatsJSON(name) }

    suspend fun forceFetchIpPoolIPsJSON(name: String): String = io { requireApp().forceFetchIPPoolIPsJSON(name) }

    suspend fun refreshIpPoolJSON(name: String) = io { requireApp().refreshIPPoolJSON(name) }

    suspend fun removeInvalidIpPoolIPsJSON(name: String): Int = io { requireApp().removeInvalidIPPoolIPsJSON(name) }

    suspend fun triggerIpPoolHealthCheckJSON(name: String) = io { requireApp().triggerIPPoolHealthCheckJSON(name) }

    // ------------------------------------------------------------------
    // M4：档案 / 证书 / 配置导入导出 / 通用设置（仓库层的类型化透传）
    // ------------------------------------------------------------------

    // 配置档案
    suspend fun profilesJSON(): String = io { requireApp().listProfilesJSON() }

    suspend fun activeProfileJSON(): String = io { requireApp().activeProfileJSON }

    suspend fun createProfileJSON(name: String): String = io { requireApp().createProfileJSON(name) }

    suspend fun renameProfileJSON(oldName: String, newName: String) =
        io { requireApp().renameProfileJSON(oldName, newName) }

    suspend fun duplicateProfileJSON(source: String, newName: String) =
        io { requireApp().duplicateProfileJSON(source, newName) }

    suspend fun switchProfileJSON(name: String) = io { requireApp().switchProfileJSON(name) }

    /** 返回档案 bundle JSON 原文（旧桥接 ExportProfile 保持字符串形状）。 */
    suspend fun exportProfileJSON(name: String): String = io { requireApp().exportProfileJSON(name) }

    suspend fun importProfileJSON(content: String): String = io { requireApp().importProfileJSON(content) }

    // CA 证书
    suspend fun installCA() = io { requireApp().installCA() }

    suspend fun regenerateCert() = io { requireApp().regenerateCert() }

    suspend fun uninstallCert(token: String) = io { requireApp().uninstallCert(token) }

    suspend fun caInstallStatusJSON(): String = io { requireApp().getCAInstallStatusJSON() }

    suspend fun installedCertsJSON(): String = io { requireApp().getInstalledCertsJSON() }

    suspend fun caCertPEM(): String = io { requireApp().getCACertPEM() }

    suspend fun exportCert(): String = io { requireApp().exportCert() }

    // 配置导入导出（ExportConfig / ExportDiagnostics 返回原文，由调用方落盘）
    suspend fun exportConfigJSON(): String = io { requireApp().exportConfigJSON() }

    suspend fun importConfigWithSummaryJSON(content: String): String =
        io { requireApp().importConfigWithSummaryJSON(content) }

    // 通用设置
    suspend fun checkPortAvailable(port: Int): Boolean = io { requireApp().checkPortAvailable(port) }

    suspend fun tunConfigJSON(): String = io { requireApp().getTUNConfigJSON() }

    suspend fun updateTunConfigJSON(json: String) = io { requireApp().updateTUNConfigJSON(json) }

    suspend fun ipv6Available(): Boolean = io { requireApp().getIPv6Available() }

    suspend fun refreshIPv6Check(): Boolean = io { requireApp().refreshIPv6Check() }

    // Cloudflare 池（保留名池的独立统计入口）
    suspend fun cloudflareConfigJSON(): String = io { requireApp().getCloudflareConfigJSON() }

    suspend fun cloudflareIpStatsJSON(): String = io { requireApp().getCloudflareIPStatsJSON() }

    suspend fun forceFetchCloudflareIPs() = io { requireApp().forceFetchCloudflareIPs() }

    suspend fun triggerCFHealthCheck() = io { requireApp().triggerCFHealthCheck() }

    suspend fun removeInvalidCFIPs(): Int = io { requireApp().removeInvalidCFIPs() }

    // ------------------------------------------------------------------
    // M5：演进模式（仓库层的类型化透传；进度经 Listener.onEvolutionProgress
    // 推入 [evolution] StateFlow，JSON 原文在 :core:model 解析）
    // ------------------------------------------------------------------

    suspend fun startEvolutionTest(domainsJSON: String, enableIPv6: Boolean) =
        io { requireApp().startEvolutionTest(domainsJSON, enableIPv6) }

    suspend fun stopEvolutionTest() = io { requireApp().stopEvolutionTest() }

    suspend fun evolutionStatusJSON(): String = io { requireApp().evolutionStatusJSON }

    suspend fun applyEvolutionRule(ruleId: String) = io { requireApp().applyEvolutionRule(ruleId) }
}

package com.snishaper.android.core.data

import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.engine.SnishaperEngine
import com.snishaper.android.core.model.CloudflareIpStat
import com.snishaper.android.core.model.TunConfig
import org.json.JSONObject

/**
 * 通用设置（对应旧桥接契约 SetListenPort / Get-SetLanguage / Get-SetAppearanceMode /
 * Get-SetAutoUpdateRules / Get-UpdateTUNConfig / Get-RefreshIPv6Check /
 * Cloudflare 池统计）。语言 / 外观 / 自动更新规则存于引擎设置文档，
 * 读改走 getSettingsJSON/updateSettingsJSON——与旧桥接 patchSettings 一致。
 */
interface SettingsRepository {
    suspend fun listenPort(): AppResult<Int>
    suspend fun setListenPort(port: Int): AppResult<Unit>
    suspend fun checkPortAvailable(port: Int): AppResult<Boolean>

    suspend fun language(): AppResult<String>
    suspend fun setLanguage(lang: String): AppResult<Unit>

    suspend fun appearanceMode(): AppResult<String>
    suspend fun setAppearanceMode(mode: String): AppResult<Unit>

    suspend fun autoUpdateRules(): AppResult<Boolean>
    suspend fun setAutoUpdateRules(enabled: Boolean): AppResult<Unit>

    suspend fun tunConfig(): AppResult<TunConfig>
    suspend fun updateTunConfig(config: TunConfig): AppResult<Unit>

    suspend fun ipv6Available(): AppResult<Boolean>
    suspend fun refreshIPv6(): AppResult<Boolean>

    suspend fun cloudflareIpStats(): AppResult<List<CloudflareIpStat>>
    suspend fun forceFetchCloudflareIPs(): AppResult<Unit>
    suspend fun triggerCFHealthCheck(): AppResult<Unit>
    suspend fun removeInvalidCFIPs(): AppResult<Int>
}

internal class SettingsRepositoryImpl : SettingsRepository {

    /**
     * UpdateSettingsJSON 会整体替换收到的设置段落，必须先读完整文档、
     * 打补丁后整体写回（旧桥接 patchSettings 的既定做法）。
     */
    private suspend fun patchSettings(key: String, value: Any): AppResult<Unit> = AppResult.of {
        val doc = runCatching { JSONObject(SnishaperEngine.settingsJSON()) }.getOrElse { JSONObject() }
        doc.put(key, value)
        SnishaperEngine.updateSettingsJSON(doc.toString())
    }

    private suspend fun settingsString(key: String, default: String): String =
        runCatching { JSONObject(SnishaperEngine.settingsJSON()).optString(key, default) }
            .getOrDefault(default)

    override suspend fun listenPort(): AppResult<Int> =
        AppResult.of { SnishaperEngine.status.value.listenPort }

    override suspend fun setListenPort(port: Int): AppResult<Unit> =
        AppResult.of { SnishaperEngine.setListenPort(port) }

    override suspend fun checkPortAvailable(port: Int): AppResult<Boolean> =
        AppResult.of { SnishaperEngine.checkPortAvailable(port) }

    override suspend fun language(): AppResult<String> =
        AppResult.of { settingsString("language", "") }

    override suspend fun setLanguage(lang: String): AppResult<Unit> =
        patchSettings("language", lang)

    override suspend fun appearanceMode(): AppResult<String> =
        AppResult.of { settingsString("appearance_mode", "") }

    override suspend fun setAppearanceMode(mode: String): AppResult<Unit> =
        patchSettings("appearance_mode", mode)

    override suspend fun autoUpdateRules(): AppResult<Boolean> = AppResult.of {
        runCatching { JSONObject(SnishaperEngine.settingsJSON()).optBoolean("auto_update_rules", false) }
            .getOrDefault(false)
    }

    override suspend fun setAutoUpdateRules(enabled: Boolean): AppResult<Unit> =
        patchSettings("auto_update_rules", enabled)

    override suspend fun tunConfig(): AppResult<TunConfig> =
        AppResult.of { TunConfig.fromJson(SnishaperEngine.tunConfigJSON()) }

    override suspend fun updateTunConfig(config: TunConfig): AppResult<Unit> = AppResult.of {
        // 旧桥接做法：读完整 TUN 配置、只替换 mtu 后整体写回
        val doc = runCatching { JSONObject(SnishaperEngine.tunConfigJSON()) }.getOrElse { JSONObject() }
        doc.put("mtu", config.mtu)
        SnishaperEngine.updateTunConfigJSON(doc.toString())
    }

    override suspend fun ipv6Available(): AppResult<Boolean> =
        AppResult.of { SnishaperEngine.ipv6Available() }

    override suspend fun refreshIPv6(): AppResult<Boolean> =
        AppResult.of { SnishaperEngine.refreshIPv6Check() }

    override suspend fun cloudflareIpStats(): AppResult<List<CloudflareIpStat>> =
        AppResult.of { CloudflareIpStat.listFromJson(SnishaperEngine.cloudflareIpStatsJSON()) }

    override suspend fun forceFetchCloudflareIPs(): AppResult<Unit> =
        AppResult.of { SnishaperEngine.forceFetchCloudflareIPs() }

    override suspend fun triggerCFHealthCheck(): AppResult<Unit> =
        AppResult.of { SnishaperEngine.triggerCFHealthCheck() }

    override suspend fun removeInvalidCFIPs(): AppResult<Int> =
        AppResult.of { SnishaperEngine.removeInvalidCFIPs() }
}

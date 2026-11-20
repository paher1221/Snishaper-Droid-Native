package com.snishaper.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 引擎状态（Go 侧 `statusPayload` 的 JSON 反序列化）。
 *
 * 字段名与桌面版 Go 结构体保持一致（`ignoreUnknownKeys` 允许桌面版新增字段
 * 透传不丢——这是从旧 WebView 桥接层继承的契约：JSON 原样透传，只在唯一解析点反序列化）。
 */
@Serializable
data class EngineStatus(
    @SerialName("proxyRunning") val proxyRunning: Boolean = false,
    @SerialName("tunRunning") val tunRunning: Boolean = false,
    @SerialName("tunSupported") val tunSupported: Boolean = true,
    @SerialName("tunMessage") val tunMessage: String = "",
    @SerialName("listenAddr") val listenAddr: String = "127.0.0.1:8080",
    @SerialName("listenPort") val listenPort: Int = 8080,
    @SerialName("proxyMode") val proxyMode: String = "",
    @SerialName("ipv6Available") val ipv6Available: Boolean = true,
    @SerialName("lastError") val lastError: String = "",
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun fromJson(json: String): EngineStatus =
            runCatching { parser.decodeFromString<EngineStatus>(json) }
                .getOrDefault(EngineStatus())
    }
}

/** 构建信息（gomobile `buildInfoJSON`）。 */
@Serializable
data class BuildInfo(
    @SerialName("version") val version: String = "",
    @SerialName("channel") val channel: String = "stable",
    @SerialName("coreVersion") val coreVersion: String = "",
    @SerialName("goVersion") val goVersion: String = "",
    @SerialName("platform") val platform: String = "android",
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun fromJson(json: String): BuildInfo =
            runCatching { parser.decodeFromString<BuildInfo>(json) }
                .getOrDefault(BuildInfo())
    }
}

/** 引擎上报的一条路由命中（实时连接流的最小单元）。 */
data class RouteEvent(
    val domain: String,
    val mode: String,
    val timestamp: Long = System.currentTimeMillis(),
)

/** 流量统计（Go `statsPayload`：down/up/etc 字节计数）。 */
@Serializable
data class ProxyStats(
    @SerialName("down") val down: Long = 0,
    @SerialName("up") val up: Long = 0,
    @SerialName("etc") val etc: Long = 0,
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun fromJson(json: String): ProxyStats =
            runCatching { parser.decodeFromString<ProxyStats>(json) }
                .getOrDefault(ProxyStats())
    }
}

/** TUN 隧道状态（Go `tunStatusPayload`）。 */
@Serializable
data class TunStatus(
    @SerialName("supported") val supported: Boolean = true,
    @SerialName("running") val running: Boolean = false,
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("message") val message: String = "",
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun fromJson(json: String): TunStatus =
            runCatching { parser.decodeFromString<TunStatus>(json) }
                .getOrDefault(TunStatus())
    }
}

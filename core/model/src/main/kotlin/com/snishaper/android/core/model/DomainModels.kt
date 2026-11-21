package com.snishaper.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** 活动连接（Go `ConnInfo`，GetActiveConnectionsJSON 的元素）。 */
@Serializable
data class ConnectionInfo(
    @SerialName("id") val id: Long = 0,
    @SerialName("protocol") val protocol: String = "",
    @SerialName("host") val host: String = "",
    @SerialName("dst_addr") val dstAddr: String = "",
    @SerialName("rule") val rule: String = "",
    @SerialName("rule_domain") val ruleDomain: String = "",
    @SerialName("mode") val mode: String = "",
    @SerialName("source") val source: String = "",
    @SerialName("state") val state: String = "",
    @SerialName("started_at") val startedAt: String = "",
    @SerialName("bytes_up") val bytesUp: Long = 0,
    @SerialName("bytes_down") val bytesDown: Long = 0,
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun listFromJson(json: String): List<ConnectionInfo> =
            runCatching { parser.decodeFromString<List<ConnectionInfo>>(json) }.getOrDefault(emptyList())
    }
}

/** 证书校验策略（Go `CertVerifyPolicy`）。 */
@Serializable
data class CertVerify(
    @SerialName("mode") val mode: String = "",
    @SerialName("names") val names: List<String> = emptyList(),
    @SerialName("suffixes") val suffixes: List<String> = emptyList(),
    @SerialName("spki_sha256") val spkiSha256: List<String> = emptyList(),
    @SerialName("allow_unknown_authority") val allowUnknownAuthority: Boolean = false,
)

/**
 * 站点分组规则（Go `SiteGroup`）。字段与桌面版一致；frag 为透传的
 * TLS 分片配置（结构由引擎定义，原样保留避免编辑丢失）。
 */
@Serializable
data class SiteGroup(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("website") val website: String = "",
    @SerialName("mode") val mode: String = "mitm",
    @SerialName("upstream") val upstream: String = "",
    @SerialName("domains") val domains: List<String> = emptyList(),
    @SerialName("dns_mode") val dnsMode: String = "",
    @SerialName("sni_fake") val sniFake: String = "",
    @SerialName("enabled") val enabled: Boolean = true,
    @SerialName("ech_enabled") val echEnabled: Boolean = false,
    @SerialName("ech_profile_id") val echProfileId: String = "",
    @SerialName("ech_domain") val echDomain: String = "",
    @SerialName("use_cf_pool") val useCfPool: Boolean = false,
    @SerialName("ip_pool") val ipPool: String = "",
    @SerialName("nat64_enabled") val nat64Enabled: Boolean = false,
    @SerialName("nat64_profile_id") val nat64ProfileId: String = "",
    @SerialName("frag") val frag: JsonElement? = null,
    @SerialName("cert_verify") val certVerify: CertVerify = CertVerify(),
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun listFromJson(json: String): List<SiteGroup> =
            runCatching { parser.decodeFromString<List<SiteGroup>>(json) }.getOrDefault(emptyList())
    }
}

/** 规则分层信息（Go `RulesLayerInfo`：id → override / user / ""）。 */
@Serializable
data class RulesLayerInfo(
    @SerialName("site_group_layers") val siteGroupLayers: Map<String, String> = emptyMap(),
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun fromJson(json: String): RulesLayerInfo =
            runCatching { parser.decodeFromString<RulesLayerInfo>(json) }.getOrDefault(RulesLayerInfo())
    }
}

/** 自动分流状态（Go `AutoRoutingStatus`）。 */
@Serializable
data class AutoRoutingStatus(
    @SerialName("enabled") val enabled: Boolean = false,
    @SerialName("domain_count") val domainCount: Int = 0,
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun fromJson(json: String): AutoRoutingStatus =
            runCatching { parser.decodeFromString<AutoRoutingStatus>(json) }.getOrDefault(AutoRoutingStatus())
    }
}

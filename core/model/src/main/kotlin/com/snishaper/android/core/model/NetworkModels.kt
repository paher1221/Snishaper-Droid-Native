package com.snishaper.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * M3 网络配置模型：DNS 节点 / ECH 档案 / NAT64 档案 / IP 池 / 迁移报告。
 * 字段名与桌面版 Go 结构体保持一致（ignoreUnknownKeys 允许桌面版新增字段透传不丢）。
 */

/** DNS 节点（Go `DNSNode`，GetDNSNodesJSON 的元素）。 */
@Serializable
data class DnsNode(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("sni") val sni: String = "",
    @SerialName("ips") val ips: List<String> = emptyList(),
    @SerialName("ech_enabled") val echEnabled: Boolean = false,
    @SerialName("ech_profile_id") val echProfileId: String = "",
    @SerialName("quic") val quic: Boolean = false,
    @SerialName("enabled") val enabled: Boolean = true,
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun listFromJson(json: String): List<DnsNode> =
            runCatching { parser.decodeFromString<List<DnsNode>>(json) }.getOrDefault(emptyList())
    }
}

/** DNS 节点连通性测试结果（Go `testDNSNode` 返回）。 */
@Serializable
data class DnsTestResult(
    @SerialName("success") val success: Boolean = false,
    @SerialName("latency") val latency: String = "",
    @SerialName("ips") val ips: List<String> = emptyList(),
    @SerialName("error") val error: String = "",
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun fromJson(json: String): DnsTestResult =
            runCatching { parser.decodeFromString<DnsTestResult>(json) }.getOrDefault(DnsTestResult())
    }
}

/** ECH 档案（Go `ECHProfile`）。 */
@Serializable
data class EchProfile(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("discovery_domain") val discoveryDomain: String = "",
    @SerialName("doh_upstream") val dohUpstream: String = "",
    @SerialName("config") val config: String = "",
    @SerialName("auto_update") val autoUpdate: Boolean = true,
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun listFromJson(json: String): List<EchProfile> =
            runCatching { parser.decodeFromString<List<EchProfile>>(json) }.getOrDefault(emptyList())
    }
}

/** NAT64 档案（Go `NAT64Profile`）。 */
@Serializable
data class Nat64Profile(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("prefix") val prefix: String = "",
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun listFromJson(json: String): List<Nat64Profile> =
            runCatching { parser.decodeFromString<List<Nat64Profile>>(json) }.getOrDefault(emptyList())
    }
}

/** IP 池（Go `IPPool`，GetIPPoolsJSON 的元素）。 */
@Serializable
data class IpPool(
    @SerialName("name") val name: String = "",
    @SerialName("preferred_ips") val preferredIps: List<String> = emptyList(),
    @SerialName("auto_update") val autoUpdate: Boolean = true,
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun listFromJson(json: String): List<IpPool> =
            runCatching { parser.decodeFromString<List<IpPool>>(json) }.getOrDefault(emptyList())
    }
}

/** 最近一次迁移报告（Go `lastMigrationReportJSON`）。 */
@Serializable
data class MigrationReport(
    @SerialName("changes") val changes: List<String> = emptyList(),
    @SerialName("warnings") val warnings: List<String> = emptyList(),
    @SerialName("errors") val errors: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = changes.isEmpty() && warnings.isEmpty() && errors.isEmpty()

    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun fromJson(json: String): MigrationReport =
            runCatching { parser.decodeFromString<MigrationReport>(json) }.getOrDefault(MigrationReport())
    }
}

package com.snishaper.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * M4 模型：配置档案 / CA 证书 / TUN 配置 / Cloudflare 池统计。
 * 字段名与桌面版 Go 结构体保持一致（ignoreUnknownKeys 允许桌面版新增字段透传不丢）。
 */

/** 配置档案（Go `listProfilesJSON` 的元素，name/updated_at 两个字段）。 */
@Serializable
data class ProfileInfo(
    @SerialName("name") val name: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun listFromJson(json: String): List<ProfileInfo> =
            runCatching { parser.decodeFromString<List<ProfileInfo>>(json) }.getOrDefault(emptyList())
    }
}

/** 档案导入结果（Go `importProfileJSON` 返回 {name, warnings[]}）。 */
@Serializable
data class ProfileImportResult(
    @SerialName("name") val name: String = "",
    @SerialName("warnings") val warnings: List<String> = emptyList(),
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun fromJson(json: String): ProfileImportResult =
            runCatching { parser.decodeFromString<ProfileImportResult>(json) }.getOrDefault(ProfileImportResult())
    }
}

/** CA 安装状态（Go `getCAInstallStatusJSON` 返回 {Installed, CertPath}）。 */
@Serializable
data class CaInstallStatus(
    @SerialName("Installed") val installed: Boolean = false,
    @SerialName("CertPath") val certPath: String = "",
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun fromJson(json: String): CaInstallStatus =
            runCatching { parser.decodeFromString<CaInstallStatus>(json) }.getOrDefault(CaInstallStatus())
    }
}

/** 系统证书库中的一张已安装证书（Go `getInstalledCertsJSON` 的元素）。 */
@Serializable
data class InstalledCert(
    @SerialName("subject") val subject: String = "",
    @SerialName("storeLocation") val storeLocation: String = "",
    @SerialName("storeName") val storeName: String = "",
    @SerialName("thumbprint") val thumbprint: String = "",
    @SerialName("token") val token: String = "",
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun listFromJson(json: String): List<InstalledCert> =
            runCatching { parser.decodeFromString<List<InstalledCert>>(json) }.getOrDefault(emptyList())
    }
}

/** TUN 配置（Go `getTUNConfigJSON`；M4 只消费 mtu 字段，其余透传保留）。 */
@Serializable
data class TunConfig(
    @SerialName("mtu") val mtu: Int = 1500,
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun fromJson(json: String): TunConfig =
            runCatching { parser.decodeFromString<TunConfig>(json) }.getOrDefault(TunConfig())
    }
}

/** Cloudflare 池单条 IP 统计（Go `getCloudflareIPStatsJSON` 的元素）。 */
@Serializable
data class CloudflareIpStat(
    @SerialName("ip") val ip: String = "",
    @SerialName("latency") val latency: String = "",
) {
    companion object {
        private val parser = Json { ignoreUnknownKeys = true }

        fun listFromJson(json: String): List<CloudflareIpStat> =
            runCatching { parser.decodeFromString<List<CloudflareIpStat>>(json) }.getOrDefault(emptyList())
    }
}

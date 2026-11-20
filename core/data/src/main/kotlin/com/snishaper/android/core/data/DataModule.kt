package com.snishaper.android.core.data

/**
 * M0 占位：仓库实现随 M1 落地。此处只固定模块边界与依赖方向，
 * 领域仓库接口的清单见 docs/ARCHITECTURE.md §4（桥接契约迁移映射）。
 */
object DataLayerContract {
    /** M1 起按域落地的仓库接口（与旧桥接契约一一对应）。 */
    val plannedRepositories = listOf(
        "SettingsRepository",
        "AppInfoRepository",
        "NoticesRepository",
        "ProxyRepository",
        "TunRepository",
        "RulesRepository",
        "ConfigRepository",
        "DnsRepository",
        "EchRepository",
        "Nat64Repository",
        "MigrationRepository",
        "IpPoolRepository",
        "ConnectionsRepository",
        "LogsRepository",
        "EvolutionRepository",
        "CertRepository",
        "ProfileRepository",
    )
}

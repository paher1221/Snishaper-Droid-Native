package com.snishaper.android.core.data

import android.content.Context
import com.snishaper.android.core.engine.SnishaperEngine

/**
 * 手动依赖图：Application.onCreate 调 [init] 存一次 applicationContext，
 * 之后各仓库按域懒加载单例。不引入 DI 框架——模块数量与依赖方向都由
 * Gradle 边界约束（见 docs/ARCHITECTURE.md §2），手动图足够清晰。
 *
 * 18 个领域仓库的落地清单见 docs/ARCHITECTURE.md §4 映射表；M1 先落
 * Proxy / Tun / AppInfo 三个（Dashboard 最小闭环），其余随里程碑逐个补齐：
 * Settings, Notices, Rules, Config, Dns, Ech, Nat64, Migration, IpPool,
 * Connections, Logs, Evolution, Cert, Profile。
 */
object DataGraph {

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun requireContext(): Context =
        appContext ?: throw IllegalStateException("DataGraph.init(context) was not called")

    val proxyRepository: ProxyRepository by lazy { ProxyRepositoryImpl() }
    val tunRepository: TunRepository by lazy { TunRepositoryImpl(::requireContext) }
    val appInfoRepository: AppInfoRepository by lazy { AppInfoRepositoryImpl() }
    val connectionsRepository: ConnectionsRepository by lazy { ConnectionsRepositoryImpl() }
    val logsRepository: LogsRepository by lazy { LogsRepositoryImpl() }
    val rulesRepository: RulesRepository by lazy { RulesRepositoryImpl() }
    val routingRepository: RoutingRepository by lazy { RoutingRepositoryImpl() }

    /** 引擎状态流：仓库层不重复包装，UI 直接收集（引擎是进程级单例）。 */
    val engineStatus get() = SnishaperEngine.status
}

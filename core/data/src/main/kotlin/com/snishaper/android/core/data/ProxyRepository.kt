package com.snishaper.android.core.data

import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.common.map
import com.snishaper.android.core.engine.SnishaperEngine
import com.snishaper.android.core.model.ProxyStats

/**
 * 代理生命周期与状态（对应旧桥接契约 StartProxy / StopProxy /
 * IsProxyRunning / GetProxyMode / SetListenPort / GetStats）。
 */
interface ProxyRepository {
    suspend fun start(): AppResult<Unit>
    suspend fun stop(): AppResult<Unit>
    suspend fun isRunning(): AppResult<Boolean>
    suspend fun mode(): AppResult<String>
    suspend fun setListenPort(port: Int): AppResult<Unit>
    suspend fun stats(): AppResult<ProxyStats>
}

internal class ProxyRepositoryImpl : ProxyRepository {

    override suspend fun start(): AppResult<Unit> = AppResult.of { SnishaperEngine.startProxy() }

    override suspend fun stop(): AppResult<Unit> = AppResult.of { SnishaperEngine.stopProxy() }

    override suspend fun isRunning(): AppResult<Boolean> = AppResult.of { SnishaperEngine.isProxyRunning() }

    override suspend fun mode(): AppResult<String> = AppResult.of { SnishaperEngine.proxyMode() }

    override suspend fun setListenPort(port: Int): AppResult<Unit> =
        AppResult.of { SnishaperEngine.setListenPort(port) }

    override suspend fun stats(): AppResult<ProxyStats> =
        AppResult.of { SnishaperEngine.statsJSON() }.map { ProxyStats.fromJson(it) }
}

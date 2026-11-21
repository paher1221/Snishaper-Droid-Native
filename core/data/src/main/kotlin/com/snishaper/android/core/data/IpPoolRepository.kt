package com.snishaper.android.core.data

import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.engine.SnishaperEngine
import com.snishaper.android.core.model.IpPool
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * IP 池管理（对应旧桥接契约 Get/UpdateIPPools、GetIPPoolStats、
 * ForceFetch/Refresh/RemoveInvalid/TriggerHealthCheck）。
 */
interface IpPoolRepository {
    suspend fun pools(): AppResult<List<IpPool>>
    suspend fun update(pools: List<IpPool>): AppResult<Unit>
    suspend fun statsJSON(name: String): AppResult<String>
    suspend fun forceFetch(name: String): AppResult<String>
    suspend fun refresh(name: String): AppResult<Unit>
    suspend fun removeInvalid(name: String): AppResult<Int>
    suspend fun healthCheck(name: String): AppResult<Unit>
}

internal class IpPoolRepositoryImpl : IpPoolRepository {

    private val parser = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun pools(): AppResult<List<IpPool>> =
        AppResult.of { IpPool.listFromJson(SnishaperEngine.ipPoolsJSON()) }

    override suspend fun update(pools: List<IpPool>): AppResult<Unit> =
        AppResult.of { SnishaperEngine.updateIpPoolsJSON(parser.encodeToString(pools)) }

    override suspend fun statsJSON(name: String): AppResult<String> =
        AppResult.of { SnishaperEngine.ipPoolStatsJSON(name) }

    override suspend fun forceFetch(name: String): AppResult<String> =
        AppResult.of { SnishaperEngine.forceFetchIpPoolIPsJSON(name) }

    override suspend fun refresh(name: String): AppResult<Unit> =
        AppResult.of { SnishaperEngine.refreshIpPoolJSON(name) }

    override suspend fun removeInvalid(name: String): AppResult<Int> =
        AppResult.of { SnishaperEngine.removeInvalidIpPoolIPsJSON(name) }

    override suspend fun healthCheck(name: String): AppResult<Unit> =
        AppResult.of { SnishaperEngine.triggerIpPoolHealthCheckJSON(name) }
}

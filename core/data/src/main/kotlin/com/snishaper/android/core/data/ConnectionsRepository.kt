package com.snishaper.android.core.data

import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.engine.SnishaperEngine
import com.snishaper.android.core.model.ConnectionInfo

/** 活动连接（对应旧桥接契约 GetActiveConnections）。 */
interface ConnectionsRepository {
    suspend fun activeConnections(): AppResult<List<ConnectionInfo>>
}

internal class ConnectionsRepositoryImpl : ConnectionsRepository {
    override suspend fun activeConnections(): AppResult<List<ConnectionInfo>> =
        AppResult.of { ConnectionInfo.listFromJson(SnishaperEngine.activeConnectionsJSON()) }
}

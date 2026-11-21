package com.snishaper.android.core.data

import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.engine.SnishaperEngine
import com.snishaper.android.core.model.AutoRoutingStatus
import com.snishaper.android.core.model.RouteEvent
import kotlinx.coroutines.flow.SharedFlow

/** 自动分流（Android 上为只读状态 + 实时命中流，对应旧桥接契约 GetAutoRoutingStatus / EventsOn("app:route")）。 */
interface RoutingRepository {
    suspend fun status(): AppResult<AutoRoutingStatus>
    val routeEvents: SharedFlow<RouteEvent>
}

internal class RoutingRepositoryImpl : RoutingRepository {
    override suspend fun status(): AppResult<AutoRoutingStatus> =
        AppResult.of { AutoRoutingStatus.fromJson(SnishaperEngine.autoRoutingStatusJSON()) }

    override val routeEvents: SharedFlow<RouteEvent>
        get() = SnishaperEngine.routes
}

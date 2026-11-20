package com.snishaper.android.core.data

import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.engine.SnishaperEngine
import com.snishaper.android.core.model.BuildInfo

/**
 * 构建信息（对应旧桥接契约 GetBuildInfo）。
 */
interface AppInfoRepository {
    suspend fun buildInfo(): AppResult<BuildInfo>
}

internal class AppInfoRepositoryImpl : AppInfoRepository {
    override suspend fun buildInfo(): AppResult<BuildInfo> = AppResult.of { SnishaperEngine.buildInfo() }
}

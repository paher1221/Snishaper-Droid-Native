package com.snishaper.android.core.data

import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.engine.SnishaperEngine

/** 引擎日志与诊断包（对应旧桥接契约 GetRecentLogs / ClearLogs / ExportDiagnostics）。 */
interface LogsRepository {
    suspend fun recentLogs(lines: Int = 500): AppResult<String>
    suspend fun clear(): AppResult<Unit>
    suspend fun exportDiagnostics(): AppResult<String>
}

internal class LogsRepositoryImpl : LogsRepository {
    override suspend fun recentLogs(lines: Int): AppResult<String> =
        AppResult.of { SnishaperEngine.recentLogs(lines) }

    override suspend fun clear(): AppResult<Unit> =
        AppResult.of { SnishaperEngine.clearLogs() }

    override suspend fun exportDiagnostics(): AppResult<String> =
        AppResult.of { SnishaperEngine.exportDiagnosticsJSON() }
}

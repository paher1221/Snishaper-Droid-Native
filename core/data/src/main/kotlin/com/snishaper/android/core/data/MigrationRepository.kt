package com.snishaper.android.core.data

import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.engine.SnishaperEngine
import com.snishaper.android.core.model.MigrationReport
import org.json.JSONObject

/**
 * 迁移服务器（对应旧桥接契约 Get/SetMigrationServer、TestMigration、
 * GetLastMigrationReport）。引擎无专用方法：migration_server 存在设置
 * 文档里，读改走 getSettingsJSON/updateSettingsJSON——与旧桥接一致。
 */
interface MigrationRepository {
    suspend fun server(): AppResult<String>
    suspend fun setServer(server: String): AppResult<Unit>
    suspend fun test(server: String): AppResult<String>
    suspend fun lastReport(): AppResult<MigrationReport>
}

internal class MigrationRepositoryImpl : MigrationRepository {

    override suspend fun server(): AppResult<String> = AppResult.of {
        runCatching { JSONObject(SnishaperEngine.settingsJSON()).optString("migration_server", "") }
            .getOrDefault("")
    }

    /**
     * UpdateSettingsJSON 会整体替换收到的设置段落，必须先读完整文档、
     * 打补丁后整体写回（旧桥接 patchSettings 的既定做法）。
     */
    override suspend fun setServer(server: String): AppResult<Unit> = AppResult.of {
        val doc = runCatching { JSONObject(SnishaperEngine.settingsJSON()) }.getOrElse { JSONObject() }
        doc.put("migration_server", server)
        SnishaperEngine.updateSettingsJSON(doc.toString())
    }

    override suspend fun test(server: String): AppResult<String> =
        AppResult.of { SnishaperEngine.testMigration(server) }

    override suspend fun lastReport(): AppResult<MigrationReport> =
        AppResult.of { MigrationReport.fromJson(SnishaperEngine.lastMigrationReportJSON()) }
}

package com.snishaper.android.core.data

import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.engine.SnishaperEngine

/**
 * 配置导入导出（对应旧桥接契约 ExportConfig / ImportConfigWithSummary）。
 * 两个方法都返回 JSON 原文：ExportConfig 是完整配置 bundle，
 * ImportConfigWithSummary 的摘要结构由引擎定义，UI 层原样展示。
 */
interface ConfigTransferRepository {
    suspend fun exportConfig(): AppResult<String>
    suspend fun importConfig(content: String): AppResult<String>
}

internal class ConfigTransferRepositoryImpl : ConfigTransferRepository {

    override suspend fun exportConfig(): AppResult<String> =
        AppResult.of { SnishaperEngine.exportConfigJSON() }

    override suspend fun importConfig(content: String): AppResult<String> =
        AppResult.of { SnishaperEngine.importConfigWithSummaryJSON(content) }
}

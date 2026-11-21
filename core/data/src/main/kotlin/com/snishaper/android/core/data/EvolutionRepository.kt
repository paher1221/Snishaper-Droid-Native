package com.snishaper.android.core.data

import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.engine.SnishaperEngine
import com.snishaper.android.core.model.EvolutionTask
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray

/**
 * 演进模式（对应旧桥接契约 StartEvolutionTest / StopEvolutionTest /
 * GetEvolutionTestStatus / ApplyEvolutionRule）。
 * 进度快照由引擎 Listener 推送（500ms 一拍），[task] 直接暴露原始流。
 */
interface EvolutionRepository {
    /** 引擎侧最新任务快照流（JSON 原文；"" = 尚无任务）。 */
    val task: StateFlow<String>

    suspend fun currentStatus(): AppResult<EvolutionTask>
    suspend fun start(domains: List<String>, enableIPv6: Boolean): AppResult<Unit>
    suspend fun stop(): AppResult<Unit>
    suspend fun applyRule(ruleId: String): AppResult<Unit>
}

internal class EvolutionRepositoryImpl : EvolutionRepository {

    override val task: StateFlow<String> = SnishaperEngine.evolution

    override suspend fun currentStatus(): AppResult<EvolutionTask> = AppResult.of {
        EvolutionTask.fromJson(SnishaperEngine.evolutionStatusJSON()) ?: EvolutionTask()
    }

    override suspend fun start(domains: List<String>, enableIPv6: Boolean): AppResult<Unit> = AppResult.of {
        val json = JSONArray(domains).toString()
        SnishaperEngine.startEvolutionTest(json, enableIPv6)
    }

    override suspend fun stop(): AppResult<Unit> =
        AppResult.of { SnishaperEngine.stopEvolutionTest() }

    override suspend fun applyRule(ruleId: String): AppResult<Unit> =
        AppResult.of { SnishaperEngine.applyEvolutionRule(ruleId) }
}

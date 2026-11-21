package com.snishaper.android.core.data

import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.engine.SnishaperEngine
import com.snishaper.android.core.model.RulesLayerInfo
import com.snishaper.android.core.model.SiteGroup
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 站点分组规则（对应旧桥接契约 Get/Add/Update/DeleteSiteGroup、GetRulesLayerInfo、Revert、ExplainMatch）。 */
interface RulesRepository {
    suspend fun siteGroups(): AppResult<List<SiteGroup>>
    suspend fun add(group: SiteGroup): AppResult<String>
    suspend fun update(group: SiteGroup): AppResult<Unit>
    suspend fun delete(id: String): AppResult<Unit>
    suspend fun layerInfo(): AppResult<RulesLayerInfo>
    suspend fun revertToUpstream(id: String): AppResult<Unit>
    suspend fun explainMatch(host: String, ip: String): AppResult<String>
}

internal class RulesRepositoryImpl : RulesRepository {

    private val parser = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun siteGroups(): AppResult<List<SiteGroup>> =
        AppResult.of { SiteGroup.listFromJson(SnishaperEngine.siteGroupsJSON()) }

    override suspend fun add(group: SiteGroup): AppResult<String> =
        AppResult.of { SnishaperEngine.addSiteGroupJSON(parser.encodeToString(group)) }

    override suspend fun update(group: SiteGroup): AppResult<Unit> =
        AppResult.of { SnishaperEngine.updateSiteGroupJSON(parser.encodeToString(group)) }

    override suspend fun delete(id: String): AppResult<Unit> =
        AppResult.of { SnishaperEngine.deleteSiteGroup(id) }

    override suspend fun layerInfo(): AppResult<RulesLayerInfo> =
        AppResult.of { RulesLayerInfo.fromJson(SnishaperEngine.rulesLayerInfoJSON()) }

    override suspend fun revertToUpstream(id: String): AppResult<Unit> =
        AppResult.of { SnishaperEngine.revertSiteGroupToUpstream(id) }

    override suspend fun explainMatch(host: String, ip: String): AppResult<String> =
        AppResult.of { SnishaperEngine.explainMatchJSON(host, ip) }
}

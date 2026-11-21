package com.snishaper.android.core.data

import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.engine.SnishaperEngine
import com.snishaper.android.core.model.EchProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** ECH 档案管理（对应旧桥接契约 Get/Upsert/DeleteECHProfile、FetchECHConfig）。 */
interface EchRepository {
    suspend fun profiles(): AppResult<List<EchProfile>>
    suspend fun upsert(profile: EchProfile): AppResult<Unit>
    suspend fun delete(id: String): AppResult<Unit>
    suspend fun fetchConfig(discoveryDomain: String, dohUpstream: String): AppResult<String>
}

internal class EchRepositoryImpl : EchRepository {

    private val parser = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun profiles(): AppResult<List<EchProfile>> =
        AppResult.of { EchProfile.listFromJson(SnishaperEngine.echProfilesJSON()) }

    override suspend fun upsert(profile: EchProfile): AppResult<Unit> =
        AppResult.of { SnishaperEngine.upsertEchProfileJSON(parser.encodeToString(profile)) }

    override suspend fun delete(id: String): AppResult<Unit> =
        AppResult.of { SnishaperEngine.deleteEchProfile(id) }

    override suspend fun fetchConfig(discoveryDomain: String, dohUpstream: String): AppResult<String> =
        AppResult.of { SnishaperEngine.fetchEchConfigJSON(discoveryDomain, dohUpstream) }
}

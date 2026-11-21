package com.snishaper.android.core.data

import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.engine.SnishaperEngine
import com.snishaper.android.core.model.Nat64Profile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** NAT64 档案管理（对应旧桥接契约 Get/Add/Update/Delete/TestNAT64Profile）。 */
interface Nat64Repository {
    suspend fun profiles(): AppResult<List<Nat64Profile>>
    suspend fun add(profile: Nat64Profile): AppResult<String>
    suspend fun update(profile: Nat64Profile): AppResult<Unit>
    suspend fun delete(id: String): AppResult<Unit>
    suspend fun test(prefix: String): AppResult<String>
}

internal class Nat64RepositoryImpl : Nat64Repository {

    private val parser = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun profiles(): AppResult<List<Nat64Profile>> =
        AppResult.of { Nat64Profile.listFromJson(SnishaperEngine.nat64ProfilesJSON()) }

    override suspend fun add(profile: Nat64Profile): AppResult<String> =
        AppResult.of { SnishaperEngine.addNat64ProfileJSON(parser.encodeToString(profile)) }

    override suspend fun update(profile: Nat64Profile): AppResult<Unit> =
        AppResult.of { SnishaperEngine.updateNat64ProfileJSON(parser.encodeToString(profile)) }

    override suspend fun delete(id: String): AppResult<Unit> =
        AppResult.of { SnishaperEngine.deleteNat64Profile(id) }

    override suspend fun test(prefix: String): AppResult<String> =
        AppResult.of { SnishaperEngine.testNat64ProfileJSON(prefix) }
}

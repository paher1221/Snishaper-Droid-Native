package com.snishaper.android.core.data

import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.engine.SnishaperEngine
import com.snishaper.android.core.model.DnsNode
import com.snishaper.android.core.model.DnsTestResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** DNS 节点管理（对应旧桥接契约 Get/Add/Update/Delete/SetPriority/TestDNSNode）。 */
interface DnsRepository {
    suspend fun nodes(): AppResult<List<DnsNode>>
    suspend fun add(node: DnsNode): AppResult<String>
    suspend fun update(node: DnsNode): AppResult<Unit>
    suspend fun delete(id: String): AppResult<Unit>
    suspend fun setPriority(id: String, index: Int): AppResult<Unit>
    suspend fun test(id: String): AppResult<DnsTestResult>
}

internal class DnsRepositoryImpl : DnsRepository {

    private val parser = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun nodes(): AppResult<List<DnsNode>> =
        AppResult.of { DnsNode.listFromJson(SnishaperEngine.dnsNodesJSON()) }

    override suspend fun add(node: DnsNode): AppResult<String> =
        AppResult.of { SnishaperEngine.addDnsNodeJSON(parser.encodeToString(node)) }

    override suspend fun update(node: DnsNode): AppResult<Unit> =
        AppResult.of { SnishaperEngine.updateDnsNodeJSON(parser.encodeToString(node)) }

    override suspend fun delete(id: String): AppResult<Unit> =
        AppResult.of { SnishaperEngine.deleteDnsNode(id) }

    override suspend fun setPriority(id: String, index: Int): AppResult<Unit> =
        AppResult.of { SnishaperEngine.setDnsNodePriority(id, index) }

    override suspend fun test(id: String): AppResult<DnsTestResult> =
        AppResult.of { DnsTestResult.fromJson(SnishaperEngine.testDnsNodeJSON(id)) }
}

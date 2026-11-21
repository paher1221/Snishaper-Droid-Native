package com.snishaper.android.core.data

import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.engine.SnishaperEngine
import com.snishaper.android.core.model.ProfileImportResult
import com.snishaper.android.core.model.ProfileInfo

/**
 * 配置档案管理（对应旧桥接契约 ListProfiles / GetActiveProfile / Create /
 * Rename / Duplicate / Switch / Export / ImportProfile）。
 */
interface ProfileRepository {
    suspend fun profiles(): AppResult<List<ProfileInfo>>
    suspend fun activeProfile(): AppResult<String>
    suspend fun create(name: String): AppResult<Unit>
    suspend fun rename(oldName: String, newName: String): AppResult<Unit>
    suspend fun duplicate(source: String, newName: String): AppResult<Unit>
    suspend fun switch(name: String): AppResult<Unit>

    /** 返回档案 bundle JSON 原文，由 UI 层落盘（SAF）。 */
    suspend fun export(name: String): AppResult<String>

    /** content 为档案 bundle JSON 原文；返回 {name, warnings[]}。 */
    suspend fun import(content: String): AppResult<ProfileImportResult>
}

internal class ProfileRepositoryImpl : ProfileRepository {

    override suspend fun profiles(): AppResult<List<ProfileInfo>> =
        AppResult.of { ProfileInfo.listFromJson(SnishaperEngine.profilesJSON()) }

    override suspend fun activeProfile(): AppResult<String> = AppResult.of {
        SnishaperEngine.activeProfileJSON().trim().removeSurrounding("\"")
    }

    override suspend fun create(name: String): AppResult<Unit> =
        AppResult.of { SnishaperEngine.createProfileJSON(name); Unit }

    override suspend fun rename(oldName: String, newName: String): AppResult<Unit> =
        AppResult.of { SnishaperEngine.renameProfileJSON(oldName, newName) }

    override suspend fun duplicate(source: String, newName: String): AppResult<Unit> =
        AppResult.of { SnishaperEngine.duplicateProfileJSON(source, newName) }

    override suspend fun switch(name: String): AppResult<Unit> =
        AppResult.of { SnishaperEngine.switchProfileJSON(name) }

    override suspend fun export(name: String): AppResult<String> =
        AppResult.of { SnishaperEngine.exportProfileJSON(name) }

    override suspend fun import(content: String): AppResult<ProfileImportResult> =
        AppResult.of { ProfileImportResult.fromJson(SnishaperEngine.importProfileJSON(content)) }
}

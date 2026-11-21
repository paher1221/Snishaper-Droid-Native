package com.snishaper.android.core.data

import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.engine.SnishaperEngine
import com.snishaper.android.core.model.CaInstallStatus
import com.snishaper.android.core.model.InstalledCert

/**
 * CA 证书管理（对应旧桥接契约 InstallCA / RegenerateCert / UninstallCert /
 * GetCAInstallStatus / GetInstalledCerts / GetCACertPEM）。
 */
interface CertRepository {
    suspend fun installStatus(): AppResult<CaInstallStatus>
    suspend fun installedCerts(): AppResult<List<InstalledCert>>
    suspend fun regenerate(): AppResult<Unit>
    suspend fun uninstall(token: String): AppResult<Unit>

    /** 返回 CA 证书 PEM 原文，由 UI 层落盘（SAF）。 */
    suspend fun exportPEM(): AppResult<String>
}

internal class CertRepositoryImpl : CertRepository {

    override suspend fun installStatus(): AppResult<CaInstallStatus> =
        AppResult.of { CaInstallStatus.fromJson(SnishaperEngine.caInstallStatusJSON()) }

    override suspend fun installedCerts(): AppResult<List<InstalledCert>> =
        AppResult.of { InstalledCert.listFromJson(SnishaperEngine.installedCertsJSON()) }

    override suspend fun regenerate(): AppResult<Unit> =
        AppResult.of { SnishaperEngine.regenerateCert() }

    override suspend fun uninstall(token: String): AppResult<Unit> =
        AppResult.of { SnishaperEngine.uninstallCert(token) }

    override suspend fun exportPEM(): AppResult<String> =
        AppResult.of { SnishaperEngine.caCertPEM() }
}

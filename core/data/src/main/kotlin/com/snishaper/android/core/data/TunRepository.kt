package com.snishaper.android.core.data

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.engine.vpn.TunVpnService
import com.snishaper.android.core.engine.vpn.VpnInstaller
import com.snishaper.android.core.model.TunStatus

/**
 * TUN 启动结果：consent 已授予时服务直接拉起；否则把系统 VPN 授权
 * Intent 交给 Activity（startActivityForResult），授权成功后重试。
 * 对应旧桥接契约 StartTUN 返回 "started" / "consent" 的两种分支。
 */
sealed interface TunStartResult {
    data object Started : TunStartResult
    data class ConsentNeeded(val consentIntent: Intent) : TunStartResult
}

/**
 * TUN 隧道生命周期（对应旧桥接契约 StartTUN / StopTUN / GetTUNStatus）。
 */
interface TunRepository {
    suspend fun start(): AppResult<TunStartResult>
    suspend fun stop(): AppResult<Unit>
    suspend fun status(): AppResult<TunStatus>
}

internal class TunRepositoryImpl(private val context: () -> Context) : TunRepository {

    override suspend fun start(): AppResult<TunStartResult> = AppResult.of {
        val appContext = context()
        val consent = VpnInstaller.prepare(appContext)
        if (consent == null) {
            // minSdk 24：ContextCompat 在 API 26 以下回退为 startService
            ContextCompat.startForegroundService(
                appContext,
                Intent(appContext, TunVpnService::class.java),
            )
            TunStartResult.Started
        } else {
            TunStartResult.ConsentNeeded(consent)
        }
    }

    override suspend fun stop(): AppResult<Unit> = AppResult.of {
        val appContext = context()
        appContext.startService(
            Intent(appContext, TunVpnService::class.java).setAction(TunVpnService.ACTION_STOP),
        )
    }

    override suspend fun status(): AppResult<TunStatus> = AppResult.of {
        TunStatus.fromJson(com.snishaper.android.core.engine.SnishaperEngine.tunStatusJSON())
    }
}

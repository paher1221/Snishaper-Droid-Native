package com.snishaper.android.core.engine.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService

/** Thin helper around `VpnService.prepare` so UI code stays free of VPN plumbing. */
object VpnInstaller {

    /**
     * Null → consent already granted; otherwise launch the returned intent from an
     * Activity (`startActivityForResult`) so the user can grant VPN consent.
     */
    fun prepare(context: Context): Intent? = VpnService.prepare(context)
}

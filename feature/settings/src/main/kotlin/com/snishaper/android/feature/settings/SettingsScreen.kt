package com.snishaper.android.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snishaper.android.core.design.theme.AppearanceMode
import com.snishaper.android.feature.settings.vm.SettingsViewModel

/**
 * 设置页：通用 / TUN / 证书 / IPv6 / Cloudflare 池 / IP 池 / 配置导入导出
 * （与旧版 Settings.tsx 各区块一致；主题配色选择随 M5 落地）。
 * 证书与配置的导出/导入走系统 SAF（旧版是 WebView 下载/上传）。
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 证书导出 / 配置导出：先取内容，再让用户选目标位置
    val certExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-x509-ca-cert"),
    ) { uri ->
        if (uri != null) viewModel.writePendingCert(uri, context.contentResolver)
        else viewModel.cancelPendingCert()
    }
    val configExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) viewModel.writePendingConfig(uri, context.contentResolver)
        else viewModel.cancelPendingConfig()
    }
    val configImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val content = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            }.getOrNull()
            if (!content.isNullOrEmpty()) viewModel.importConfig(content)
        }
    }

    LaunchedEffect(state.pendingCertPem) {
        if (state.pendingCertPem != null) certExportLauncher.launch("snishaper-ca.crt")
    }
    LaunchedEffect(state.pendingConfigExport) {
        if (state.pendingConfigExport != null) configExportLauncher.launch("snishaper-config.json")
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        // ================= 通用 =================
        SectionTitle(stringResource(R.string.settings_general))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 端口
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_port), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.settings_port_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedTextField(
                        value = state.port,
                        onValueChange = { viewModel.portChanged(it) },
                        modifier = Modifier.padding(top = 0.dp),
                        singleLine = true,
                        label = { Text(stringResource(R.string.settings_port_label)) },
                    )
                    Button(onClick = { viewModel.applyPort() }) {
                        Text(stringResource(R.string.settings_apply))
                    }
                }

                // 语言
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.settings_language_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("zh" to "中文", "en" to "English", "ru" to "Русский").forEach { (code, label) ->
                            TextButton(
                                onClick = { viewModel.languageChanged(code) },
                                enabled = state.language != code,
                            ) { Text(label) }
                        }
                    }
                }

                // 外观
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.settings_appearance_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            AppearanceMode.LIGHT to R.string.settings_appearance_light,
                            AppearanceMode.DARK to R.string.settings_appearance_dark,
                            AppearanceMode.SYSTEM to R.string.settings_appearance_system,
                        ).forEach { (mode, labelRes) ->
                            TextButton(
                                onClick = { viewModel.appearanceChanged(mode) },
                                enabled = state.appearance != mode,
                            ) { Text(stringResource(labelRes)) }
                        }
                    }
                }

                // 自动更新规则
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_auto_update_rules), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.settings_auto_update_rules_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.autoUpdateRules, onCheckedChange = { viewModel.autoUpdateRulesChanged(it) })
                }

                state.generalMessage?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ================= TUN =================
        SectionTitle(
            stringResource(R.string.settings_tun),
            trailing = {
                Text(
                    if (state.tunStatus?.running == true) stringResource(R.string.settings_tun_running)
                    else stringResource(R.string.settings_tun_off),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.tunStatus?.running == true) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_mtu), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.settings_mtu_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedTextField(
                        value = state.mtu,
                        onValueChange = { viewModel.mtuChanged(it) },
                        singleLine = true,
                        label = { Text(stringResource(R.string.settings_mtu_label)) },
                    )
                    Button(onClick = { viewModel.applyMtu() }) {
                        Text(stringResource(R.string.settings_apply))
                    }
                }
                Text(
                    stringResource(R.string.settings_tun_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ================= 证书 =================
        SectionTitle(stringResource(R.string.settings_security))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_ca_reset), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.settings_ca_reset_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(onClick = { viewModel.regenerateCert() }, enabled = !state.certBusy) {
                        Text(stringResource(R.string.settings_apply))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_ca_export), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        state.caStatus?.certPath?.takeIf { it.isNotEmpty() }?.let { path ->
                            Text(
                                path,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    TextButton(onClick = { viewModel.exportCert() }, enabled = !state.certBusy) {
                        Text(stringResource(R.string.settings_export))
                    }
                }

                // 已安装证书列表
                Text(
                    if (state.caStatus?.installed == true) stringResource(R.string.settings_certs_count, state.installedCerts.size)
                    else stringResource(R.string.settings_ca_not_installed),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state.caStatus?.installed == true) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.installedCerts.isEmpty()) {
                    Text(
                        stringResource(R.string.settings_certs_empty),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.installedCerts.forEach { cert ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(cert.subject, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    listOf(cert.storeLocation, cert.storeName, cert.thumbprint)
                                        .filter { it.isNotEmpty() }.joinToString(" / "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(
                                onClick = { viewModel.uninstallCert(cert.token) },
                                enabled = !state.certBusy,
                            ) { Text(stringResource(R.string.settings_delete), color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
                state.certMessage?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ================= IPv6 =================
        SectionTitle(
            stringResource(R.string.settings_ipv6),
            trailing = {
                TextButton(onClick = { viewModel.refreshIpv6() }, enabled = !state.ipv6Checking) {
                    Text(stringResource(R.string.settings_ipv6_refresh))
                }
            },
        )
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (state.ipv6Available) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (state.ipv6Available) stringResource(R.string.settings_ipv6_ok)
                    else stringResource(R.string.settings_ipv6_bad),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (state.ipv6Available) stringResource(R.string.settings_ipv6_ok_desc)
                    else stringResource(R.string.settings_ipv6_bad_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ================= Cloudflare 池 =================
        SectionTitle(
            stringResource(R.string.settings_cf_pool),
            trailing = {
                TextButton(onClick = { viewModel.cfHealthCheck() }, enabled = !state.cfChecking) {
                    Text(stringResource(R.string.settings_cf_health))
                }
            },
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.settings_cf_count, state.ipStats.size),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = { viewModel.fetchCfIPs() }, enabled = !state.cfRefreshing) {
                        Text(stringResource(R.string.settings_cf_fetch))
                    }
                }
                if (state.ipStats.isEmpty()) {
                    Text(
                        stringResource(R.string.settings_cf_empty),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.ipStats.forEach { stat ->
                        val latency = stat.latency.toDoubleOrNull()
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("●", color = if (latency != null && latency > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            Text(stat.ip, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            Text(
                                if (latency != null && latency > 0) "${latency.toInt()}ms" else "---",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // ================= IP 池（M3 已落） =================
        SectionTitle(stringResource(R.string.ip_pools_title))
        Text(
            stringResource(R.string.ip_pools_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.pools.isEmpty() && state.loaded) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.ip_pools_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            state.pools.forEach { pool ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(pool.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(
                                    stringResource(R.string.ip_pools_ip_count, pool.preferredIps.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            TextButton(onClick = { viewModel.forceFetch(pool.name) }, enabled = pool.name !in state.busy) {
                                Text(stringResource(R.string.ip_pools_fetch))
                            }
                            TextButton(onClick = { viewModel.healthCheck(pool.name) }, enabled = pool.name !in state.busy) {
                                Text(stringResource(R.string.ip_pools_health))
                            }
                            TextButton(onClick = { viewModel.removeInvalid(pool.name) }, enabled = pool.name !in state.busy) {
                                Text(stringResource(R.string.ip_pools_clean))
                            }
                            TextButton(onClick = { viewModel.removePool(pool.name) }, enabled = pool.name !in state.busy) {
                                Text(stringResource(R.string.ip_pools_remove), color = MaterialTheme.colorScheme.error)
                            }
                        }
                        state.poolMessages[pool.name]?.let { msg ->
                            Text(msg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.newPoolName,
                    onValueChange = { viewModel.newPoolNameChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.ip_pools_name_label)) },
                )
                OutlinedTextField(
                    value = state.newPoolIps,
                    onValueChange = { viewModel.newPoolIpsChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2, maxLines = 5,
                    label = { Text(stringResource(R.string.ip_pools_ips_label)) },
                )
                state.addMessage?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = { viewModel.addPool() }, enabled = !state.adding, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.ip_pools_add))
                }
            }
        }

        // ================= 配置导入导出 =================
        SectionTitle(stringResource(R.string.settings_transfer))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.settings_transfer_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.exportConfig() }, enabled = !state.transferBusy) {
                        Text(stringResource(R.string.settings_transfer_export))
                    }
                    Button(
                        onClick = { configImportLauncher.launch(arrayOf("application/json")) },
                        enabled = !state.transferBusy,
                    ) {
                        Text(stringResource(R.string.settings_transfer_import))
                    }
                }
                state.transferMessage?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        trailing?.invoke()
    }
}

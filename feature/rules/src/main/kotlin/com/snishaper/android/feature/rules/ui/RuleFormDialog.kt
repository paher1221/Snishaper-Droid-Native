package com.snishaper.android.feature.rules.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.snishaper.android.core.common.AppResult
import com.snishaper.android.core.data.DataGraph
import com.snishaper.android.core.model.CertVerify
import com.snishaper.android.core.model.EchProfile
import com.snishaper.android.core.model.fragOrDefault
import com.snishaper.android.core.model.Nat64Profile
import com.snishaper.android.core.model.SiteGroup
import com.snishaper.android.core.design.component.ScrollableFormDialog
import com.snishaper.android.feature.rules.R
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private data class ModeOption(val id: String, val labelRes: Int)

private val MODES = listOf(
    ModeOption("mitm", R.string.rules_mode_mitm),
    ModeOption("tls-rf", R.string.rules_mode_tls_rf),
    ModeOption("quic", R.string.rules_mode_quic),
    ModeOption("transparent", R.string.rules_mode_transparent),
    ModeOption("migration", R.string.rules_mode_migration),
)

private val DNS_MODES = listOf(
    ModeOption("", R.string.rules_dns_default),
    ModeOption("prefer_ipv4", R.string.rules_dns_prefer_ipv4),
    ModeOption("prefer_ipv6", R.string.rules_dns_prefer_ipv6),
    ModeOption("ipv4_only", R.string.rules_dns_ipv4_only),
    ModeOption("ipv6_only", R.string.rules_dns_ipv6_only),
)

private val CERT_MODES = listOf(
    ModeOption("", R.string.rules_cert_ignore),
    ModeOption("strict_real", R.string.rules_cert_strict),
    ModeOption("allow_names", R.string.rules_cert_names),
    ModeOption("allow_suffixes", R.string.rules_cert_suffixes),
    ModeOption("allow_spki", R.string.rules_cert_spki),
    ModeOption("chain_only", R.string.rules_cert_chain),
)

/** 各模式可见的设置块（对齐旧版 RuleForm 的条件逻辑）。 */
private fun showSniFake(mode: String) = mode in setOf("mitm", "quic", "migration")
private fun showEch(mode: String) = mode in setOf("mitm", "quic")
private fun showCertVerify(mode: String) = mode in setOf("mitm", "quic", "migration")
private fun showCfPool(mode: String) = mode in setOf("mitm", "transparent", "tls-rf", "quic")
private fun showFragment(mode: String) = mode == "tls-rf"

/** 新建/编辑规则表单：按模式显示不同设置；tls-rf 模式带分片参数；ECH/NAT64 档案为下拉选择。 */
@Composable
fun RuleFormDialog(initial: SiteGroup, onDismiss: () -> Unit, onSave: (SiteGroup) -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var website by remember { mutableStateOf(initial.website) }
    var mode by remember { mutableStateOf(initial.mode.ifBlank { "mitm" }) }
    var upstream by remember { mutableStateOf(initial.upstream) }
    var domains by remember { mutableStateOf(initial.domains) }
    var domainInput by remember { mutableStateOf("") }
    var dnsMode by remember { mutableStateOf(initial.dnsMode) }
    var sniFake by remember { mutableStateOf(initial.sniFake) }
    var enabled by remember { mutableStateOf(initial.enabled) }
    var useCfPool by remember { mutableStateOf(initial.useCfPool) }
    var ipPool by remember { mutableStateOf(initial.ipPool) }
    var echEnabled by remember { mutableStateOf(initial.echEnabled) }
    var echProfileId by remember { mutableStateOf(initial.echProfileId) }
    var nat64Enabled by remember { mutableStateOf(initial.nat64Enabled) }
    var nat64ProfileId by remember { mutableStateOf(initial.nat64ProfileId) }
    var certMode by remember { mutableStateOf(initial.certVerify.mode) }
    var certNames by remember { mutableStateOf(initial.certVerify.names.joinToString(", ")) }
    var certSuffixes by remember { mutableStateOf(initial.certVerify.suffixes.joinToString(", ")) }
    var certSpki by remember { mutableStateOf(initial.certVerify.spkiSha256.joinToString(", ")) }
    var certAllowUnknown by remember { mutableStateOf(initial.certVerify.allowUnknownAuthority) }

    // TLS 分片参数（仅 tls-rf 模式）；tcp_only 对应旧版：记录数 1 / 分段数 4。
    val initFrag = remember { initial.fragOrDefault() }
    var fragTcpOnly by remember { mutableStateOf(initFrag.numRecords == 1) }
    var fragNumRecords by remember { mutableStateOf(if (initFrag.numRecords == 1) 4 else initFrag.numRecords) }
    var fragNumSegments by remember { mutableStateOf(if (initFrag.numRecords == 1) 4 else initFrag.numSegments) }
    var fragIntervalMs by remember { mutableStateOf(initFrag.intervalMs) }
    var fragModMinorVer by remember { mutableStateOf(initFrag.modMinorVer) }
    var fragOob by remember { mutableStateOf(initFrag.oob) }

    // ECH / NAT64 档案下拉的数据源（进表单时加载一次）。
    var echProfiles by remember { mutableStateOf(listOf<EchProfile>()) }
    var nat64Profiles by remember { mutableStateOf(listOf<Nat64Profile>()) }
    LaunchedEffect(Unit) {
        when (val r = DataGraph.echRepository.profiles()) {
            is AppResult.Success -> echProfiles = r.value
            is AppResult.Failure -> Unit
        }
        when (val r = DataGraph.nat64Repository.profiles()) {
            is AppResult.Success -> nat64Profiles = r.value
            is AppResult.Failure -> Unit
        }
    }

    fun splitList(text: String) = text.split(',', ';', '\n').map { it.trim() }.filter { it.isNotEmpty() }

    ScrollableFormDialog(
        title = stringResource(if (initial.id.isBlank()) R.string.rules_add else R.string.rules_edit),
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.rules_save),
        dismissLabel = stringResource(R.string.rules_cancel),
        onConfirm = {
            val fragJson = if (showFragment(mode)) {
                buildJsonObject {
                    put("num_records", if (fragTcpOnly) 1 else fragNumRecords)
                    put("num_segments", if (fragTcpOnly) 4 else fragNumSegments)
                    put("interval_ms", fragIntervalMs)
                    put("mod_minor_ver", fragModMinorVer)
                    put("oob", fragOob)
                    put("oob_ex", false)
                }
            } else null
            onSave(
                initial.copy(
                    name = name.trim(),
                    website = website.trim(),
                    mode = mode,
                    upstream = upstream.trim(),
                    domains = domains,
                    dnsMode = dnsMode,
                    sniFake = sniFake.trim(),
                    enabled = enabled,
                    useCfPool = useCfPool,
                    ipPool = ipPool.trim(),
                    echEnabled = echEnabled,
                    echProfileId = echProfileId.trim(),
                    nat64Enabled = nat64Enabled,
                    nat64ProfileId = nat64ProfileId.trim(),
                    frag = fragJson,
                    certVerify = CertVerify(
                        mode = certMode,
                        names = splitList(certNames),
                        suffixes = splitList(certSuffixes),
                        spkiSha256 = splitList(certSpki),
                        allowUnknownAuthority = certAllowUnknown,
                    ),
                ),
            )
        },
    ) {
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text(stringResource(R.string.rules_field_name)) }, singleLine = true,
        )
        OutlinedTextField(
            value = website, onValueChange = { website = it },
            label = { Text(stringResource(R.string.rules_field_website)) }, singleLine = true,
        )
        Text(stringResource(R.string.rules_field_mode), style = MaterialTheme.typography.labelMedium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MODES.chunked(3).forEach { rowModes ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowModes.forEach { m ->
                        FilterChip(
                            selected = mode == m.id,
                            onClick = { mode = m.id },
                            label = { Text(stringResource(m.labelRes), style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        }
        Text(stringResource(R.string.rules_field_domains), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = domainInput, onValueChange = { domainInput = it },
                placeholder = { Text(stringResource(R.string.rules_field_domain_hint)) },
                singleLine = true, modifier = Modifier.weight(1f),
            )
            Button(onClick = {
                val parts = domainInput.split(Regex("[\\s,;]+")).filter { it.isNotBlank() }
                domains = (domains + parts).distinct()
                domainInput = ""
            }) { Text(stringResource(R.string.rules_field_domain_add)) }
        }
        domains.forEachIndexed { idx, d ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(d, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1)
                TextButton(onClick = { domains = domains.filterIndexed { i, _ -> i != idx } }) {
                    Text(stringResource(R.string.rules_field_domain_remove))
                }
            }
        }
        OutlinedTextField(
            value = upstream, onValueChange = { upstream = it },
            label = { Text(stringResource(R.string.rules_field_upstream)) }, singleLine = true,
        )
        Text(stringResource(R.string.rules_field_dns_mode), style = MaterialTheme.typography.labelMedium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DNS_MODES.chunked(3).forEach { rowModes ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowModes.forEach { m ->
                        FilterChip(
                            selected = dnsMode == m.id,
                            onClick = { dnsMode = m.id },
                            label = { Text(stringResource(m.labelRes), style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        }
        if (showSniFake(mode)) {
            OutlinedTextField(
                value = sniFake, onValueChange = { sniFake = it },
                label = { Text(stringResource(R.string.rules_field_sni_fake)) }, singleLine = true,
            )
        }
        if (showCfPool(mode)) {
            OutlinedTextField(
                value = ipPool, onValueChange = { ipPool = it },
                label = { Text(stringResource(R.string.rules_field_ip_pool)) }, singleLine = true,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.rules_field_use_cf_pool), modifier = Modifier.weight(1f))
                Switch(checked = useCfPool, onCheckedChange = { useCfPool = it })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.rules_field_enabled), modifier = Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = { enabled = it })
        }
        if (showEch(mode)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.rules_field_ech), modifier = Modifier.weight(1f))
                Switch(checked = echEnabled, onCheckedChange = { echEnabled = it })
            }
            if (echEnabled) {
                ProfileDropdown(
                    label = stringResource(R.string.rules_field_ech_profile),
                    options = listOf("" to stringResource(R.string.rules_ech_auto)) +
                        echProfiles.map { it.id to it.name },
                    selectedId = echProfileId,
                    onSelect = { echProfileId = it },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.rules_field_nat64), modifier = Modifier.weight(1f))
            Switch(checked = nat64Enabled, onCheckedChange = { nat64Enabled = it })
        }
        if (nat64Enabled) {
            ProfileDropdown(
                label = stringResource(R.string.rules_field_nat64_profile),
                options = nat64Profiles.map { it.id to it.name },
                selectedId = nat64ProfileId,
                onSelect = { nat64ProfileId = it },
            )
        }
        if (showFragment(mode)) {
            Text(stringResource(R.string.rules_frag_title), style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.rules_frag_tcp_only), modifier = Modifier.weight(1f))
                Switch(
                    checked = fragTcpOnly,
                    onCheckedChange = {
                        fragTcpOnly = it
                        if (it) {
                            fragNumRecords = 1; fragNumSegments = 4
                        } else {
                            fragNumRecords = 4; fragNumSegments = 1
                        }
                    },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = (if (fragTcpOnly) 1 else fragNumRecords).toString(),
                    onValueChange = { fragNumRecords = it.toIntOrNull() ?: 4 },
                    label = { Text(stringResource(R.string.rules_frag_records)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.weight(1f), enabled = !fragTcpOnly,
                )
                OutlinedTextField(
                    value = (if (fragTcpOnly) 4 else fragNumSegments).toString(),
                    onValueChange = { fragNumSegments = it.toIntOrNull() ?: 1 },
                    label = { Text(stringResource(R.string.rules_frag_segments)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.weight(1f), enabled = !fragTcpOnly,
                )
            }
            OutlinedTextField(
                value = fragIntervalMs.toString(),
                onValueChange = { fragIntervalMs = it.toIntOrNull() ?: 0 },
                label = { Text(stringResource(R.string.rules_frag_interval)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.rules_frag_minorver), modifier = Modifier.weight(1f))
                Switch(checked = fragModMinorVer, onCheckedChange = { fragModMinorVer = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.rules_frag_oob), modifier = Modifier.weight(1f))
                Switch(checked = fragOob, onCheckedChange = { fragOob = it })
            }
            TextButton(onClick = {
                fragTcpOnly = false; fragNumRecords = 4; fragNumSegments = 1
                fragIntervalMs = 50; fragModMinorVer = true; fragOob = false
            }) { Text(stringResource(R.string.rules_frag_reset)) }
        }
        if (showCertVerify(mode)) {
            Text(stringResource(R.string.rules_field_cert_verify), style = MaterialTheme.typography.labelMedium)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CERT_MODES.chunked(3).forEach { rowModes ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowModes.forEach { m ->
                            FilterChip(
                                selected = certMode == m.id,
                                onClick = { certMode = m.id },
                                label = { Text(stringResource(m.labelRes), style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }
            }
            if (certMode == "allow_names") {
                OutlinedTextField(
                    value = certNames, onValueChange = { certNames = it },
                    label = { Text(stringResource(R.string.rules_field_cert_names)) }, singleLine = true,
                )
            }
            if (certMode == "allow_suffixes") {
                OutlinedTextField(
                    value = certSuffixes, onValueChange = { certSuffixes = it },
                    label = { Text(stringResource(R.string.rules_field_cert_suffixes)) }, singleLine = true,
                )
            }
            if (certMode == "allow_spki") {
                OutlinedTextField(
                    value = certSpki, onValueChange = { certSpki = it },
                    label = { Text(stringResource(R.string.rules_field_cert_spki)) }, singleLine = true,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.rules_field_cert_unknown), modifier = Modifier.weight(1f))
                Switch(checked = certAllowUnknown, onCheckedChange = { certAllowUnknown = it })
            }
        }
    }
}

/** 档案下拉选择器（显示名称，保存 ID）。 */
@Composable
private fun ProfileDropdown(label: String, options: List<Pair<String, String>>, selectedId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.first == selectedId }?.second ?: selectedId
    Box {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth()) {
            if (options.isEmpty()) {
                DropdownMenuItem(text = { Text("—") }, onClick = { expanded = false })
            } else {
                options.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name.ifBlank { id }, maxLines = 1) },
                        onClick = { onSelect(id); expanded = false },
                    )
                }
            }
        }
        // 透明覆盖层：点击整行展开菜单。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { expanded = true },
        )
    }
}

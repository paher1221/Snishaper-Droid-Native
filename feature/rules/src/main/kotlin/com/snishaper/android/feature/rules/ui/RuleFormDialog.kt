package com.snishaper.android.feature.rules.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.snishaper.android.core.model.CertVerify
import com.snishaper.android.core.model.SiteGroup
import com.snishaper.android.feature.rules.R

private val MODES = listOf("mitm", "tls-rf", "quic", "transparent", "migration")
private val DNS_MODES = listOf("", "prefer_ipv4", "allow_suffixes", "allow_spki", "chain_only")
private val CERT_MODES = listOf("", "strict_real", "allow_names", "allow_suffixes", "allow_spki", "chain_only")

/** 新建/编辑规则表单：覆盖旧版 RuleForm 的核心字段；ECH/NAT64 档案选择器随 M3 换成下拉。 */
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

    fun splitList(text: String) = text.split(',', ';', '\n').map { it.trim() }.filter { it.isNotEmpty() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.id.isBlank()) R.string.rules_add else R.string.rules_edit)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                    selected = mode == m,
                                    onClick = { mode = m },
                                    label = { Text(m.uppercase(), style = MaterialTheme.typography.labelSmall) },
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
                                    selected = dnsMode == m,
                                    onClick = { dnsMode = m },
                                    label = { Text(m.ifBlank { "default" }, style = MaterialTheme.typography.labelSmall) },
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = sniFake, onValueChange = { sniFake = it },
                    label = { Text(stringResource(R.string.rules_field_sni_fake)) }, singleLine = true,
                )
                OutlinedTextField(
                    value = ipPool, onValueChange = { ipPool = it },
                    label = { Text(stringResource(R.string.rules_field_ip_pool)) }, singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.rules_field_use_cf_pool), modifier = Modifier.weight(1f))
                    Switch(checked = useCfPool, onCheckedChange = { useCfPool = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.rules_field_enabled), modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.rules_field_ech), modifier = Modifier.weight(1f))
                    Switch(checked = echEnabled, onCheckedChange = { echEnabled = it })
                }
                if (echEnabled) {
                    OutlinedTextField(
                        value = echProfileId, onValueChange = { echProfileId = it },
                        label = { Text(stringResource(R.string.rules_field_ech_profile)) }, singleLine = true,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.rules_field_nat64), modifier = Modifier.weight(1f))
                    Switch(checked = nat64Enabled, onCheckedChange = { nat64Enabled = it })
                }
                if (nat64Enabled) {
                    OutlinedTextField(
                        value = nat64ProfileId, onValueChange = { nat64ProfileId = it },
                        label = { Text(stringResource(R.string.rules_field_nat64_profile)) }, singleLine = true,
                    )
                }
                Text(stringResource(R.string.rules_field_cert_verify), style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CERT_MODES.chunked(3).forEach { rowModes ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowModes.forEach { m ->
                                FilterChip(
                                    selected = certMode == m,
                                    onClick = { certMode = m },
                                    label = { Text(m.ifBlank { "ignore" }, style = MaterialTheme.typography.labelSmall) },
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = certNames, onValueChange = { certNames = it },
                    label = { Text(stringResource(R.string.rules_field_cert_names)) }, singleLine = true,
                )
                OutlinedTextField(
                    value = certSuffixes, onValueChange = { certSuffixes = it },
                    label = { Text(stringResource(R.string.rules_field_cert_suffixes)) }, singleLine = true,
                )
                OutlinedTextField(
                    value = certSpki, onValueChange = { certSpki = it },
                    label = { Text(stringResource(R.string.rules_field_cert_spki)) }, singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.rules_field_cert_unknown), modifier = Modifier.weight(1f))
                    Switch(checked = certAllowUnknown, onCheckedChange = { certAllowUnknown = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
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
                        certVerify = CertVerify(
                            mode = certMode,
                            names = splitList(certNames),
                            suffixes = splitList(certSuffixes),
                            spkiSha256 = splitList(certSpki),
                            allowUnknownAuthority = certAllowUnknown,
                        ),
                    ),
                )
            }) { Text(stringResource(R.string.rules_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.rules_cancel)) }
        },
    )
}

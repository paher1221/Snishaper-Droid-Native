package com.snishaper.android.feature.proxies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snishaper.android.core.model.EchProfile
import com.snishaper.android.core.model.Nat64Profile
import com.snishaper.android.feature.proxies.vm.ProxiesViewModel

@Composable
fun ProxiesScreen(modifier: Modifier = Modifier, viewModel: ProxiesViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showEchForm by remember { mutableStateOf(false) }
    var editingEch by remember { mutableStateOf<EchProfile?>(null) }
    var pendingEchDelete by remember { mutableStateOf<EchProfile?>(null) }

    var showNat64Form by remember { mutableStateOf(false) }
    var editingNat64 by remember { mutableStateOf<Nat64Profile?>(null) }
    var pendingNat64Delete by remember { mutableStateOf<Nat64Profile?>(null) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ---- 迁移服务器 ----
        SectionHeader(stringResource(R.string.proxies_migration_title))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.proxies_migration_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.migrationInput,
                        onValueChange = { viewModel.migrationInputChanged(it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.proxies_migration_hint)) },
                    )
                    OutlinedButton(onClick = { viewModel.saveMigration() }, enabled = !state.savingMigration) {
                        Text(stringResource(R.string.proxies_save))
                    }
                    OutlinedButton(onClick = { viewModel.testMigration() }) {
                        Text(stringResource(R.string.proxies_test_connect))
                    }
                }
                state.migrationMessage?.let { msg ->
                    Text(
                        msg,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.migrationOk == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        // ---- ECH 档案 ----
        SectionHeader(stringResource(R.string.proxies_ech_management)) {
            OutlinedButton(onClick = { editingEch = null; showEchForm = true }) {
                Text(stringResource(R.string.proxies_add_ech))
            }
        }
        if (state.echProfiles.isEmpty()) {
            EmptyCard(stringResource(R.string.proxies_no_ech))
        } else {
            state.echProfiles.forEach { profile ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(profile.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(
                                if (profile.autoUpdate) stringResource(R.string.proxies_auto_sync) else stringResource(R.string.proxies_static_config),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { pendingEchDelete = profile }) {
                            Text(stringResource(R.string.proxies_delete), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // ---- NAT64 档案 ----
        SectionHeader(stringResource(R.string.proxies_nat64_management)) {
            OutlinedButton(onClick = { editingNat64 = null; showNat64Form = true }) {
                Text(stringResource(R.string.proxies_add_nat64))
            }
        }
        if (state.nat64Profiles.isEmpty()) {
            EmptyCard(stringResource(R.string.proxies_no_nat64))
        } else {
            state.nat64Profiles.forEach { profile ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(profile.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(profile.prefix, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            state.nat64TestResults[profile.id]?.let { result ->
                                Text(result, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { viewModel.testNat64(profile) }) {
                                Text(stringResource(R.string.proxies_test))
                            }
                            TextButton(onClick = { editingNat64 = profile; showNat64Form = true }) {
                                Text(stringResource(R.string.proxies_edit))
                            }
                            TextButton(onClick = { pendingNat64Delete = profile }) {
                                Text(stringResource(R.string.proxies_delete), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- ECH 表单 ----
    if (showEchForm) {
        EchFormDialog(
            initial = editingEch,
            onDismiss = { showEchForm = false },
            onSubmit = { profile ->
                showEchForm = false
                viewModel.saveEch(profile) {}
            },
        )
    }

    pendingEchDelete?.let { profile ->
        ConfirmDialog(
            title = stringResource(R.string.proxies_delete_ech),
            text = stringResource(R.string.proxies_delete_confirm, profile.name),
            onConfirm = { viewModel.deleteEch(profile.id); pendingEchDelete = null },
            onDismiss = { pendingEchDelete = null },
        )
    }

    // ---- NAT64 表单 ----
    if (showNat64Form) {
        Nat64FormDialog(
            initial = editingNat64,
            onDismiss = { showNat64Form = false },
            onSubmit = { profile ->
                val isNew = editingNat64 == null
                showNat64Form = false
                viewModel.saveNat64(profile, isNew) {}
            },
        )
    }

    pendingNat64Delete?.let { profile ->
        ConfirmDialog(
            title = stringResource(R.string.proxies_delete_nat64),
            text = stringResource(R.string.proxies_delete_nat64_confirm, profile.name),
            onConfirm = { viewModel.deleteNat64(profile.id); pendingNat64Delete = null },
            onDismiss = { pendingNat64Delete = null },
        )
    }
}

@Composable
private fun SectionHeader(title: String, action: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

@Composable
private fun EmptyCard(text: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.proxies_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.proxies_cancel)) }
        },
    )
}

@Composable
private fun EchFormDialog(
    initial: EchProfile?,
    onDismiss: () -> Unit,
    onSubmit: (EchProfile) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var discoveryDomain by remember { mutableStateOf(initial?.discoveryDomain ?: "") }
    var dohUpstream by remember { mutableStateOf(initial?.dohUpstream ?: "https://cloudflare-dns.com/dns-query") }
    var config by remember { mutableStateOf(initial?.config ?: "") }
    var autoUpdate by remember { mutableStateOf(initial?.autoUpdate ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.proxies_add_ech else R.string.proxies_edit_ech)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.ech_name)) }, singleLine = true)
                OutlinedTextField(value = discoveryDomain, onValueChange = { discoveryDomain = it }, label = { Text(stringResource(R.string.ech_discovery_domain)) }, singleLine = true)
                OutlinedTextField(value = dohUpstream, onValueChange = { dohUpstream = it }, label = { Text(stringResource(R.string.ech_doh_upstream)) }, singleLine = true)
                OutlinedTextField(
                    value = config, onValueChange = { config = it },
                    label = { Text(stringResource(R.string.ech_config)) },
                    minLines = 2, maxLines = 5,
                )
                androidx.compose.material3.FilterChip(
                    selected = autoUpdate,
                    onClick = { autoUpdate = !autoUpdate },
                    label = { Text(stringResource(R.string.ech_auto_update)) },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        EchProfile(
                            id = initial?.id ?: "",
                            name = name.trim(),
                            discoveryDomain = discoveryDomain.trim(),
                            dohUpstream = dohUpstream.trim(),
                            config = config.trim(),
                            autoUpdate = autoUpdate,
                        ),
                    )
                },
                enabled = name.isNotBlank() && discoveryDomain.isNotBlank(),
            ) { Text(stringResource(R.string.proxies_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.proxies_cancel)) }
        },
    )
}

@Composable
private fun Nat64FormDialog(
    initial: Nat64Profile?,
    onDismiss: () -> Unit,
    onSubmit: (Nat64Profile) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var prefix by remember { mutableStateOf(initial?.prefix ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.proxies_add_nat64 else R.string.proxies_edit_nat64)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.nat64_name)) }, singleLine = true)
                OutlinedTextField(value = prefix, onValueChange = { prefix = it }, label = { Text(stringResource(R.string.nat64_prefix)) }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(Nat64Profile(id = initial?.id ?: "", name = name.trim(), prefix = prefix.trim())) },
                enabled = name.isNotBlank() && prefix.isNotBlank(),
            ) { Text(stringResource(R.string.proxies_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.proxies_cancel)) }
        },
    )
}

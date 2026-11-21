package com.snishaper.android.feature.rules.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import com.snishaper.android.core.model.SiteGroup
import com.snishaper.android.feature.rules.R
import com.snishaper.android.feature.rules.vm.RulesViewModel

private fun effectiveMode(group: SiteGroup): String {
    val mode = group.mode.trim().lowercase()
    return when (mode) {
        "quic" -> "QUIC"
        "tls-rf" -> "TLS-RF"
        "mitm" -> "MITM"
        "transparent" -> "TRANSPARENT"
        "migration" -> "MIGRATION"
        else -> if (mode.isEmpty()) "DIRECT" else mode.uppercase()
    }
}

private fun effectiveUpstream(group: SiteGroup): String {
    val upstream = group.upstream.trim()
    if (upstream.isNotEmpty() && !upstream.equals("DIRECT", true)) return upstream
    if (group.useCfPool) return "CF IP Pool"
    return "DoH"
}

@Composable
fun RulesScreen(modifier: Modifier = Modifier, viewModel: RulesViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var search by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("ALL") }
    var sourceFilter by remember { mutableStateOf("all") }

    val filtered = state.groups.filter { group ->
        val matchesSearch = "${group.name}${group.website}${group.domains.joinToString("")}"
            .lowercase().contains(search.lowercase())
        val matchesMode = filterMode == "ALL" || effectiveMode(group) == filterMode
        val layer = viewModel.layerOf(group.id)
        val matchesSource = sourceFilter == "all" ||
            (sourceFilter == "default" && layer.isEmpty()) ||
            (sourceFilter == "override" && layer == "override") ||
            (sourceFilter == "user" && layer == "user")
        matchesSearch && matchesMode && matchesSource
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.rules_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { viewModel.openExplain() }) {
                Text(stringResource(R.string.rules_explain))
            }
            Button(onClick = { viewModel.startCreate() }) {
                Text(stringResource(R.string.rules_add))
            }
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.rules_search)) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("ALL", "MITM", "TLS-RF", "QUIC", "TRANSPARENT", "MIGRATION").forEach { mode ->
                FilterChip(selected = filterMode == mode, label = mode, onClick = { filterMode = mode })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "all" to stringResource(R.string.rules_source_all),
                "default" to stringResource(R.string.rules_source_default),
                "override" to stringResource(R.string.rules_source_override),
                "user" to stringResource(R.string.rules_source_user),
            ).forEach { (id, label) ->
                FilterChip(selected = sourceFilter == id, label = label, onClick = { sourceFilter = id })
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { group ->
                RuleCard(
                    group = group,
                    layer = viewModel.layerOf(group.id),
                    onEdit = { viewModel.startEdit(group) },
                    onDelete = { viewModel.requestDelete(group) },
                    onRevert = { viewModel.requestRevert(group) },
                )
            }
        }
    }

    state.editing?.let { editing ->
        RuleFormDialog(
            initial = editing,
            onDismiss = viewModel::cancelEdit,
            onSave = viewModel::save,
        )
    }
    state.deleting?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.rules_delete_title)) },
            text = { Text(stringResource(R.string.rules_delete_text, target.name.ifBlank { target.id })) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text(stringResource(R.string.rules_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) { Text(stringResource(R.string.rules_cancel)) }
            },
        )
    }
    state.reverting?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRevert,
            title = { Text(stringResource(R.string.rules_revert_title)) },
            text = { Text(stringResource(R.string.rules_revert_text, target.name.ifBlank { target.id })) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRevert) { Text(stringResource(R.string.rules_revert_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRevert) { Text(stringResource(R.string.rules_cancel)) }
            },
        )
    }
    if (state.explainOpen) {
        ExplainDialog(state = state, viewModel = viewModel)
    }
}

@Composable
private fun FilterChip(selected: Boolean, label: String, onClick: () -> Unit) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
    )
}

@Composable
private fun RuleCard(
    group: SiteGroup,
    layer: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRevert: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = group.name.ifBlank { group.id },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                if (layer == "override") {
                    Text(
                        text = stringResource(R.string.rules_override_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
                if (layer == "user") {
                    Text(
                        text = stringResource(R.string.rules_custom_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }
            if (group.website.isNotBlank()) {
                Text(
                    text = group.website,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Text(
                text = group.domains.take(4).joinToString("  ") + if (group.domains.size > 4) "  +${group.domains.size - 4}" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = effectiveMode(group),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = effectiveUpstream(group),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (layer == "override") {
                    TextButton(onClick = onRevert) { Text(stringResource(R.string.rules_revert)) }
                }
                TextButton(onClick = onEdit) { Text(stringResource(R.string.rules_edit)) }
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.rules_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ExplainDialog(state: com.snishaper.android.feature.rules.vm.RulesUiState, viewModel: RulesViewModel) {
    var host by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = viewModel::closeExplain,
        title = { Text(stringResource(R.string.rules_explain)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = host, onValueChange = { host = it },
                    label = { Text(stringResource(R.string.rules_explain_host)) }, singleLine = true,
                )
                OutlinedTextField(
                    value = ip, onValueChange = { ip = it },
                    label = { Text(stringResource(R.string.rules_explain_ip)) }, singleLine = true,
                )
                Button(onClick = { viewModel.explain(host, ip) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.rules_explain_run))
                }
                state.explainResult?.let { result ->
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = viewModel::closeExplain) { Text(stringResource(R.string.rules_cancel)) }
        },
    )
}

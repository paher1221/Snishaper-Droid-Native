package com.snishaper.android.feature.dns

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snishaper.android.core.model.DnsNode
import com.snishaper.android.feature.dns.vm.DnsViewModel

@Composable
fun DnsScreen(modifier: Modifier = Modifier, viewModel: DnsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DnsNode?>(null) }
    var pendingDelete by remember { mutableStateOf<DnsNode?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.dns_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = { viewModel.testAll() }) {
                Text(stringResource(R.string.dns_test_all))
            }
            Button(onClick = { editing = null; showForm = true }) {
                Text(stringResource(R.string.dns_add_node))
            }
        }

        if (state.nodes.isEmpty() && state.loaded) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.dns_no_nodes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(state.nodes, key = { _, n -> n.id }) { index, node ->
                    DnsNodeCard(
                        node = node,
                        index = index,
                        total = state.nodes.size,
                        testResult = state.testResults[node.id],
                        isTesting = node.id in state.testingIds,
                        onEdit = { editing = it; showForm = true },
                        onDelete = { pendingDelete = it },
                        onMoveUp = { id -> viewModel.move(id, index, index - 1) },
                        onMoveDown = { id -> viewModel.move(id, index, index + 1) },
                        onTest = { viewModel.test(it) },
                    )
                }
            }
        }
    }

    if (showForm) {
        DnsNodeFormDialog(
            initial = editing,
            onDismiss = { showForm = false },
            onSubmit = { node ->
                showForm = false
                viewModel.save(node, isNew = editing == null) {}
            },
        )
    }

    pendingDelete?.let { node ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.dns_delete_node)) },
            text = { Text(stringResource(R.string.dns_delete_hint, node.name.ifBlank { node.id })) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(node.id); pendingDelete = null }) {
                    Text(stringResource(R.string.dns_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.dns_cancel))
                }
            },
        )
    }
}

@Composable
private fun DnsNodeCard(
    node: DnsNode,
    index: Int,
    total: Int,
    testResult: com.snishaper.android.core.model.DnsTestResult?,
    isTesting: Boolean,
    onEdit: (DnsNode) -> Unit,
    onDelete: (DnsNode) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onTest: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = { onMoveUp(node.id) }, enabled = index > 0, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text("↑")
                    }
                    Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { onMoveDown(node.id) }, enabled = index < total - 1, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text("↓")
                    }
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = node.name.ifBlank { "—" },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        if (node.enabled) {
                            Text("●", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text(
                        text = node.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (node.echEnabled) TagChip("ECH")
                        if (node.quic) TagChip("QUIC")
                        if (node.sni.isNotBlank()) TagChip("SNI: ${node.sni}")
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    when {
                        isTesting -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        testResult != null && testResult.success -> {
                            Text(testResult.latency, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            testResult.ips.firstOrNull()?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                        testResult != null -> Text(
                            testResult.error.ifBlank { stringResource(R.string.dns_failed) },
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onTest(node.id) }, enabled = !isTesting) {
                    Text(stringResource(R.string.dns_test))
                }
                TextButton(onClick = { onEdit(node) }) {
                    Text(stringResource(R.string.dns_edit))
                }
                TextButton(onClick = { onDelete(node) }) {
                    Text(stringResource(R.string.dns_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun TagChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                MaterialTheme.shapes.small,
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun DnsNodeFormDialog(
    initial: DnsNode?,
    onDismiss: () -> Unit,
    onSubmit: (DnsNode) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var url by remember { mutableStateOf(initial?.url ?: "") }
    var sni by remember { mutableStateOf(initial?.sni ?: "") }
    var ips by remember { mutableStateOf(initial?.ips?.joinToString("\n") ?: "") }
    var echEnabled by remember { mutableStateOf(initial?.echEnabled ?: false) }
    var quic by remember { mutableStateOf(initial?.quic ?: false) }
    var enabled by remember { mutableStateOf(initial?.enabled ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.dns_add_node else R.string.dns_edit_node)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.dns_node_name)) }, singleLine = true)
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text(stringResource(R.string.dns_doh_url)) }, singleLine = true)
                OutlinedTextField(value = sni, onValueChange = { sni = it }, label = { Text(stringResource(R.string.dns_sni_fake)) }, singleLine = true)
                OutlinedTextField(
                    value = ips, onValueChange = { ips = it },
                    label = { Text(stringResource(R.string.dns_bootstrap_ips)) },
                    minLines = 2, maxLines = 4,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = enabled, onClick = { enabled = !enabled }, label = { Text(stringResource(R.string.dns_enabled)) })
                    FilterChip(selected = echEnabled, onClick = { echEnabled = !echEnabled }, label = { Text("ECH") })
                    FilterChip(selected = quic, onClick = { quic = !quic }, label = { Text("QUIC") })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSubmit(
                    DnsNode(
                        id = initial?.id ?: "",
                        name = name.trim(),
                        url = url.trim(),
                        sni = sni.trim(),
                        ips = ips.lines().map { it.trim() }.filter { it.isNotEmpty() },
                        echEnabled = echEnabled,
                        quic = quic,
                        enabled = enabled,
                    ),
                )
            }, enabled = name.isNotBlank() && url.isNotBlank()) {
                Text(stringResource(R.string.dns_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dns_cancel)) }
        },
    )
}

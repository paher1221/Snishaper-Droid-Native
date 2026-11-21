package com.snishaper.android.feature.connections.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.snishaper.android.core.model.ConnectionInfo
import com.snishaper.android.feature.connections.R
import com.snishaper.android.feature.connections.vm.ConnectionsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun formatBytes(n: Long): String {
    if (n <= 0) return "0 B"
    val units = listOf("B", "KiB", "MiB", "GiB")
    var value = n.toDouble()
    var index = 0
    while (value >= 1024 && index < units.size - 1) {
        value /= 1024
        index += 1
    }
    return "%.1f %s".format(value, units[index])
}

/** RFC3339 手动解析（minSdk 24 无 java.time，未启用脱糖）。 */
private fun ageLabel(startedAt: String): String {
    val normalized = startedAt.substringBefore('.').substringBefore('Z').substringBefore('+')
    val parsed = runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(normalized)
    }.getOrNull() ?: return "—"
    val secs = ((Date().time - parsed.time) / 1000).coerceAtLeast(0)
    return when {
        secs < 60 -> "${secs}s"
        secs < 3600 -> "${secs / 60}m"
        else -> "${secs / 3600}h"
    }
}

@Composable
fun ConnectionsScreen(modifier: Modifier = Modifier, viewModel: ConnectionsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var search by remember { mutableStateOf("") }

    val query = search.trim().lowercase()
    val filtered = state.connections.filter { conn ->
        query.isEmpty() || "${conn.host} ${conn.dstAddr} ${conn.rule} ${conn.mode} ${conn.protocol}"
            .lowercase().contains(query)
    }
    val totalUp = state.connections.sumOf { it.bytesUp }
    val totalDown = state.connections.sumOf { it.bytesDown }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.connections_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.connections_count, state.connections.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            if (state.connections.isNotEmpty()) {
                Text(
                    text = "↑ ${formatBytes(totalUp)} · ↓ ${formatBytes(totalDown)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.connections_search)) },
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { conn ->
                ConnectionCard(conn)
            }
        }
    }
}

@Composable
private fun ConnectionCard(conn: ConnectionInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conn.host.ifBlank { conn.dstAddr },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Text(
                    text = conn.protocol.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = buildString {
                        append(conn.mode.uppercase())
                        if (conn.rule.isNotBlank()) append(" · ${conn.rule}")
                        if (conn.ruleDomain.isNotBlank()) append(" · ${conn.ruleDomain}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Text(
                    text = ageLabel(conn.startedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "↑ ${formatBytes(conn.bytesUp)} · ↓ ${formatBytes(conn.bytesDown)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

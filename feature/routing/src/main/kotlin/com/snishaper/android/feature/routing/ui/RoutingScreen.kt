package com.snishaper.android.feature.routing.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snishaper.android.core.model.RouteEvent
import com.snishaper.android.feature.routing.R
import com.snishaper.android.feature.routing.vm.RoutingViewModel

private fun modeColor(mode: String): androidx.compose.ui.graphics.Color {
    val lower = mode.lowercase()
    return when {
        lower.contains("direct") || lower.contains("tcp") -> androidx.compose.ui.graphics.Color(0xFF6B7280)
        lower.contains("transparent") -> androidx.compose.ui.graphics.Color(0xFFEF4444)
        lower.contains("mitm") || lower.contains("proxy") -> androidx.compose.ui.graphics.Color(0xFFF59E0B)
        else -> androidx.compose.ui.graphics.Color(0xFF10B981)
    }
}

@Composable
fun RoutingScreen(modifier: Modifier = Modifier, viewModel: RoutingViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.routing_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(
                        if (state.status.enabled) R.string.routing_enabled else R.string.routing_disabled,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (state.status.enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.routing_domain_count, state.status.domainCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = stringResource(R.string.routing_traffic_flow),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.flows.isEmpty()) {
            Text(
                text = stringResource(R.string.routing_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.flows, key = { "${it.timestamp}-${it.domain}" }) { flow ->
                    FlowChip(flow)
                }
            }
        }
    }
}

@Composable
private fun FlowChip(flow: RouteEvent) {
    val color = modeColor(flow.mode)
    AssistChip(
        onClick = {},
        label = {
            Column {
                Text(
                    text = flow.domain,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = flow.mode.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

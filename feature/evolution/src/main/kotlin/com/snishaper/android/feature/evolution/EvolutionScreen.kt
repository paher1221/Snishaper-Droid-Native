package com.snishaper.android.feature.evolution

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snishaper.android.core.model.EvolutionResult
import com.snishaper.android.core.model.EvolutionStep
import com.snishaper.android.core.model.EvolutionTempRule
import com.snishaper.android.feature.evolution.vm.EvolutionUiState
import com.snishaper.android.feature.evolution.vm.EvolutionViewModel

/**
 * 演进模式：测试 / 临时规则 / 结果三个页签（对齐旧版 Evolution.tsx 三 Tab）。
 * 测试流程：直连 → TCPing → 域前置 / TLS 分片 / ECH / QUIC，命中方法可一键转正式规则。
 */

private fun methodLabel(method: String?): String = when (method) {
    "direct" -> "直连"
    "domain_fronting" -> "域前置"
    "tls_fragment" -> "TLS 分片"
    "ech" -> "ECH"
    "quic" -> "QUIC"
    else -> method ?: "不可达"
}

private fun stepLabel(stepName: String): String = when (stepName.replace('-', '_')) {
    "direct" -> "直连"
    "tcping" -> "TCPing"
    "domain_fronting" -> "域前置"
    "tls_fragment" -> "TLS 分片"
    "ech" -> "ECH"
    "quic" -> "QUIC"
    else -> stepName.replace('_', ' ')
}

private fun methodColor(method: String?): Color = when (method) {
    "direct" -> Color(0xFF22C55E)
    "domain_fronting" -> Color(0xFFEA580C)
    "tls_fragment" -> Color(0xFF1976D2)
    "ech" -> Color(0xFF9C27B0)
    "quic" -> Color(0xFF06B6D4)
    else -> Color(0xFFEF4444)
}

@Composable
fun EvolutionScreen(modifier: Modifier = Modifier, viewModel: EvolutionViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tabs = listOf(
        stringResource(R.string.evolution_tab_test),
        stringResource(R.string.evolution_tab_rules),
        stringResource(R.string.evolution_tab_results),
    )

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.evolution_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.evolution_subtitle),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TabRow(selectedTabIndex = state.activeTab) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = state.activeTab == index,
                    onClick = { viewModel.tabChanged(index) },
                    text = {
                        val count = when (index) {
                            1 -> state.tempRules.size
                            2 -> state.results.size
                            else -> 0
                        }
                        Text(if (count > 0) "$label ($count)" else label)
                    },
                )
            }
        }

        when (state.activeTab) {
            0 -> TestTab(state, viewModel)
            1 -> RulesTab(state, viewModel)
            else -> ResultsTab(state)
        }
    }

    state.pendingRule?.let { rule ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelApplyRule() },
            title = { Text(stringResource(R.string.evolution_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.evolution_confirm_desc))
                    Card {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(rule.name, fontWeight = FontWeight.Bold)
                            Text(
                                "${rule.domain} · ${methodLabel(rule.method)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmApplyRule() }) {
                    Text(stringResource(R.string.evolution_confirm_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelApplyRule() }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun TestTab(state: EvolutionUiState, viewModel: EvolutionViewModel) {
    val domainCount = state.domainsText.lines().count { it.trim().isNotEmpty() }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.evolution_domain_list),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            stringResource(R.string.evolution_domain_count, domainCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedTextField(
                        value = state.domainsText,
                        onValueChange = { viewModel.domainsChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4, maxLines = 8,
                        enabled = !state.running,
                        placeholder = { Text(stringResource(R.string.evolution_domains_hint)) },
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Switch(
                                checked = state.enableIpv6,
                                onCheckedChange = { viewModel.enableIpv6Changed(it) },
                                enabled = !state.running && state.ipv6Available,
                            )
                            Text(
                                stringResource(R.string.evolution_enable_ipv6) +
                                    if (!state.ipv6Available) stringResource(R.string.evolution_ipv6_warn) else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (state.running) {
                            OutlinedButton(onClick = { viewModel.stopTest() }) {
                                Text(stringResource(R.string.evolution_stop))
                            }
                        } else {
                            Button(
                                onClick = { viewModel.startTest() },
                                enabled = !state.starting && state.domainsText.isNotBlank(),
                            ) {
                                Text(stringResource(R.string.evolution_start))
                            }
                        }
                    }

                    if (state.running) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.evolution_progress),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${state.progressCurrent} / ${state.progressTotal}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (state.progressTotal > 0) {
                                LinearProgressIndicator(
                                    progress = { state.progressCurrent.toFloat() / state.progressTotal },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    state.message?.let { msg ->
                        Text(
                            evolutionMessage(msg),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.evolution_test_flow),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "直连 → TCPing → 域前置 / TLS 分片 / ECH / QUIC",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.evolution_test_flow_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RulesTab(state: EvolutionUiState, viewModel: EvolutionViewModel) {
    if (state.tempRules.isEmpty()) {
        EmptyHint(stringResource(R.string.evolution_empty_rules), stringResource(R.string.evolution_empty_rules_desc))
        return
    }
    val applied = state.tempRules.count { it.isApplied }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                stringResource(R.string.evolution_applied_count, applied, state.tempRules.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(state.tempRules, key = { it.id }) { rule ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MethodBadge(rule.method)
                        Text(rule.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                        if (rule.isApplied) {
                            Text(
                                stringResource(R.string.evolution_applied),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            TextButton(onClick = { viewModel.requestApplyRule(rule) }) {
                                Text(stringResource(R.string.evolution_apply))
                            }
                        }
                    }
                    Text(
                        listOfNotNull(
                            rule.domain,
                            rule.sniFake?.takeIf { it.isNotEmpty() }?.let { "SNI: $it" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultsTab(state: EvolutionUiState) {
    if (state.results.isEmpty()) {
        EmptyHint(stringResource(R.string.evolution_empty_results), stringResource(R.string.evolution_empty_results_desc))
        return
    }
    val success = state.results.count { it.reachable }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                stringResource(R.string.evolution_success_fail, success, state.results.size - success),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(state.results, key = { it.domain + it.timestamp }) { result ->
            ResultCard(result)
        }
    }
}

@Composable
private fun ResultCard(result: EvolutionResult) {
    val container = if (result.reachable) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MethodBadge(result.method.takeIf { result.reachable })
                Text(result.domain, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                Text(
                    stringResource(if (result.reachable) R.string.evolution_reachable else R.string.evolution_unreachable),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (result.reachable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
            if (result.reachable) {
                Text(
                    listOfNotNull(
                        stringResource(R.string.evolution_delay, result.delayMs),
                        result.bestIp?.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.evolution_best_ip, it) },
                        if (result.isCloudflare) stringResource(R.string.evolution_cloudflare) else null,
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                result.error?.takeIf { it.isNotEmpty() }?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
            if (result.stepResults.isNotEmpty()) {
                StepChips(result.stepResults)
            }
        }
    }
}

@Composable
private fun StepChips(steps: List<EvolutionStep>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        steps.chunked(3).forEach { rowSteps ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowSteps.forEach { step ->
                    val color = if (step.success) Color(0xFF22C55E) else Color(0xFFEF4444)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(color.copy(alpha = 0.1f), RoundedCornerShape(999.dp))
                            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text("●", color = color, fontSize = 8.sp)
                        Text(
                            stepLabel(step.stepName),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MethodBadge(method: String?) {
    val color = methodColor(method)
    Text(
        methodLabel(method),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(999.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun EmptyHint(title: String, desc: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun evolutionMessage(key: String): String = when (key) {
    "empty_domains" -> stringResource(R.string.evolution_err_empty)
    "applied" -> stringResource(R.string.evolution_applied_ok)
    else -> key
}

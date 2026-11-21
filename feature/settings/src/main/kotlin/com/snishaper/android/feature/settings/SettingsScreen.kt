package com.snishaper.android.feature.settings

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snishaper.android.feature.settings.vm.SettingsViewModel

/**
 * 设置页：M3 先落 IP 池区块（与旧版 Settings 页的 IPPoolsSection 一致），
 * 其余设置项随后续里程碑补齐。
 */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        // ---- IP 池 ----
        Text(
            stringResource(R.string.ip_pools_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
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
    }
}

package com.snishaper.android.feature.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Activity
import android.widget.Toast
import com.snishaper.android.feature.dashboard.R
import com.snishaper.android.feature.dashboard.vm.DashboardUiState
import com.snishaper.android.feature.dashboard.vm.DashboardViewModel
import com.snishaper.android.feature.dashboard.vm.TunOpEvent

/** 与旧版 Dashboard.tsx 一致：空模式显示为 MITM。 */
private fun displayMode(mode: String): String = mode.trim().ifEmpty { "MITM" }.uppercase()

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    if (bytes >= 1L shl 30) return "%.2f GB".format(bytes / 1024.0 / 1024.0 / 1024.0)
    if (bytes >= 1L shl 20) return "%.1f MB".format(bytes / 1024.0 / 1024.0)
    if (bytes >= 1L shl 10) return "%d KB".format(bytes / 1024)
    return "$bytes B"
}

@Composable
fun DashboardScreen(modifier: Modifier = Modifier, viewModel: DashboardViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tunEvent by viewModel.tunEvents.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.retryTunStart()
        } else {
            viewModel.cancelTunConsent()
        }
    }

    // 字符串在组合期解析（配置感知），Toast 在副作用里弹出
    val tunToast = when (val event = tunEvent) {
        is TunOpEvent.Info -> ToastText(stringResource(tunInfoRes(event.message)), short = true)
        is TunOpEvent.Error -> ToastText(
            stringResource(R.string.dashboard_tun_failed, event.message), short = false,
        )
        is TunOpEvent.ConsentNeeded, null -> null
    }

    LaunchedEffect(tunEvent) {
        if (tunEvent is TunOpEvent.ConsentNeeded) {
            consentLauncher.launch((tunEvent as TunOpEvent.ConsentNeeded).consentIntent)
        }
        tunToast?.let { toast ->
            Toast.makeText(
                context, toast.text,
                if (toast.short) Toast.LENGTH_SHORT else Toast.LENGTH_LONG,
            ).show()
        }
        if (tunEvent != null) viewModel.consumeTunEvent()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MainCard(state = state, onToggleTun = viewModel::toggleTun)
        TrafficCard(state = state)
    }
}

private data class ToastText(val text: String, val short: Boolean)

private fun tunInfoRes(key: String): Int = when (key) {
    "tun_stopped" -> R.string.dashboard_tun_stopped
    else -> R.string.dashboard_tun_starting
}

/** 总开关卡片：TUN 开关即代理启停（一键全起 / 一键全停）。 */
@Composable
private fun MainCard(state: DashboardUiState, onToggleTun: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.dashboard_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (state.isTunBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (state.tunStatus.running) R.string.dashboard_status_running
                            else R.string.dashboard_status_stopped,
                        ),
                        color = if (state.tunStatus.running) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(
                            R.string.dashboard_mode_port, displayMode(state.proxyMode), state.listenPort,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.tunStatus.message.isNotBlank()) {
                        Text(
                            text = state.tunStatus.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = state.tunStatus.running,
                    enabled = state.tunStatus.supported && !state.isTunBusy,
                    onCheckedChange = { onToggleTun() },
                )
            }
            if (!state.tunStatus.supported) {
                Text(
                    text = stringResource(R.string.dashboard_tun_unsupported),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun TrafficCard(state: DashboardUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.dashboard_stats_down),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = formatBytes(state.stats.down), style = MaterialTheme.typography.titleMedium)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.dashboard_stats_up),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = formatBytes(state.stats.up), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

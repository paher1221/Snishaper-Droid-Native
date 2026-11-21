package com.snishaper.android.feature.onboarding

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snishaper.android.feature.onboarding.vm.AboutUiState
import com.snishaper.android.feature.onboarding.vm.AboutViewModel

/**
 * 关于页（对齐旧版 About.tsx）：版本与构建信息、核心特性、社区入口。
 * 链接用系统浏览器打开（旧版 OpenURL 桥接的等价实现）。
 */

@Composable
fun OnboardingScreen(modifier: Modifier = Modifier, viewModel: AboutViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.about_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.about_subtitle),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ================= 版本 =================
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BuildRow(stringResource(R.string.about_version), state.version.ifEmpty { "—" })
                BuildRow(stringResource(R.string.about_core_version), state.buildInfo?.coreVersion?.ifEmpty { "—" } ?: "—")
                BuildRow(stringResource(R.string.about_channel), channelLabel(state.buildInfo?.channel))
                BuildRow(stringResource(R.string.about_go_version), state.buildInfo?.goVersion?.ifEmpty { "—" } ?: "—")
                BuildRow(stringResource(R.string.about_platform), state.buildInfo?.platform?.ifEmpty { "android" } ?: "android")
            }
        }

        // ================= 核心特性 =================
        Text(
            stringResource(R.string.about_features),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureRow(stringResource(R.string.about_feature_ech), stringResource(R.string.about_feature_ech_desc))
                FeatureRow(stringResource(R.string.about_feature_fast), stringResource(R.string.about_feature_fast_desc))
                FeatureRow(stringResource(R.string.about_feature_open), stringResource(R.string.about_feature_open_desc))
            }
        }

        // ================= 社区 =================
        Text(
            stringResource(R.string.about_community),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinkRow(stringResource(R.string.about_website), AboutViewModel.URL_WEBSITE) {
                    openUrl(context, AboutViewModel.URL_WEBSITE)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                LinkRow("GitHub", AboutViewModel.URL_GITHUB) {
                    openUrl(context, AboutViewModel.URL_GITHUB)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                LinkRow(stringResource(R.string.about_releases), AboutViewModel.URL_RELEASES) {
                    openUrl(context, AboutViewModel.URL_RELEASES)
                }
            }
        }

        Text(
            stringResource(R.string.about_copyright),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

@Composable
private fun channelLabel(channel: String?): String = when (channel?.lowercase()) {
    "alpha" -> stringResource(R.string.about_channel_alpha)
    "beta" -> stringResource(R.string.about_channel_beta)
    "rc" -> stringResource(R.string.about_channel_rc)
    else -> stringResource(R.string.about_channel_stable)
}

@Composable
private fun BuildRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterVertically),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun FeatureRow(title: String, desc: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LinkRow(title: String, url: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(
                url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        TextButton(onClick = onClick) { Text(stringResource(R.string.about_open)) }
    }
}

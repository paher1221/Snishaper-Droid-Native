package com.snishaper.android.feature.profiles

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snishaper.android.core.design.component.ScrollableFormDialog
import com.snishaper.android.feature.profiles.vm.ProfilesUiState
import com.snishaper.android.feature.profiles.vm.ProfilesViewModel

/**
 * 档案页：列表 + 激活档案 + 增删改切换 + 导入导出（与旧版 Profiles.tsx 一致）。
 * 导出/导入走系统 SAF（旧版 SaveFile 是 WebView 下载，原生改用存储框架）。
 */
@Composable
fun ProfilesScreen(modifier: Modifier = Modifier, viewModel: ProfilesViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 导出：先取 bundle，再让用户选目标位置
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) viewModel.writePendingExport(uri, context.contentResolver)
        else viewModel.cancelPendingExport()
    }

    // 导入：选 JSON 文件后读入内容交给引擎
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val content = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            }.getOrNull()
            if (!content.isNullOrEmpty()) viewModel.import(content) else viewModel.importFailed()
        }
    }

    LaunchedEffect(state.pendingExport) {
        state.pendingExport?.let { pending ->
            exportLauncher.launch("snishaper-profile-${pending.name}.json")
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.profiles_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { importLauncher.launch(arrayOf("application/json")) }, enabled = !state.busy) {
                Text(stringResource(R.string.profiles_import))
            }
            Button(onClick = { viewModel.openNameDialog(ProfilesUiState.NameDialog.Mode.CREATE) }, enabled = !state.busy) {
                Text(stringResource(R.string.profiles_create))
            }
        }

        Text(
            if (state.active.isEmpty()) stringResource(R.string.profiles_none_active)
            else stringResource(R.string.profiles_active, state.active),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )

        state.message?.let { msg ->
            Text(
                msg,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (state.profiles.isEmpty() && state.loaded) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.profiles_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.profiles, key = { it.name }) { profile ->
                    val isActive = profile.name == state.active
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                profile.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                            if (profile.updatedAt.isNotEmpty()) {
                                Text(
                                    stringResource(R.string.profiles_updated, profile.updatedAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = { viewModel.requestSwitch(profile.name) },
                                    enabled = !state.busy && !isActive,
                                ) { Text(stringResource(R.string.profiles_switch)) }
                                TextButton(
                                    onClick = { viewModel.openNameDialog(ProfilesUiState.NameDialog.Mode.RENAME, profile.name) },
                                    enabled = !state.busy,
                                ) { Text(stringResource(R.string.profiles_rename)) }
                                TextButton(
                                    onClick = { viewModel.openNameDialog(ProfilesUiState.NameDialog.Mode.DUPLICATE, profile.name) },
                                    enabled = !state.busy,
                                ) { Text(stringResource(R.string.profiles_duplicate)) }
                                TextButton(onClick = { viewModel.export(profile.name) }, enabled = !state.busy) {
                                    Text(stringResource(R.string.profiles_export))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- 切换确认弹窗 ----
    state.switchTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelSwitch() },
            title = { Text(stringResource(R.string.profiles_switch_confirm_title)) },
            text = { Text(stringResource(R.string.profiles_switch_confirm_desc, target)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmSwitch() }) {
                    Text(stringResource(R.string.profiles_switch))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelSwitch() }) {
                    Text(stringResource(R.string.profiles_cancel))
                }
            },
        )
    }

    // ---- 名称弹窗（create / rename / duplicate 三模式复用） ----
    state.nameDialog?.let { dialog ->
        var nameInput by remember(dialog) { mutableStateOf(if (dialog.mode == ProfilesUiState.NameDialog.Mode.CREATE) "" else dialog.source) }
        val (titleRes, actionRes) = when (dialog.mode) {
            ProfilesUiState.NameDialog.Mode.CREATE -> R.string.profiles_create_title to R.string.profiles_create
            ProfilesUiState.NameDialog.Mode.RENAME -> R.string.profiles_rename_title to R.string.profiles_rename
            ProfilesUiState.NameDialog.Mode.DUPLICATE -> R.string.profiles_duplicate_title to R.string.profiles_duplicate
        }
        ScrollableFormDialog(
            title = stringResource(titleRes),
            onDismiss = { viewModel.closeNameDialog() },
            confirmLabel = stringResource(actionRes),
            dismissLabel = stringResource(R.string.profiles_cancel),
            confirmEnabled = nameInput.isNotBlank(),
            onConfirm = { viewModel.submitName(nameInput) },
        ) {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.profiles_name_label)) },
            )
        }
    }
}

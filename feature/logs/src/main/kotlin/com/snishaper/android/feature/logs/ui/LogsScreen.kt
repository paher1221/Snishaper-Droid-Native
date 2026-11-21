package com.snishaper.android.feature.logs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snishaper.android.feature.logs.R
import com.snishaper.android.feature.logs.vm.LogsViewModel

private val RE_ERROR = Regex("error|failed|panic", RegexOption.IGNORE_CASE)
private val RE_WARN = Regex("warn", RegexOption.IGNORE_CASE)
private val RE_LINE = Regex("^(\\d{4}/\\d{2}/\\d{2}) (\\d{2}:\\d{2}:\\d{2})(?:\\.\\d+)?\\s+(.*)$")

private data class ParsedLine(val time: String, val msg: String, val isError: Boolean, val isWarn: Boolean)

private fun parseLine(text: String): ParsedLine {
    val match = RE_LINE.find(text) ?: return ParsedLine("--:--:--", text, false, false)
    val msg = match.groupValues[3]
    return ParsedLine(
        time = match.groupValues[2],
        msg = msg,
        isError = RE_ERROR.containsMatchIn(msg),
        isWarn = RE_WARN.containsMatchIn(msg),
    )
}

@Composable
fun LogsScreen(modifier: Modifier = Modifier, viewModel: LogsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var search by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val filtered = state.lines.filter { it.lowercase().contains(search.lowercase()) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.logs_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { viewModel.togglePause() }) {
                Text(
                    stringResource(
                        if (state.isPaused) R.string.logs_resume else R.string.logs_pause,
                    ),
                )
            }
            FilledIconButton(
                onClick = { viewModel.clear() },
                modifier = Modifier.size(36.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.logs_clear),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.logs_search)) },
        )
        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(filtered) { line ->
                LogLine(parseLine(line))
            }
        }
    }
}

@Composable
private fun LogLine(line: ParsedLine) {
    val levelColor = when {
        line.isError -> MaterialTheme.colorScheme.error
        line.isWarn -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    line.isError -> MaterialTheme.colorScheme.error.copy(alpha = 0.05f)
                    line.isWarn -> Color(0xFFF59E0B).copy(alpha = 0.05f)
                    else -> Color.Transparent
                },
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = line.time,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = when {
                line.isError -> "ERR"
                line.isWarn -> "WARN"
                else -> "INFO"
            },
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = levelColor,
            modifier = Modifier.padding(end = 8.dp, top = 2.dp),
        )
        Text(text = line.msg, fontSize = 11.sp, color = levelColor, lineHeight = 17.sp)
    }
}

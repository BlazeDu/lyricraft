package com.storm.lyricraft.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.storm.lyricraft.R
import com.storm.lyricraft.data.LyricLine
import com.storm.lyricraft.ui.components.LyricItemReadOnly
import com.storm.lyricraft.utils.formatMs

@Composable
fun RecordTimeScreen(
    lyricList: List<LyricLine>,
    selectedIndex: Int,
    audioLoaded: Boolean,
    player: ExoPlayer?,
    currentPos: Long,
    duration: Long,
    isPlaying: Boolean,
    onSelectAudio: () -> Unit,
    onShowExport: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSelectLine: (Int) -> Unit,
    onRecordTime: () -> Unit,
    onResetTime: (Int) -> Unit,
    onUpdateTime: (Int, Long) -> Unit // 新增：更新指定行的时间
) {
    val listState = rememberLazyListState()

    // 控制编辑弹窗的状态
    var showEditDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf(-1) }
    var timeInput by remember { mutableStateOf("") }
    var timeError by remember { mutableStateOf(false) }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex in lyricList.indices) {
            listState.animateScrollToItem(
                index = selectedIndex,
                scrollOffset = -200
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onSelectAudio) {
                Text(stringResource(R.string.time_select_audio))
            }
            Button(onClick = onShowExport) {
                Text(stringResource(R.string.time_export_lrc))
            }
        }

        Box(modifier = Modifier.padding(vertical = 6.dp))

        if (audioLoaded && player != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) stringResource(R.string.time_pause) else stringResource(R.string.time_play),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    Slider(
                        value = currentPos.toFloat(),
                        valueRange = 0f..duration.coerceAtLeast(0).toFloat(),
                        onValueChange = {
                            onSeek(it.toLong())
                        },
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                            thumbColor = MaterialTheme.colorScheme.primary,
                            disabledThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMs(currentPos),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatMs(duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Box(modifier = Modifier.padding(vertical = 8.dp))

            Button(
                onClick = onRecordTime,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.time_record_btn))
            }
            Box(modifier = Modifier.padding(vertical = 4.dp))
        } else {
            Text(
                text = stringResource(R.string.time_no_audio),
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 4.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(6.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(lyricList) { index, item ->
                    LyricItemReadOnly(
                        data = item,
                        isSelected = index == selectedIndex,
                        onSelect = { onSelectLine(index) },
                        onResetTime = { onResetTime(index) },
                        onEditTime = {
                            editingIndex = index
                            // 格式化当前时间填入输入框，例如 "01:23.45"
                            timeInput = item.formatTime()
                            timeError = false
                            showEditDialog = true
                        }
                    )
                }
            }
        }
    }

    // 修改时间的弹窗
    if (showEditDialog && editingIndex in lyricList.indices) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("修改时间") },
            text = {
                Column {
                    OutlinedTextField(
                        value = timeInput,
                        onValueChange = {
                            timeInput = it
                            timeError = false
                        },
                        label = { Text("时间格式 (MM:SS.cc)") },
                        isError = timeError,
                        singleLine = true
                    )
                    if (timeError) {
                        Text(
                            text = "格式错误，请使用如 01:23.45",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newTimeMs = parseTimeToMs(timeInput)
                    if (newTimeMs != null) {
                        onUpdateTime(editingIndex, newTimeMs)
                        showEditDialog = false
                    } else {
                        timeError = true
                    }
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

// 辅助函数：解析 "MM:SS.cc" 格式的字符串为毫秒
fun parseTimeToMs(timeStr: String): Long? {
    return try {
        val parts = timeStr.split(":", ".")
        if (parts.size >= 3) {
            val min = parts[0].toLong()
            val sec = parts[1].toLong()
            val cs = parts[2].toLong()
            (min * 60 + sec) * 1000 + cs * 10
        } else if (parts.size == 2) {
            // 兼容 "MM:SS" 格式
            val min = parts[0].toLong()
            val sec = parts[1].toLong()
            (min * 60 + sec) * 1000
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}
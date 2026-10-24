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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    onResetTime: (Int) -> Unit
) {
    // 创建 LazyListState 用于控制滚动
    val listState = rememberLazyListState()

    // 监听选中行变化，自动滚动到居中位置
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in lyricList.indices) {
            // 计算偏移量，使当前行居中
            // 假设每行高度固定，可以用 scrollOffset 进行偏移微调。
            // 这里我们使用简单的 animateScrollToItem 滚动到目标，稍微加上一点偏移让其视觉居中
            listState.animateScrollToItem(
                index = selectedIndex,
                scrollOffset = -200 // 负值会向上偏移，根据你的行高调整这个值，通常设为列表可视高度的一半左右
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

        // 歌词列表
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
                state = listState, // 绑定 listState
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(lyricList) { index, item ->
                    LyricItemReadOnly(
                        data = item,
                        isSelected = index == selectedIndex,
                        onSelect = { onSelectLine(index) },
                        onResetTime = { onResetTime(index) }
                    )
                }
            }
        }
    }
}
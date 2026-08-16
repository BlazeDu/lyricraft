package com.storm.lyricraft.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
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
                Text("选择音频")
            }
            Button(onClick = onShowExport) {
                Text("导出LRC")
            }
        }

        Box(modifier = Modifier.padding(vertical = 6.dp))

        if (audioLoaded && player != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onPlayPause) {
                    Text(if (isPlaying) "暂停" else "播放")
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Slider(
                        value = currentPos.toFloat(),
                        valueRange = 0f..duration.coerceAtLeast(0).toFloat(),
                        onValueChange = {
                            onSeek(it.toLong())
                        },
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatMs(currentPos), style = MaterialTheme.typography.bodySmall)
                        Text(formatMs(duration), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Box(modifier = Modifier.padding(vertical = 6.dp))

            Button(
                onClick = onRecordTime,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("记录当前歌曲时间到选中行（自动跳转下一行）")
            }
            Box(modifier = Modifier.padding(vertical = 6.dp))
        } else {
            Text("请先选择音频", modifier = Modifier.padding(vertical = 10.dp))
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
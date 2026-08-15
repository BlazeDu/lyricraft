package com.storm.lyricraft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.storm.lyricraft.data.LyricLine

@Composable
fun LyricItemReadOnly(
    data: LyricLine,
    index: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onResetTime: () -> Unit
) {
    val hasTime = data.timeMs > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .padding(horizontal = 6.dp)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    hasTime -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    else -> Color.Transparent
                }
            )
            .clickable { onSelect() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (hasTime) "[${data.formatTime()}]" else "[00:00.00]",
            style = MaterialTheme.typography.bodyMedium,
            color = if (hasTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )

        Text(
            text = data.text.ifEmpty { "(空)" },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        Button(
            onClick = onResetTime,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text("清除")
        }
    }
}
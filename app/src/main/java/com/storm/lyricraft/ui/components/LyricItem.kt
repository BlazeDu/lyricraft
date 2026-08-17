package com.storm.lyricraft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storm.lyricraft.R
import com.storm.lyricraft.data.LyricLine

@Composable
fun LyricItemReadOnly(
    data: LyricLine,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onResetTime: () -> Unit
) {
    val hasTime = data.timeMs > 0

    val shape = RoundedCornerShape(12.dp)

    // 颜色层次：选中 > 已打点 > 未打点 > 容器背景
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        hasTime -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        hasTime -> MaterialTheme.colorScheme.onBackground
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    val timeColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        hasTime -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    }

    val deleteIconColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .clip(shape)
            .background(backgroundColor)
            .clickable { onSelect() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (hasTime) "[${data.formatTime()}]" else "[00:00.00]",
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            color = timeColor,
            modifier = Modifier.padding(end = 12.dp)
        )

        Text(
            text = data.text.ifEmpty { stringResource(R.string.time_empty_lyric) },
            fontSize = 16.sp,
            color = textColor,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onResetTime,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.time_clear),
                tint = deleteIconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
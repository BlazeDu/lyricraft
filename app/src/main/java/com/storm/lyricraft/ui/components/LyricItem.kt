package com.storm.lyricraft.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.storm.lyricraft.data.LyricLine

@Composable
fun LyricItemCompose(
    data: LyricLine,
    isCurrentRecord: Boolean,
    onUpdate: (String) -> Unit,
    onDelete: () -> Unit
) {
    val textState = remember { mutableStateOf(data.text) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .padding(horizontal = 6.dp)
            .background(if (isCurrentRecord) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("[${data.formatTime()}]")
        OutlinedTextField(
            value = textState.value,
            onValueChange = {
                textState.value = it
                onUpdate(it)
            },
            modifier = Modifier.weight(1f),
            singleLine = false,
            placeholder = { Text("输入歌词") }
        )
        Button(onClick = onDelete) {
            Text("删除")
        }
    }
}
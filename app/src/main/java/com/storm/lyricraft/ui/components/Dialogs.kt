package com.storm.lyricraft.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun ExportDialog(
    show: Boolean,
    selectedSaveDir: File?,
    fileNameInput: String,
    onFileNameChange: (String) -> Unit,
    onSelectDir: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = selectedSaveDir?.absolutePath ?: "未选择文件夹",
                        onValueChange = {},
                        enabled = false,
                        label = { Text("保存目录") }
                    )
                    Button(modifier = Modifier.padding(start = 8.dp), onClick = onSelectDir) {
                        Text("选择目录")
                    }
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = fileNameInput,
                    onValueChange = onFileNameChange,
                    label = { Text("文件名") }
                )
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("确认导出") } },
        dismissButton = { Button(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
fun FolderPickerDialog(
    show: Boolean,
    currentDir: File,
    dirChildren: List<File>,
    canGoUp: Boolean,
    onFolderClick: (File) -> Unit,
    onGoUp: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择保存文件夹") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("当前目录：${currentDir.absolutePath}", modifier = Modifier.padding(bottom = 8.dp))
                HorizontalDivider(modifier = Modifier.padding(bottom = 6.dp))
                if (canGoUp) {
                    Text(
                        text = "../",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGoUp() }
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(dirChildren) { _, folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFolderClick(folder) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📁 ")
                            Text(folder.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("选用此文件夹")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun PermissionTipDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("文件权限") },
        text = { Text("需要授予【所有文件访问权限】才能浏览并选择文件夹保存LRC文件。") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("前往设置开启")
            }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("稍后") } }
    )
}
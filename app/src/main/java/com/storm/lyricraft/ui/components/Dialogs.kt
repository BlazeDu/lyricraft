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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.storm.lyricraft.R
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
        title = { Text(stringResource(R.string.dialog_export_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = selectedSaveDir?.absolutePath ?: stringResource(R.string.dialog_export_select_dir),
                        onValueChange = {},
                        enabled = false,
                        label = { Text(stringResource(R.string.dialog_export_save_dir)) }
                    )
                    Button(modifier = Modifier.padding(start = 8.dp), onClick = onSelectDir) {
                        Text(stringResource(R.string.dialog_export_select_dir))
                    }
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = fileNameInput,
                    onValueChange = onFileNameChange,
                    label = { Text(stringResource(R.string.dialog_export_filename)) }
                )
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.dialog_export_confirm)) } },
        dismissButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.dialog_export_cancel)) } }
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
        title = { Text(stringResource(R.string.dialog_folder_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${stringResource(R.string.dialog_folder_current)} ${currentDir.absolutePath}",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
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
                Text(stringResource(R.string.dialog_folder_confirm))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_export_cancel))
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
        title = { Text(stringResource(R.string.dialog_permission_title)) },
        text = { Text(stringResource(R.string.dialog_permission_message)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.dialog_permission_confirm))
            }
        },
        dismissButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.dialog_permission_dismiss)) } }
    )
}
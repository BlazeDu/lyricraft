package com.storm.lyricraft

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

// ============ M3 主题配色 ============
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2979FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF0D47A1),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF121212),
    surface = Color.White,
    onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFE8EAF6),
    onSurfaceVariant = Color(0xFF31313C)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF002984),
    primaryContainer = Color(0xFF153660),
    onPrimaryContainer = Color(0xFFD0E4FF),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2D2D3A),
    onSurfaceVariant = Color(0xFFC5C6D0)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                LyricEditor(openPermSetting = ::jumpPermSetting)
            }
        }
    }

    private fun jumpPermSetting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }
}

enum class TabPage { InputText, RecordTime, Setting }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricEditor(openPermSetting: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playerState = remember { mutableStateOf<ExoPlayer?>(null) }
    val lyricList = remember { mutableStateListOf<LyricLine>() }

    val currentPosState = remember { mutableStateOf(0L) }
    val durationState = remember { mutableStateOf(0L) }
    val isPlayingState = remember { mutableStateOf(false) }
    val audioLoadedState = remember { mutableStateOf(false) }
    val audioBaseNameState = remember { mutableStateOf("lyric") }
    val currentTab = remember { mutableStateOf(TabPage.InputText) }
    val rawLyricText = remember { mutableStateOf("") }
    val currentRecordIndex = remember { mutableStateOf(0) }

    val showExportDialogState = remember { mutableStateOf(false) }
    val selectedSaveDirState = remember { mutableStateOf<File?>(null) }
    val fileNameInputState = remember { mutableStateOf("lyric.lrc") }
    val showFolderPickerState = remember { mutableStateOf(false) }
    val currentBrowseDirState = remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    val dirChildrenState = remember { mutableStateOf(listOf<File>()) }
    val canGoUpState = remember { mutableStateOf(false) }
    val showPermTipState = remember { mutableStateOf(false) }

    fun loadFolderList(targetDir: File) {
        scope.launch(Dispatchers.IO) {
            val list = targetDir.listFiles()
                ?.filter { it.isDirectory }
                ?.sortedBy { it.name } ?: emptyList()
            val parent = targetDir.parentFile
            val parentAccessible = parent?.listFiles() != null
            scope.launch(Dispatchers.Main) {
                currentBrowseDirState.value = targetDir
                dirChildrenState.value = list
                canGoUpState.value = parentAccessible
            }
        }
    }

    fun saveLrcToFolder() {
        val saveDir = selectedSaveDirState.value
        val fileName = fileNameInputState.value.trim()
        if (saveDir == null) {
            Toast.makeText(context, "保存目录不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        if (fileName.isBlank()) {
            Toast.makeText(context, "文件名不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        val realName = if (fileName.endsWith(".lrc")) fileName else "$fileName.lrc"
        val saveFile = File(saveDir, realName)
        scope.launch(Dispatchers.IO) {
            try {
                val content = buildString {
                    lyricList.sortedBy { it.timeMs }.forEach {
                        appendLine("[${it.formatTime()}]${it.text}")
                    }
                }
                FileOutputStream(saveFile).bufferedWriter().use { it.write(content) }
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
                    showExportDialogState.value = false
                }
            } catch (e: Exception) {
                val errMsg = e.message ?: "未知错误"
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(context, "导出失败:$errMsg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        playerState.value?.release()
        val newPlayer = ExoPlayer.Builder(context).build()
        playerState.value = newPlayer
        var displayName = "lyric"
        // 压制API Level警告，不影响运行
        @Suppress("DEPRECATION")
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) displayName = cursor.getString(idx) ?: "lyric"
            }
        }
        if (displayName.contains('.')) displayName = displayName.substringBeforeLast('.')
        audioBaseNameState.value = displayName
        newPlayer.setMediaItem(MediaItem.fromUri(uri))
        newPlayer.prepare()
        durationState.value = newPlayer.duration
        audioLoadedState.value = true
    }

    DisposableEffect(Unit) {
        onDispose { playerState.value?.release() }
    }

    LaunchedEffect(playerState.value) {
        while (true) {
            val p = playerState.value ?: break
            currentPosState.value = p.currentPosition
            durationState.value = p.duration
            isPlayingState.value = p.isPlaying
            delay(100)
        }
    }

    LaunchedEffect(showFolderPickerState.value) {
        if (showFolderPickerState.value) {
            loadFolderList(currentBrowseDirState.value)
        }
    }

    fun parseRawLyric() {
        val lines = rawLyricText.value.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        lyricList.clear()
        lines.forEach { text ->
            lyricList.add(LyricLine(timeMs = 0, text = text))
        }
        currentRecordIndex.value = 0
        Toast.makeText(context, "已导入${lines.size}行歌词", Toast.LENGTH_SHORT).show()
        currentTab.value = TabPage.RecordTime
    }

    fun recordCurrentTime() {
        if (lyricList.isEmpty()) {
            Toast.makeText(context, "请先导入歌词", Toast.LENGTH_SHORT).show()
            return
        }
        val idx = currentRecordIndex.value
        if (idx >= lyricList.size) {
            Toast.makeText(context, "全部歌词打点完成", Toast.LENGTH_SHORT).show()
            return
        }
        lyricList[idx] = lyricList[idx].copy(timeMs = currentPosState.value)
        currentRecordIndex.value = idx + 1
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // 底栏：surfaceVariant 和页面background区分颜色，铺满底部无悬浮
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = currentTab.value == TabPage.InputText,
                    onClick = { currentTab.value = TabPage.InputText },
                    label = { Text("文本") },
                    icon = {}
                )
                NavigationBarItem(
                    selected = currentTab.value == TabPage.RecordTime,
                    onClick = { currentTab.value = TabPage.RecordTime },
                    label = { Text("时间") },
                    icon = {}
                )
                NavigationBarItem(
                    selected = currentTab.value == TabPage.Setting,
                    onClick = { currentTab.value = TabPage.Setting },
                    label = { Text("设置") },
                    icon = {}
                )
            }
        }
    ) { padding ->
        // 主页面背景
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentTab.value) {
                    TabPage.InputText -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text("粘贴全部歌词（自动按换行分割每行）")
                        TextField(
                            value = rawLyricText.value,
                            onValueChange = { rawLyricText.value = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            placeholder = { Text("在这里粘贴完整歌词，每一行会自动生成一条歌词记录") },
                            maxLines = Int.MAX_VALUE
                        )
                        Button(
                            onClick = ::parseRawLyric,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Text("导入歌词并进入打点界面")
                        }
                    }

                    TabPage.RecordTime -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = { audioPicker.launch("audio/*") }) {
                                Text("选择音频")
                            }
                            Button(onClick = {
                                fileNameInputState.value = "${audioBaseNameState.value}.lrc"
                                showExportDialogState.value = true
                            }) {
                                Text("导出LRC")
                            }
                            Button(onClick = { showPermTipState.value = true }) {
                                Text("权限设置")
                            }
                        }

                        Box(modifier = Modifier.padding(vertical = 6.dp))

                        if (audioLoadedState.value && playerState.value != null) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = {
                                    if (isPlayingState.value) playerState.value?.pause() else playerState.value?.play()
                                }) {
                                    Text(if (isPlayingState.value) "暂停" else "播放")
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Slider(
                                        value = currentPosState.value.toFloat(),
                                        valueRange = 0f..durationState.value.coerceAtLeast(0).toFloat(),
                                        onValueChange = {
                                            playerState.value?.seekTo(it.toLong())
                                            currentPosState.value = it.toLong()
                                        },
                                        colors = SliderDefaults.colors(
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(formatMs(currentPosState.value), style = MaterialTheme.typography.bodySmall)
                                        Text(formatMs(durationState.value), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            Box(modifier = Modifier.padding(vertical = 6.dp))

                            Button(
                                onClick = ::recordCurrentTime,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("记录当前歌曲时间（自动下一行）")
                            }
                            Box(modifier = Modifier.padding(vertical = 6.dp))
                        } else {
                            Text("请先选择音频", modifier = Modifier.padding(vertical = 10.dp))
                        }

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(lyricList) { index, item ->
                                LyricItemCompose(
                                    data = item,
                                    isCurrentRecord = index == currentRecordIndex.value,
                                    onUpdate = { newText ->
                                        lyricList[index] = item.copy(text = newText)
                                    },
                                    onDelete = { lyricList.remove(item) }
                                )
                            }
                        }
                    }

                    TabPage.Setting -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("设置页面", style = MaterialTheme.typography.headlineMedium)
                            Text("功能开发中", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (showExportDialogState.value) {
            AlertDialog(
                onDismissRequest = { showExportDialogState.value = false },
                title = { Text("导出") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                value = selectedSaveDirState.value?.absolutePath ?: "未选择文件夹",
                                onValueChange = {},
                                enabled = false,
                                label = { Text("保存目录") }
                            )
                            Button(modifier = Modifier.padding(start = 8.dp), onClick = {
                                showFolderPickerState.value = true
                            }) {
                                Text("选择目录")
                            }
                        }
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = fileNameInputState.value,
                            onValueChange = { fileNameInputState.value = it },
                            label = { Text("文件名") }
                        )
                    }
                },
                confirmButton = { Button(onClick = ::saveLrcToFolder) { Text("确认导出") } },
                dismissButton = { Button(onClick = { showExportDialogState.value = false }) { Text("取消") } }
            )
        }

        if (showFolderPickerState.value) {
            AlertDialog(
                onDismissRequest = { showFolderPickerState.value = false },
                title = { Text("选择保存文件夹") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("当前目录：${currentBrowseDirState.value.absolutePath}", modifier = Modifier.padding(bottom = 8.dp))
                        HorizontalDivider(modifier = Modifier.padding(bottom = 6.dp))
                        if (canGoUpState.value) {
                            Text(
                                text = "../",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentBrowseDirState.value.parentFile?.let { loadFolderList(it) }
                                    }
                                    .padding(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            itemsIndexed(dirChildrenState.value) { _, folder ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { loadFolderList(folder) }
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
                    Button(onClick = {
                        selectedSaveDirState.value = currentBrowseDirState.value
                        showFolderPickerState.value = false
                    }) {
                        Text("选用此文件夹")
                    }
                },
                dismissButton = {
                    Button(onClick = { showFolderPickerState.value = false }) {
                        Text("取消")
                    }
                }
            )
        }

        if (showPermTipState.value) {
            AlertDialog(
                onDismissRequest = { showPermTipState.value = false },
                title = { Text("文件权限") },
                text = { Text("需要授予【所有文件访问权限】才能浏览并选择文件夹保存LRC文件。") },
                confirmButton = {
                    Button(onClick = {
                        openPermSetting()
                        showPermTipState.value = false
                    }) {
                        Text("前往设置开启")
                    }
                },
                dismissButton = { Button(onClick = { showPermTipState.value = false }) { Text("稍后") } }
            )
        }
    }
}

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

fun formatMs(ms: Long): String {
    if (ms <= 0) return "00:00.00"
    val secAll = ms / 1000
    val min = secAll / 60
    val sec = secAll % 60
    val msPart = (ms % 1000) / 10
    return "%02d:%02d.%02d".format(min, sec, msPart)
}

data class LyricLine(val timeMs: Long, val text: String) {
    fun formatTime() = formatMs(timeMs)
}
package com.storm.lyricraft

import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.storm.lyricraft.data.LyricLine
import com.storm.lyricraft.ui.components.ExportDialog
import com.storm.lyricraft.ui.components.FolderPickerDialog
import com.storm.lyricraft.ui.navigation.TabPage
import com.storm.lyricraft.ui.screens.InputTextScreen
import com.storm.lyricraft.ui.screens.RecordTimeScreen
import com.storm.lyricraft.ui.screens.SettingScreen
import com.storm.lyricraft.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                LyricEditor()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricEditor() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 播放器相关
    val playerState = remember { mutableStateOf<ExoPlayer?>(null) }
    val currentPosState = remember { mutableStateOf(0L) }
    val durationState = remember { mutableStateOf(0L) }
    val isPlayingState = remember { mutableStateOf(false) }
    val audioLoadedState = remember { mutableStateOf(false) }
    val audioBaseNameState = remember { mutableStateOf("lyric") }

    // 歌词列表 — 唯一数据源
    val lyricList = remember { mutableStateListOf<LyricLine>() }
    // 当前选中的行索引
    val selectedIndex = remember { mutableStateOf(0) }

    // 导航
    val currentTab = remember { mutableStateOf(TabPage.InputText) }
    val rawLyricText = remember { mutableStateOf("") }

    // 导出
    val showExportDialogState = remember { mutableStateOf(false) }
    val selectedSaveDirState = remember { mutableStateOf<File?>(null) }
    val fileNameInputState = remember { mutableStateOf("lyric.lrc") }

    // 文件夹选择器
    val showFolderPickerState = remember { mutableStateOf(false) }
    val currentBrowseDirState = remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    val dirChildrenState = remember { mutableStateOf(listOf<File>()) }
    val canGoUpState = remember { mutableStateOf(false) }

    // ============ 切换到时间页时自动同步歌词 ============
    LaunchedEffect(currentTab.value) {
        if (currentTab.value == TabPage.RecordTime && rawLyricText.value.isNotBlank()) {
            val lines = rawLyricText.value.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (lines.isNotEmpty()) {
                val newList = lines.mapIndexed { index, text ->
                    if (index < lyricList.size) {
                        lyricList[index].copy(text = text)
                    } else {
                        LyricLine(timeMs = 0, text = text)
                    }
                }
                // 检测是否有变化
                val hasChanged = newList.size != lyricList.size ||
                        newList.zip(lyricList).any { it.first.text != it.second.text }

                if (hasChanged) {
                    lyricList.clear()
                    lyricList.addAll(newList)
                    if (selectedIndex.value >= lyricList.size) {
                        selectedIndex.value = 0
                    }
                }
            }
        }
    }

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
                java.io.FileOutputStream(saveFile).bufferedWriter().use { it.write(content) }
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

    // 导入歌词（初始化列表）
    fun importLyrics(text: String) {
        val lines = text.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            Toast.makeText(context, "请先粘贴歌词", Toast.LENGTH_SHORT).show()
            return
        }

        val newList = lines.mapIndexed { index, line ->
            if (index < lyricList.size) {
                lyricList[index].copy(text = line)
            } else {
                LyricLine(timeMs = 0, text = line)
            }
        }

        lyricList.clear()
        lyricList.addAll(newList)
        selectedIndex.value = 0
        Toast.makeText(context, "已导入${lines.size}行歌词", Toast.LENGTH_SHORT).show()
        currentTab.value = TabPage.RecordTime
    }

    // 清除时间戳
    fun resetLyricTime(index: Int) {
        if (index in lyricList.indices) {
            val current = lyricList[index]
            if (current.timeMs == 0L) return
            lyricList[index] = current.copy(timeMs = 0)
        }
    }

    // 记录当前时间到选中行，自动跳转下一行
    fun recordCurrentTime() {
        if (lyricList.isEmpty()) {
            Toast.makeText(context, "请先导入歌词", Toast.LENGTH_SHORT).show()
            return
        }

        val idx = selectedIndex.value
        if (idx !in lyricList.indices) {
            Toast.makeText(context, "请选中一行歌词", Toast.LENGTH_SHORT).show()
            return
        }

        lyricList[idx] = lyricList[idx].copy(timeMs = currentPosState.value)

        val nextIndex = idx + 1
        if (nextIndex < lyricList.size) {
            selectedIndex.value = nextIndex
        } else {
            Toast.makeText(context, "全部歌词打点完成", Toast.LENGTH_SHORT).show()
        }
    }

    // 选中某行并跳转音频
    fun selectLyricLine(index: Int) {
        if (index !in lyricList.indices) return

        val targetTime = lyricList[index].timeMs
        if (targetTime > 0) {
            playerState.value?.seekTo(targetTime)
            currentPosState.value = targetTime
        }

        selectedIndex.value = index
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
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
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentTab.value) {
                    TabPage.InputText -> InputTextScreen(
                        rawLyricText = rawLyricText.value,
                        onTextChange = { rawLyricText.value = it }
                    )

                    TabPage.RecordTime -> RecordTimeScreen(
                        lyricList = lyricList,
                        selectedIndex = selectedIndex.value,
                        audioLoaded = audioLoadedState.value,
                        player = playerState.value,
                        currentPos = currentPosState.value,
                        duration = durationState.value,
                        isPlaying = isPlayingState.value,
                        onSelectAudio = { audioPicker.launch("audio/*") },
                        onShowExport = {
                            fileNameInputState.value = "${audioBaseNameState.value}.lrc"
                            showExportDialogState.value = true
                        },
                        onPlayPause = {
                            if (isPlayingState.value) playerState.value?.pause()
                            else playerState.value?.play()
                        },
                        onSeek = { pos ->
                            playerState.value?.seekTo(pos)
                            currentPosState.value = pos
                        },
                        onSelectLine = ::selectLyricLine,
                        onRecordTime = ::recordCurrentTime,
                        onResetTime = ::resetLyricTime
                    )

                    TabPage.Setting -> SettingScreen()
                }
            }
        }

        ExportDialog(
            show = showExportDialogState.value,
            selectedSaveDir = selectedSaveDirState.value,
            fileNameInput = fileNameInputState.value,
            onFileNameChange = { fileNameInputState.value = it },
            onSelectDir = { showFolderPickerState.value = true },
            onConfirm = ::saveLrcToFolder,
            onDismiss = { showExportDialogState.value = false }
        )

        FolderPickerDialog(
            show = showFolderPickerState.value,
            currentDir = currentBrowseDirState.value,
            dirChildren = dirChildrenState.value,
            canGoUp = canGoUpState.value,
            onFolderClick = { loadFolderList(it) },
            onGoUp = {
                currentBrowseDirState.value.parentFile?.let { loadFolderList(it) }
            },
            onConfirm = {
                selectedSaveDirState.value = currentBrowseDirState.value
                showFolderPickerState.value = false
            },
            onDismiss = { showFolderPickerState.value = false }
        )
    }
}
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
import com.storm.lyricraft.ui.components.PermissionTipDialog
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricEditor(openPermSetting: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ============ 播放器状态 ============
    val playerState = remember { mutableStateOf<ExoPlayer?>(null) }
    val currentPosState = remember { mutableStateOf(0L) }
    val durationState = remember { mutableStateOf(0L) }
    val isPlayingState = remember { mutableStateOf(false) }
    val audioLoadedState = remember { mutableStateOf(false) }
    val audioBaseNameState = remember { mutableStateOf("lyric") }

    // ============ 歌词数据 ============
    val lyricList = remember { mutableStateListOf<LyricLine>() }
    val currentRecordIndex = remember { mutableStateOf(0) }

    // ============ UI 导航 ============
    val currentTab = remember { mutableStateOf(TabPage.InputText) }
    val rawLyricText = remember { mutableStateOf("") }

    // ============ 导出弹窗状态 ============
    val showExportDialogState = remember { mutableStateOf(false) }
    val selectedSaveDirState = remember { mutableStateOf<File?>(null) }
    val fileNameInputState = remember { mutableStateOf("lyric.lrc") }

    // ============ 文件夹选择器状态 ============
    val showFolderPickerState = remember { mutableStateOf(false) }
    val currentBrowseDirState = remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    val dirChildrenState = remember { mutableStateOf(listOf<File>()) }
    val canGoUpState = remember { mutableStateOf(false) }

    // ============ 权限提示弹窗 ============
    val showPermTipState = remember { mutableStateOf(false) }

    // ============ 加载文件夹列表 ============
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

    // ============ 导出 LRC ============
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

    // ============ 音频选择器 ============
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

    // ============ 生命周期：释放播放器 ============
    DisposableEffect(Unit) {
        onDispose { playerState.value?.release() }
    }

    // ============ 进度轮询 ============
    LaunchedEffect(playerState.value) {
        while (true) {
            val p = playerState.value ?: break
            currentPosState.value = p.currentPosition
            durationState.value = p.duration
            isPlayingState.value = p.isPlaying
            delay(100)
        }
    }

    // ============ 文件夹弹窗自动刷新 ============
    LaunchedEffect(showFolderPickerState.value) {
        if (showFolderPickerState.value) {
            loadFolderList(currentBrowseDirState.value)
        }
    }

    // ============ 解析歌词 ============
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

    // ============ 时间打点 ============
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

    // ============ UI ============
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
                        onTextChange = { rawLyricText.value = it },
                        onParse = ::parseRawLyric
                    )

                    TabPage.RecordTime -> RecordTimeScreen(
                        lyricList = lyricList,
                        currentRecordIndex = currentRecordIndex.value,
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
                        onShowPermTip = { showPermTipState.value = true },
                        onPlayPause = {
                            if (isPlayingState.value) playerState.value?.pause()
                            else playerState.value?.play()
                        },
                        onSeek = { pos ->
                            playerState.value?.seekTo(pos)
                            currentPosState.value = pos
                        },
                        onRecordTime = ::recordCurrentTime,
                        onUpdateLyric = { index, newText ->
                            lyricList[index] = lyricList[index].copy(text = newText)
                        },
                        onDeleteLyric = { index ->
                            lyricList.removeAt(index)
                        }
                    )

                    TabPage.Setting -> SettingScreen()
                }
            }
        }

        // ============ 弹窗 ============
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

        PermissionTipDialog(
            show = showPermTipState.value,
            onConfirm = {
                openPermSetting()
                showPermTipState.value = false
            },
            onDismiss = { showPermTipState.value = false }
        )
    }
}
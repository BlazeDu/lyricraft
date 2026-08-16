package com.storm.lyricraft

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.storm.lyricraft.utils.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var themeManager: ThemeManager

    private fun jumpPermSetting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        themeManager = ThemeManager(this)
        val savedTheme = themeManager.getTheme()

        // 设置状态栏和导航栏为透明，颜色由 Theme 控制
        window.apply {
            statusBarColor = android.graphics.Color.TRANSPARENT
            navigationBarColor = android.graphics.Color.TRANSPARENT
        }

        setContent {
            val currentTheme = rememberSaveable { mutableStateOf(savedTheme) }

            AppTheme(themeMode = currentTheme.value) {
                LyricEditor(
                    openPermSetting = ::jumpPermSetting,
                    themeManager = themeManager,
                    initialTheme = savedTheme,
                    onThemeChanged = { newTheme ->
                        currentTheme.value = newTheme
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricEditor(
    openPermSetting: () -> Unit,
    themeManager: ThemeManager,
    initialTheme: String,
    onThemeChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ============ 可序列化状态（rememberSaveable） ============
    val currentPosState = rememberSaveable { mutableStateOf(0L) }
    val durationState = rememberSaveable { mutableStateOf(0L) }
    val isPlayingState = rememberSaveable { mutableStateOf(false) }
    val audioLoadedState = rememberSaveable { mutableStateOf(false) }
    val audioBaseNameState = rememberSaveable { mutableStateOf("lyric") }

    val selectedIndex = rememberSaveable { mutableStateOf(0) }
    val currentTab = rememberSaveable { mutableStateOf(TabPage.InputText) }
    val rawLyricText = rememberSaveable { mutableStateOf("") }

    val showExportDialogState = rememberSaveable { mutableStateOf(false) }
    val fileNameInputState = rememberSaveable { mutableStateOf("lyric.lrc") }
    val showFolderPickerState = rememberSaveable { mutableStateOf(false) }
    val showPermTipState = rememberSaveable { mutableStateOf(false) }
    val hasStoragePermission = rememberSaveable {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
            }
        )
    }
    val themeMode = rememberSaveable { mutableStateOf(initialTheme) }

    // ============ 不可序列化状态（remember） ============
    val lyricList = remember { mutableStateListOf<LyricLine>() }
    val playerState = remember { mutableStateOf<ExoPlayer?>(null) }
    val selectedSaveDirState = remember { mutableStateOf<File?>(null) }
    val currentBrowseDirState = remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    val dirChildrenState = remember { mutableStateOf(listOf<File>()) }
    val canGoUpState = remember { mutableStateOf(false) }

    // ============ 启动时自动检测权限 ============
    LaunchedEffect(Unit) {
        delay(500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            showPermTipState.value = true
        }
    }

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

    // ============ 每次回到设置页刷新权限状态 ============
    LaunchedEffect(currentTab.value) {
        if (currentTab.value == TabPage.Setting) {
            hasStoragePermission.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Toast.makeText(context, "请先授予全部文件访问权限", Toast.LENGTH_SHORT).show()
            showPermTipState.value = true
            return
        }

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

    fun resetLyricTime(index: Int) {
        if (index !in lyricList.indices) return

        val current = lyricList[index]
        if (current.timeMs == 0L) return

        lyricList[index] = current.copy(timeMs = 0)
        selectedIndex.value = index

        val prevIndex = index - 1
        val targetTime = if (prevIndex >= 0 && lyricList[prevIndex].timeMs > 0) {
            lyricList[prevIndex].timeMs
        } else {
            0L
        }

        playerState.value?.seekTo(targetTime)
        currentPosState.value = targetTime

        Toast.makeText(context, "已清除，跳转到上一行", Toast.LENGTH_SHORT).show()
    }

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
                tonalElevation = 0.dp,
                modifier = Modifier.padding(bottom = 0.dp)
            ) {
                NavigationBarItem(
                    selected = currentTab.value == TabPage.InputText,
                    onClick = { currentTab.value = TabPage.InputText },
                    label = { Text("歌词", fontSize = 14.dp.value.sp) },
                    icon = {}
                )
                NavigationBarItem(
                    selected = currentTab.value == TabPage.RecordTime,
                    onClick = { currentTab.value = TabPage.RecordTime },
                    label = { Text("时间", fontSize = 14.dp.value.sp) },
                    icon = {}
                )
                NavigationBarItem(
                    selected = currentTab.value == TabPage.Setting,
                    onClick = { currentTab.value = TabPage.Setting },
                    label = { Text("设置", fontSize = 14.dp.value.sp) },
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

                    TabPage.Setting -> SettingScreen(
                        hasStoragePermission = hasStoragePermission.value,
                        appVersion = getAppVersion(context),
                        currentTheme = themeMode.value,
                        onThemeSelected = { selectedTheme ->
                            themeMode.value = selectedTheme
                            themeManager.saveTheme(selectedTheme)
                            onThemeChanged(selectedTheme)
                        },
                        onOpenPermSetting = openPermSetting,
                        onOpenGitHub = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/BlazeDu"))
                            context.startActivity(intent)
                        }
                    )
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

fun getAppVersion(context: android.content.Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }
}
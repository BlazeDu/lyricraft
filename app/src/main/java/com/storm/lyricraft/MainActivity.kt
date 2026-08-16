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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.storm.lyricraft.R
import com.storm.lyricraft.data.LyricLine
import com.storm.lyricraft.ui.components.ExportDialog
import com.storm.lyricraft.ui.components.FolderPickerDialog
import com.storm.lyricraft.ui.components.PermissionTipDialog
import com.storm.lyricraft.ui.navigation.TabPage
import com.storm.lyricraft.ui.screens.InputTextScreen
import com.storm.lyricraft.ui.screens.RecordTimeScreen
import com.storm.lyricraft.ui.screens.SettingScreen
import com.storm.lyricraft.ui.theme.AppTheme
import com.storm.lyricraft.utils.LocaleManager
import com.storm.lyricraft.utils.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var themeManager: ThemeManager
    private lateinit var localeManager: LocaleManager

    private fun jumpPermSetting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val localeManager = LocaleManager(newBase)
        val context = localeManager.applyLanguage(newBase)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        themeManager = ThemeManager(this)
        localeManager = LocaleManager(this)

        val savedTheme = themeManager.getTheme()
        val savedLanguage = localeManager.getLanguage()

        setContent {
            val currentTheme = rememberSaveable { mutableStateOf(savedTheme) }
            val currentLanguage = rememberSaveable { mutableStateOf(savedLanguage) }

            AppTheme(themeMode = currentTheme.value) {
                LyricEditor(
                    openPermSetting = ::jumpPermSetting,
                    themeManager = themeManager,
                    localeManager = localeManager,
                    initialTheme = savedTheme,
                    initialLanguage = savedLanguage,
                    onThemeChanged = { newTheme ->
                        currentTheme.value = newTheme
                    },
                    onLanguageChanged = { newLang ->
                        currentLanguage.value = newLang
                        recreate()
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
    localeManager: LocaleManager,
    initialTheme: String,
    initialLanguage: String,
    onThemeChanged: (String) -> Unit,
    onLanguageChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentPosState = rememberSaveable { mutableLongStateOf(0L) }
    val durationState = rememberSaveable { mutableLongStateOf(0L) }
    val isPlayingState = rememberSaveable { mutableStateOf(false) }
    val audioLoadedState = rememberSaveable { mutableStateOf(false) }
    val audioBaseNameState = rememberSaveable { mutableStateOf("lyric") }

    val selectedIndex = rememberSaveable { mutableIntStateOf(0) }
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
    val languageMode = rememberSaveable { mutableStateOf(initialLanguage) }

    val lyricList = remember { mutableStateListOf<LyricLine>() }
    val playerState = remember { mutableStateOf<ExoPlayer?>(null) }
    val selectedSaveDirState = remember { mutableStateOf<File?>(null) }
    val currentBrowseDirState = remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    val dirChildrenState = remember { mutableStateOf(listOf<File>()) }
    val canGoUpState = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            showPermTipState.value = true
        }
    }

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
                        selectedIndex.intValue = 0
                    }
                }
            }
        }
    }

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
            Toast.makeText(context, context.getString(R.string.toast_permission_required), Toast.LENGTH_SHORT).show()
            showPermTipState.value = true
            return
        }

        val saveDir = selectedSaveDirState.value
        val fileName = fileNameInputState.value.trim()
        if (saveDir == null) {
            Toast.makeText(context, context.getString(R.string.dialog_export_dir_empty), Toast.LENGTH_SHORT).show()
            return
        }
        if (fileName.isBlank()) {
            Toast.makeText(context, context.getString(R.string.dialog_export_name_empty), Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, context.getString(R.string.dialog_export_success), Toast.LENGTH_SHORT).show()
                    showExportDialogState.value = false
                }
            } catch (_: Exception) {
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.dialog_export_failed, "未知错误"), Toast.LENGTH_LONG).show()
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
            currentPosState.longValue = p.currentPosition
            durationState.longValue = p.duration
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
            Toast.makeText(context, context.getString(R.string.toast_paste_first), Toast.LENGTH_SHORT).show()
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
        selectedIndex.intValue = 0
        Toast.makeText(context, context.getString(R.string.lyrics_import_toast, lines.size), Toast.LENGTH_SHORT).show()
        currentTab.value = TabPage.RecordTime
    }

    fun resetLyricTime(index: Int) {
        if (index !in lyricList.indices) return

        val current = lyricList[index]
        if (current.timeMs == 0L) return

        lyricList[index] = current.copy(timeMs = 0)
        selectedIndex.intValue = index

        val prevIndex = index - 1
        val targetTime = if (prevIndex >= 0 && lyricList[prevIndex].timeMs > 0) {
            lyricList[prevIndex].timeMs
        } else {
            0L
        }

        playerState.value?.seekTo(targetTime)
        currentPosState.longValue = targetTime

        Toast.makeText(context, context.getString(R.string.time_clear_toast), Toast.LENGTH_SHORT).show()
    }

    fun recordCurrentTime() {
        if (lyricList.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.toast_import_first), Toast.LENGTH_SHORT).show()
            return
        }

        val idx = selectedIndex.intValue
        if (idx !in lyricList.indices) {
            Toast.makeText(context, context.getString(R.string.time_please_select), Toast.LENGTH_SHORT).show()
            return
        }

        lyricList[idx] = lyricList[idx].copy(timeMs = currentPosState.longValue)

        val nextIndex = idx + 1
        if (nextIndex < lyricList.size) {
            selectedIndex.intValue = nextIndex
        } else {
            Toast.makeText(context, context.getString(R.string.time_all_done), Toast.LENGTH_SHORT).show()
        }
    }

    fun selectLyricLine(index: Int) {
        if (index !in lyricList.indices) return

        val targetTime = lyricList[index].timeMs
        if (targetTime > 0) {
            playerState.value?.seekTo(targetTime)
            currentPosState.longValue = targetTime
        }

        selectedIndex.intValue = index
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
                    label = { Text(stringResource(R.string.nav_lyrics), fontSize = 14.sp) },
                    icon = {}
                )
                NavigationBarItem(
                    selected = currentTab.value == TabPage.RecordTime,
                    onClick = { currentTab.value = TabPage.RecordTime },
                    label = { Text(stringResource(R.string.nav_time), fontSize = 14.sp) },
                    icon = {}
                )
                NavigationBarItem(
                    selected = currentTab.value == TabPage.Setting,
                    onClick = { currentTab.value = TabPage.Setting },
                    label = { Text(stringResource(R.string.nav_settings), fontSize = 14.sp) },
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
                        selectedIndex = selectedIndex.intValue,
                        audioLoaded = audioLoadedState.value,
                        player = playerState.value,
                        currentPos = currentPosState.longValue,
                        duration = durationState.longValue,
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
                            currentPosState.longValue = pos
                        },
                        onSelectLine = ::selectLyricLine,
                        onRecordTime = ::recordCurrentTime,
                        onResetTime = ::resetLyricTime
                    )

                    TabPage.Setting -> SettingScreen(
                        hasStoragePermission = hasStoragePermission.value,
                        appVersion = getAppVersion(context),
                        currentTheme = themeMode.value,
                        currentLanguage = languageMode.value,
                        onThemeSelected = { selectedTheme ->
                            themeMode.value = selectedTheme
                            themeManager.saveTheme(selectedTheme)
                            onThemeChanged(selectedTheme)
                        },
                        onLanguageSelected = { selectedLanguage ->
                            languageMode.value = selectedLanguage
                            localeManager.saveLanguage(selectedLanguage)
                            onLanguageChanged(selectedLanguage)
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
    } catch (_: Exception) {
        "1.0.0"
    }
}
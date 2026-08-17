package com.storm.lyricraft.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storm.lyricraft.R
import com.storm.lyricraft.utils.LocaleManager
import com.storm.lyricraft.utils.ThemeManager

@Composable
fun SettingScreen(
    hasStoragePermission: Boolean,
    appVersion: String,
    currentTheme: String,
    currentLanguage: String,
    onThemeSelected: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onOpenPermSetting: () -> Unit,
    onOpenGitHub: () -> Unit,
    onCheckUpdate: () -> Unit
) {
    val showThemeDialog = remember { mutableStateOf(false) }
    val showLanguageDialog = remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // 标题
            Text(
                text = stringResource(R.string.settings_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // ============ 外观 ============
            Text(
                text = stringResource(R.string.settings_appearance),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showThemeDialog.value = true }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.settings_theme),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = when (currentTheme) {
                            ThemeManager.THEME_LIGHT -> stringResource(R.string.settings_theme_light)
                            ThemeManager.THEME_DARK -> stringResource(R.string.settings_theme_dark)
                            else -> stringResource(R.string.settings_theme_system)
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ============ 语言 ============
            Text(
                text = stringResource(R.string.settings_language),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguageDialog.value = true }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.settings_language),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = when {
                            currentLanguage == LocaleManager.LANGUAGE_ZH -> "中文"
                            currentLanguage == LocaleManager.LANGUAGE_EN -> "English"
                            else -> {
                                val sysLang = LocaleManager.getCurrentLanguage()
                                if (sysLang.startsWith("zh")) "中文" else "English"
                            }
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ============ 权限 ============
            Text(
                text = stringResource(R.string.settings_permissions),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPermSetting() }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.settings_permission_storage),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = if (hasStoragePermission) stringResource(R.string.settings_permission_granted) else stringResource(R.string.settings_permission_denied),
                        fontSize = 14.sp,
                        color = if (hasStoragePermission) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ============ 关于 ============
            Text(
                text = stringResource(R.string.settings_about),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // 检查更新
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCheckUpdate() }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.settings_check_update),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // 版本号
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.settings_version),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = appVersion,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // 开发者
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenGitHub() }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.settings_developer),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "Blaze Du",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // ============ 主题对话框 ============
    if (showThemeDialog.value) {
        AlertDialog(
            onDismissRequest = { showThemeDialog.value = false },
            title = { Text(stringResource(R.string.settings_theme_dialog_title), fontSize = 20.sp) },
            text = {
                Column {
                    ThemeOption(
                        label = stringResource(R.string.settings_theme_system),
                        isSelected = currentTheme == ThemeManager.THEME_SYSTEM,
                        onClick = {
                            onThemeSelected(ThemeManager.THEME_SYSTEM)
                            showThemeDialog.value = false
                        }
                    )
                    ThemeOption(
                        label = stringResource(R.string.settings_theme_light),
                        isSelected = currentTheme == ThemeManager.THEME_LIGHT,
                        onClick = {
                            onThemeSelected(ThemeManager.THEME_LIGHT)
                            showThemeDialog.value = false
                        }
                    )
                    ThemeOption(
                        label = stringResource(R.string.settings_theme_dark),
                        isSelected = currentTheme == ThemeManager.THEME_DARK,
                        onClick = {
                            onThemeSelected(ThemeManager.THEME_DARK)
                            showThemeDialog.value = false
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }

    // ============ 语言对话框 ============
    if (showLanguageDialog.value) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog.value = false },
            title = { Text(stringResource(R.string.settings_language_dialog_title), fontSize = 20.sp) },
            text = {
                Column {
                    ThemeOption(
                        label = stringResource(R.string.settings_language_follow_system),
                        isSelected = currentLanguage.isEmpty(),
                        onClick = {
                            onLanguageSelected("")
                            showLanguageDialog.value = false
                        }
                    )
                    ThemeOption(
                        label = stringResource(R.string.settings_language_english),
                        isSelected = currentLanguage == LocaleManager.LANGUAGE_EN,
                        onClick = {
                            onLanguageSelected(LocaleManager.LANGUAGE_EN)
                            showLanguageDialog.value = false
                        }
                    )
                    ThemeOption(
                        label = stringResource(R.string.settings_language_chinese),
                        isSelected = currentLanguage == LocaleManager.LANGUAGE_ZH,
                        onClick = {
                            onLanguageSelected(LocaleManager.LANGUAGE_ZH)
                            showLanguageDialog.value = false
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun ThemeOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Text(
            text = label,
            fontSize = 16.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
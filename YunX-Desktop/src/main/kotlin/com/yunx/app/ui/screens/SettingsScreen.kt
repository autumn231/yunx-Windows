/*
 * YunX (云析) - A network drive share-link parser and high-speed downloader.
 * Desktop port of the Android app, Copyright (C) 2026 CYQawa
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Original source: https://github.com/CYQawa/YunX (AGPL-3.0)
 */

package com.yunx.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.yunx.app.ui.LocalAppWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yunx.app.data.download.DownloadPlatform
import com.yunx.app.data.prefs.SettingsRepository
import com.yunx.app.platform.DesktopPaths
import com.yunx.app.ui.AppGraph
import com.yunx.app.ui.SnackbarController
import com.yunx.app.ui.login.BaiduLoginWindow
import com.yunx.app.ui.login.QuarkLoginWindow
import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser

/** 可选的下载线程数档位（最高 512） */
private val threadOptions = listOf(1, 2, 4, 8, 16, 32, 64, 128, 256, 512)

/** 按平台下载线程数设置项（桌面版仅保留 夸克/百度/通用） */
private data class ThreadPlatform(val platform: String, val label: String)

private val threadPlatforms = listOf(
    ThreadPlatform(DownloadPlatform.QUARK, "夸克网盘"),
    ThreadPlatform(DownloadPlatform.BAIDU, "百度网盘"),
    ThreadPlatform(DownloadPlatform.GENERIC, "通用（手动添加）")
)

/** 速度限制选项（字节/秒；0 = 不限速） */
private val speedOptions = listOf(
    0L, 512L * 1024, 1024L * 1024, 2L * 1024 * 1024, 4L * 1024 * 1024,
    8L * 1024 * 1024, 16L * 1024 * 1024, 32L * 1024 * 1024
)

/** 最大同时下载任务数选项 */
private val concurrencyOptions = listOf(1, 2, 3, 4, 5, 6, 8, 10)

/** 失败自动重试次数选项 */
private val retryOptions = listOf(0, 1, 2, 3, 5, 8, 10)

/**
 * 设置页（桌面版，自上游 SettingsScreen 重新组织）：
 * - 账号管理（夸克/百度登录、退出）——上游分散在各网盘页，桌面版统一收纳于此；
 * - 下载设置（线程数/保存目录/并发/限速/重试）；
 * - 外观（深色模式）与通用（关闭最小化到托盘）；
 * - 关于（版本、开源协议、上游项目）。
 * 不移植：主题色、检查更新、日志导出、网盘认证备份。
 */
@Composable
fun SettingsScreen(
    appGraph: AppGraph,
    darkMode: Int,
    onDarkModeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = appGraph.settings
    val quarkAccount by appGraph.quarkAccountViewModel.quarkAccount.collectAsState()
    val baiduAccount by appGraph.baiduAccountViewModel.baiduAccount.collectAsState()

    // 各设置弹窗开关
    var showThreadsDialog by remember { mutableStateOf(false) }
    var showConcurrencyDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showRetryDialog by remember { mutableStateOf(false) }
    var showDarkModeDialog by remember { mutableStateOf(false) }
    // 登录窗口开关
    var showQuarkLogin by remember { mutableStateOf(false) }
    var showBaiduLogin by remember { mutableStateOf(false) }
    // 登出二次确认
    var showLogoutConfirm by remember { mutableStateOf<String?>(null) } // "quark" / "baidu"

    // 本地状态驱动 UI（同时同步持久化）
    var downloadDir by remember { mutableStateOf(settings.downloadDirUri) }
    var maxConcurrent by remember { mutableStateOf(settings.maxConcurrentDownloads) }
    var speedLimitBps by remember { mutableStateOf(settings.downloadSpeedLimit) }
    var retryCount by remember { mutableStateOf(settings.downloadRetryCount) }
    var minimizeToTray by remember { mutableStateOf(settings.minimizeToTray) }

    val awtWindow = LocalAppWindow.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
            SettingsSectionLabel("账号管理")

            AccountCard(
                icon = Icons.Outlined.Cloud,
                title = "夸克网盘",
                accountName = quarkAccount?.nickname?.takeIf { it.isNotBlank() } ?: "未登录",
                loggedIn = quarkAccount != null,
                onLogin = { showQuarkLogin = true },
                onLogout = { showLogoutConfirm = "quark" }
            )

            Spacer(modifier = Modifier.height(8.dp))

            AccountCard(
                icon = Icons.Outlined.AccountCircle,
                title = "百度网盘",
                accountName = baiduAccount?.nickname?.takeIf { it.isNotBlank() } ?: "未登录",
                loggedIn = baiduAccount != null,
                onLogin = { showBaiduLogin = true },
                onLogout = { showLogoutConfirm = "baidu" }
            )

            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionLabel("下载")

            SettingsItem(
                icon = Icons.Outlined.Tune,
                title = "下载线程数",
                description = "按网盘分别设置分片并发数（默认 32，最高 512）",
                onClick = { showThreadsDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsItem(
                icon = Icons.Outlined.FolderOpen,
                title = "下载保存目录",
                description = downloadDir ?: "系统默认「下载」文件夹（${DesktopPaths.defaultDownloadDir.absolutePath}）",
                onClick = {
                    val chooser = JFileChooser(
                        downloadDir?.let(::File) ?: DesktopPaths.defaultDownloadDir
                    )
                    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    chooser.isMultiSelectionEnabled = false
                    chooser.dialogTitle = "选择下载保存目录"
                    if (chooser.showOpenDialog(awtWindow) == JFileChooser.APPROVE_OPTION) {
                        val dir = chooser.selectedFile.absolutePath
                        settings.downloadDirUri = dir
                        downloadDir = dir
                        SnackbarController.show("下载保存目录已更新")
                    }
                },
                trailing = if (downloadDir != null) {
                    {
                        TextButton(onClick = {
                            settings.downloadDirUri = null
                            downloadDir = null
                            SnackbarController.show("已恢复默认下载目录")
                        }) {
                            Text(
                                text = "恢复默认",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    null
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsItem(
                icon = Icons.Outlined.Layers,
                title = "最大同时下载任务数",
                description = "同时下载 $maxConcurrent 个任务（其余排队，避免占满带宽）",
                onClick = { showConcurrencyDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsItem(
                icon = Icons.Outlined.Speed,
                title = "下载速度限制",
                description = speedLimitText(speedLimitBps),
                onClick = { showSpeedDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsItem(
                icon = Icons.Outlined.Refresh,
                title = "失败自动重试",
                description = if (retryCount == 0) "失败后不自动重试" else "失败后自动重试 $retryCount 次（断点续传）",
                onClick = { showRetryDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionLabel("外观")

            SettingsItem(
                icon = Icons.Outlined.DarkMode,
                title = "深色模式",
                description = when (darkMode) {
                    1 -> "浅色"
                    2 -> "深色"
                    else -> "跟随系统"
                },
                onClick = { showDarkModeDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionLabel("通用")

            SettingsItem(
                icon = Icons.Outlined.Layers,
                title = "关闭窗口时最小化到托盘",
                description = "关闭主窗口后程序驻留托盘继续下载；托盘右键菜单可退出",
                onClick = {
                    minimizeToTray = !minimizeToTray
                    settings.minimizeToTray = minimizeToTray
                },
                trailing = { Switch(checked = minimizeToTray, onCheckedChange = null) }
            )

            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionLabel("关于")

            SettingsItem(
                icon = Icons.Outlined.Info,
                title = "关于 YunX 桌面版",
                description = "v1.2.6 · AGPL-3.0 开源 · 上游：CYQawa/YunX",
                onClick = {
                    runCatching { Desktop.getDesktop().browse(java.net.URI("https://github.com/CYQawa/YunX")) }
                        .onFailure { SnackbarController.show("无法打开浏览器") }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "本程序为 YunX（AGPL-3.0）的桌面移植版，同样遵循 AGPL-3.0 开源。\n解析需登录网盘账号；直链由网盘官方接口生成，请合法使用。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // ---------- 登录窗口（独立 Compose Window） ----------

    if (showQuarkLogin) {
        QuarkLoginWindow(
            repository = appGraph.quarkAccountRepository,
            onBack = { showQuarkLogin = false },
            onSaved = {
                showQuarkLogin = false
                SnackbarController.show("夸克账号已登录")
            }
        )
    }
    if (showBaiduLogin) {
        BaiduLoginWindow(
            repository = appGraph.baiduAccountRepository,
            onBack = { showBaiduLogin = false },
            onSaved = {
                showBaiduLogin = false
                SnackbarController.show("百度账号已登录")
            }
        )
    }

    // ---------- 登出二次确认 ----------

    showLogoutConfirm?.let { platform ->
        val name = if (platform == "quark") "夸克" else "百度"
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = null },
            title = { Text("退出$name 账号") },
            text = { Text("确定退出${name}网盘登录吗？退出后需重新登录才能解析下载。") },
            confirmButton = {
                Button(
                    onClick = {
                        if (platform == "quark") {
                            appGraph.quarkAccountViewModel.logout()
                        } else {
                            appGraph.baiduAccountViewModel.logout()
                        }
                        showLogoutConfirm = null
                        SnackbarController.show("已退出${name}账号")
                    }
                ) { Text("退出登录") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = null }) { Text("取消") }
            }
        )
    }

    // ---------- 各设置弹窗 ----------

    // 线程数选择弹窗（按平台）
    if (showThreadsDialog) {
        var selectedPlatform by remember { mutableStateOf(threadPlatforms.first()) }
        var showPlatformThreadDialog by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showThreadsDialog = false },
            title = { Text("下载线程数") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "按网盘分别设置分片并发数；线程数不是越多越好，适当调整",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    threadPlatforms.forEach { item ->
                        val current = settings.downloadThreadsFor(item.platform)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPlatform = item
                                    showPlatformThreadDialog = true
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$current 线程",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Outlined.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThreadsDialog = false }) { Text("关闭") }
            }
        )

        // 单个平台线程数选择（二级弹窗）
        if (showPlatformThreadDialog) {
            val current = settings.downloadThreadsFor(selectedPlatform.platform)
            AlertDialog(
                onDismissRequest = { showPlatformThreadDialog = false },
                title = { Text("${selectedPlatform.label}线程数") },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        threadOptions.chunked(2).forEach { rowValues ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                rowValues.forEach { value ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                settings.setDownloadThreads(selectedPlatform.platform, value)
                                                showPlatformThreadDialog = false
                                            }
                                            .padding(vertical = 6.dp)
                                    ) {
                                        RadioButton(
                                            selected = value == current,
                                            onClick = {
                                                settings.setDownloadThreads(selectedPlatform.platform, value)
                                                showPlatformThreadDialog = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("$value 线程", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                if (rowValues.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPlatformThreadDialog = false }) { Text("取消") }
                }
            )
        }
    }

    // 最大同时下载任务数
    if (showConcurrencyDialog) {
        OptionPickerDialog(
            title = "最大同时下载任务数",
            options = concurrencyOptions,
            selected = maxConcurrent,
            optionText = { "$it 个" },
            onSelect = {
                settings.maxConcurrentDownloads = it
                maxConcurrent = it
                showConcurrencyDialog = false
            },
            onDismiss = { showConcurrencyDialog = false }
        )
    }

    // 下载速度限制
    if (showSpeedDialog) {
        OptionPickerDialog(
            title = "下载速度限制",
            options = speedOptions,
            selected = speedLimitBps,
            optionText = { speedLimitText(it) },
            onSelect = {
                settings.downloadSpeedLimit = it
                speedLimitBps = it
                showSpeedDialog = false
            },
            onDismiss = { showSpeedDialog = false }
        )
    }

    // 失败自动重试
    if (showRetryDialog) {
        OptionPickerDialog(
            title = "失败自动重试",
            options = retryOptions,
            selected = retryCount,
            optionText = { if (it == 0) "不重试" else "$it 次" },
            onSelect = {
                settings.downloadRetryCount = it
                retryCount = it
                showRetryDialog = false
            },
            onDismiss = { showRetryDialog = false }
        )
    }

    // 深色模式
    if (showDarkModeDialog) {
        val options = listOf(0, 1, 2)
        val labels = listOf("跟随系统", "浅色", "深色")
        AlertDialog(
            onDismissRequest = { showDarkModeDialog = false },
            title = { Text("深色模式") },
            text = {
                Column {
                    options.forEachIndexed { index, value ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDarkModeChange(value)
                                    showDarkModeDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = darkMode == value,
                                onClick = {
                                    onDarkModeChange(value)
                                    showDarkModeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(labels[index], style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDarkModeDialog = false }) { Text("取消") }
            }
        )
    }
}

/** 通用单选弹窗（速度限制/并发数/重试次数） */
@Composable
private fun <T> OptionPickerDialog(
    title: String,
    options: List<T>,
    selected: T,
    optionText: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())
            ) {
                options.forEach { value ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 6.dp)
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = { onSelect(value) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(optionText(value), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 账号卡片：图标 + 名称 + 登录状态 + 登录/退出按钮 */
@Composable
private fun AccountCard(
    icon: ImageVector,
    title: String,
    accountName: String,
    loggedIn: Boolean,
    onLogin: () -> Unit,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = if (loggedIn) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (loggedIn) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (loggedIn) "已登录 · $accountName" else "未登录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (loggedIn) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (loggedIn) {
                OutlinedButton(onClick = onLogout) {
                    Icon(
                        Icons.Outlined.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("退出")
                }
            } else {
                Button(onClick = onLogin) { Text("登录") }
            }
        }
    }
}

/** 设置分组标题 */
@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

/** 设置项卡片（自上游 SettingsItem 移植：去 ripple 依赖） */
@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun speedLimitText(bytesPerSec: Long): String = when {
    bytesPerSec <= 0 -> "不限速"
    bytesPerSec >= 1024 * 1024 -> "限制 ${bytesPerSec / 1024 / 1024} MB/s"
    else -> "限制 ${bytesPerSec / 1024} KB/s"
}

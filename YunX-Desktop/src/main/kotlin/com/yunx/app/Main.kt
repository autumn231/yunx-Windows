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

package com.yunx.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.yunx.app.platform.KcefManager
import com.yunx.app.platform.SystemTrayManager
import com.yunx.app.ui.AppGraph
import com.yunx.app.ui.GlobalSnackbarHost
import com.yunx.app.ui.LocalAppWindow
import com.yunx.app.ui.navigation.MainTab
import com.yunx.app.ui.screens.DownloadScreen
import com.yunx.app.ui.screens.ResolveScreen
import com.yunx.app.ui.screens.SettingsScreen
import com.yunx.app.ui.theme.YunXTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Frame

/**
 * 应用入口（桌面版）：
 * - 主窗口：左侧 NavigationRail（解析/下载/设置）+ 内容区；
 * - 系统托盘：关闭窗口驻留后台、进度 tooltip、完成通知（替代 Android 前台服务）；
 * - 启动时：恢复上次中断的下载任务（断点续传）、后台预热 KCEF（登录浏览器）。
 */
fun main() = application {
    val appGraph = remember { AppGraph() }
    val settings = appGraph.settings

    // 主题模式（设置页修改后即时生效）
    var darkMode by remember { mutableStateOf(settings.darkMode) }
    // 当前 Tab
    var currentTab by remember { mutableStateOf(MainTab.Resolve) }

    // 主窗口 AWT 引用（托盘「打开」/ 关闭最小化用）
    var mainFrame by remember { mutableStateOf<java.awt.Window?>(null) }

    /** 退出应用：移除托盘 → 释放 KCEF → 结束进程 */
    val exitApp: () -> Unit = {
        SystemTrayManager.remove()
        KcefManager.shutdown()
        exitApplication()
    }

    // 启动初始化：托盘 + 下载恢复 + KCEF 预热
    val appScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        // 下载进度 → 托盘（2 秒节流的回调，IO 线程，内部已切 EDT）
        appGraph.trayProgressListener = { event -> SystemTrayManager.updateProgress(event) }

        // 托盘注册（不支持托盘的系统上退化为关闭即退出）
        SystemTrayManager.setup(
            onOpen = {
                java.awt.EventQueue.invokeLater {
                    mainFrame?.isVisible = true
                    (mainFrame as? Frame)?.toFront()
                }
            },
            onExit = { exitApp() }
        )

        // 恢复上次中断的下载（断点续传）
        appScope.launch(Dispatchers.IO) {
            runCatching { appGraph.downloadManager.resumeOnStartup() }
        }

        // 后台预热 KCEF（首次运行需下载 JCEF 运行时约 150MB）
        appScope.launch(Dispatchers.IO) {
            KcefManager.ensureInitialized()
        }
    }

    val windowIcon = remember {
        BitmapPainter(SystemTrayManager.appIconImage().toComposeImageBitmap())
    }

    Window(
        title = "YunX 云析 - 网盘分享解析与高速下载",
        state = rememberWindowState(
            size = DpSize(1180.dp, 780.dp),
            position = WindowPosition(androidx.compose.ui.Alignment.Center)
        ),
        icon = windowIcon,
        resizable = true,
        onCloseRequest = {
            if (settings.minimizeToTray && SystemTrayManager.isRegistered) {
                // 最小化到托盘：进程驻留，下载继续
                mainFrame?.isVisible = false
            } else {
                exitApp()
            }
        }
    ) {
        // WindowScope.window：当前 Compose 主窗口的 AWT 引用
        val frame = window
        LaunchedEffect(frame) { mainFrame = frame }

        CompositionLocalProvider(LocalAppWindow provides frame) {
        YunXTheme(darkModeSetting = darkMode) {
            Row(modifier = Modifier.fillMaxSize()) {
                // 左侧导航栏（桌面版以 NavigationRail 替代 Android 底部 NavigationBar）
                NavigationRail(
                    header = {
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text(
                                text = "云析",
                                style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "YunX Desktop",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                ) {
                    MainTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == tab) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) }
                        )
                    }
                }

                androidx.compose.material3.VerticalDivider(
                    modifier = Modifier.fillMaxHeight().width(1.dp)
                )

                // 内容区（含全局 Snackbar）
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentTab) {
                        MainTab.Resolve -> ResolveScreen(
                            viewModel = appGraph.resolveViewModel,
                            onNavigateToDownload = { currentTab = MainTab.Download }
                        )
                        MainTab.Download -> DownloadScreen(viewModel = appGraph.downloadViewModel)
                        MainTab.Settings -> SettingsScreen(
                            appGraph = appGraph,
                            darkMode = darkMode,
                            onDarkModeChange = {
                                darkMode = it
                                settings.darkMode = it
                            }
                        )
                    }
                    GlobalSnackbarHost()
                }
            }
        }
        }
    }
}

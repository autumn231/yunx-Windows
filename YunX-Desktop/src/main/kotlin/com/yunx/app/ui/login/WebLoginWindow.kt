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

package com.yunx.app.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.yunx.app.platform.DesktopClipboard
import com.yunx.app.platform.KcefManager
import com.yunx.app.ui.SnackbarController
import com.yunx.app.ui.rememberGlobalSnackbarHostState
import dev.datlag.kcef.KCEF
import kotlinx.coroutines.launch
import org.cef.browser.CefBrowser
import org.cef.browser.CefRendering
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import javax.swing.JPanel

/** 内嵌浏览器登录页配置（夸克 / 百度共用同一外壳，按平台注入差异项） */
data class WebLoginConfig(
    /** 窗口标题，如 "夸克网盘登录" */
    val title: String,
    /** 登录页地址（QuarkConstants.LOGIN_URL / BaiduConstants.LOGIN_URL） */
    val loginUrl: String,
    /** 提取 Cookie 的 URL（QuarkConstants.COOKIE_DOMAIN / BaiduConstants.COOKIE_DOMAIN） */
    val cookieDomain: String,
    /** 廉价预检：Cookie 关键字段是否齐全（QuarkConstants.isValidCookie / BaiduConstants.isValidCookie） */
    val isValidCookie: (String) -> Boolean,
    /** 网络校验 + 落库（与手动「保存」共用同一入口），成功返回 true */
    val validateAndSave: suspend (String) -> Boolean,
    /** 手动粘贴 Cookie 的提示文案（"需包含 __pus= 与 __puus=" / "需包含 BDUSS="） */
    val manualCookieHint: String,
    /** 登录教程步骤文案 */
    val tutorialSteps: List<String>,
    /** 进入登录页优先展示风控提示（百度） */
    val riskWarning: String? = null
)

/**
 * 内嵌浏览器登录窗口（替代 Android 的 WebView 登录页，KCEF/JCEF 驱动）：
 * - 独立 OS 窗口，顶部标题栏：返回 + 手动输入 Cookie + 「保存」手动兜底；
 * - 主体：JCEF 加载网盘官网，用户手动登录；
 * - 自动登录检测：网页内登录完成后自动提取 Cookie 并校验落库（rememberWebLoginAutoDetect 原样移植）；
 * - KCEF 首次运行需下载运行时（约 150MB），未就绪时显示下载进度。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebLoginWindow(
    config: WebLoginConfig,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val windowState = rememberWindowState(
        size = DpSize(1120.dp, 780.dp),
        position = WindowPosition(Alignment.Center)
    )

    Window(
        state = windowState,
        title = config.title,
        onCloseRequest = onBack
    ) {
        val kcefState by KcefManager.state.collectAsState()

        // 确保初始化已启动（幂等；主窗口通常已触发）
        LaunchedEffect(Unit) { KcefManager.ensureInitialized() }

        when (val s = kcefState) {
            is KcefManager.State.Ready -> WebLoginContent(
                config = config,
                onBack = onBack,
                onSaved = onSaved
            )
            is KcefManager.State.Failed -> KcefFailedContent(
                message = s.message,
                onBack = onBack
            )
            is KcefManager.State.NotReady -> KcefPreparingContent(progress = s.downloadProgress)
        }
    }
}

/** KCEF 运行时就绪后的登录主体（创建浏览器 + 自动检测 + 手动兜底） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebLoginContent(
    config: WebLoginConfig,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // 手动输入 Cookie 弹窗状态
    var showCookieDialog by remember { mutableStateOf(false) }
    var cookieInput by remember { mutableStateOf("") }
    var isSavingManual by remember { mutableStateOf(false) }

    // 登录教程弹窗：进入页面即展示一次（百度先展示风控提示，确认后再展示教程）
    var showTutorial by remember { mutableStateOf(config.riskWarning == null) }
    var showRiskDialog by remember { mutableStateOf(config.riskWarning != null) }

    // ---------- JCEF 浏览器创建（替代 Android WebView） ----------
    var browser by remember { mutableStateOf<CefBrowser?>(null) }
    var client by remember { mutableStateOf<dev.datlag.kcef.KCEFClient?>(null) }

    LaunchedEffect(Unit) {
        // KCEF 已就绪（外层保证），newClient 可在 UI 线程调用
        val c = KCEF.newClient()
        c.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadingStateChange(
                browser: CefBrowser?,
                loading: Boolean,
                canGoBack: Boolean,
                canGoForward: Boolean
            ) {
                // CEF UI 线程回调 → Compose 状态（快照系统线程安全，自动调度重组）
                isLoading = loading
            }
        })
        val b = c.createBrowser(config.loginUrl, CefRendering.DEFAULT, false)
        client = c
        browser = b
    }

    DisposableEffect(Unit) {
        onDispose {
            // 关闭登录窗口：释放浏览器与客户端（CEF 资源必须显式回收）
            runCatching { browser?.close(true) }
            runCatching { client?.dispose() }
        }
    }

    // 自动登录检测：网页内登录完成（Cookie 出现并通过接口校验）即自动保存登录
    rememberWebLoginAutoDetect(
        sampleCredential = { CefCookieReader.cookieHeader(config.cookieDomain) },
        isPlausible = { config.isValidCookie(it) },
        validateAndSave = { config.validateAndSave(it) },
        isPaused = { isSaving || isSavingManual || showCookieDialog },
        onInFlightChange = { isSaving = it },
        onAutoSaved = onSaved
    )

    val snackbarHostState = rememberGlobalSnackbarHostState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(config.title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { if (!isSaving && !isSavingManual) onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 手动输入 Cookie
                    IconButton(
                        onClick = { if (!isSaving && !isSavingManual) showCookieDialog = true },
                        enabled = !isSaving && !isSavingManual
                    ) {
                        Icon(
                            Icons.Outlined.ContentPaste,
                            contentDescription = "手动输入 Cookie",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                val cookie = CefCookieReader.cookieHeader(config.cookieDomain)
                                val saved = config.validateAndSave(cookie)
                                isSaving = false
                                if (saved) {
                                    SnackbarController.show("登录成功")
                                    onSaved()
                                } else {
                                    SnackbarController.show("未检测到登录态，请先完成登录")
                                }
                            }
                        },
                        enabled = !isSaving && !isSavingManual
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("保存")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            browser?.let { b ->
                SwingPanel(
                    factory = {
                        JPanel().apply {
                            layout = BorderLayout()
                            add(b.uiComponent, BorderLayout.CENTER)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    // 风控温馨提示弹窗（百度进入登录页优先展示）
    if (showRiskDialog && config.riskWarning != null) {
        AlertDialog(
            onDismissRequest = { showRiskDialog = false },
            icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            title = { Text("温馨提示") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = config.riskWarning!!,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "请勿高频操作（频繁解析/转存/下载），如遇异常建议降低使用频率或稍后再试。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showRiskDialog = false
                    showTutorial = true
                }) { Text("我知道了") }
            }
        )
    }

    // 登录教程弹窗
    if (showTutorial) {
        AlertDialog(
            onDismissRequest = { showTutorial = false },
            icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            title = { Text("登录教程") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    config.tutorialSteps.forEachIndexed { index, step ->
                        Text(
                            text = "${index + 1}. $step",
                            style = if (index == config.tutorialSteps.lastIndex) {
                                MaterialTheme.typography.bodyMedium
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            color = if (index == config.tutorialSteps.lastIndex) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTutorial = false }) { Text("知道了") }
            }
        )
    }

    // 手动输入 Cookie 弹窗
    if (showCookieDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSavingManual) showCookieDialog = false },
            title = { Text("手动输入 Cookie") },
            text = {
                Column {
                    Text(
                        text = "从网页登录态复制完整的 Cookie（${config.manualCookieHint}）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cookieInput,
                        onValueChange = { cookieInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("粘贴 Cookie…") },
                        minLines = 4,
                        maxLines = 8
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            DesktopClipboard.readText()?.let { cookieInput = it }
                        }
                    ) { Text("从剪贴板粘贴") }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isSavingManual = true
                            val saved = config.validateAndSave(cookieInput.trim())
                            isSavingManual = false
                            if (saved) {
                                SnackbarController.show("登录成功")
                                showCookieDialog = false
                                onSaved()
                            } else {
                                SnackbarController.show("Cookie 无效，请检查${config.manualCookieHint}")
                            }
                        }
                    },
                    enabled = cookieInput.isNotBlank() && !isSavingManual
                ) {
                    if (isSavingManual) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("保存")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!isSavingManual) showCookieDialog = false },
                    enabled = !isSavingManual
                ) { Text("取消") }
            }
        )
    }
}

/** KCEF 运行时准备中（首次运行下载约 150MB，展示进度） */
@Composable
private fun KcefPreparingContent(progress: Float) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp))
            Text(
                text = if (progress in 0f..100f) {
                    "浏览器组件下载中 ${(progress.coerceIn(0f, 100f)).toInt()}%（仅首次运行需要）"
                } else {
                    "浏览器组件准备中（仅首次运行需要下载约 150MB）"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (progress in 0f..100f) {
                LinearProgressIndicator(
                    progress = { (progress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.widthIn(max = 320.dp)
                )
            }
        }
    }
}

/** KCEF 初始化失败（离线首启/磁盘问题） */
@Composable
private fun KcefFailedContent(message: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "内嵌浏览器不可用：$message",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "可关闭本窗口后改用「手动输入 Cookie」方式登录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onBack) { Text("返回") }
        }
    }
}

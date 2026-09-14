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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yunx.app.data.network.ShareLinkParser
import com.yunx.app.platform.DesktopClipboard
import com.yunx.app.ui.SnackbarController
import com.yunx.app.ui.viewmodel.ResolveUiState
import com.yunx.app.ui.viewmodel.ResolveViewModel

/**
 * 解析页（桌面版，自上游 ResolveScreen 移植）：
 * - 输入态：链接 + 提取码输入卡片、剪贴板分享链接提示；
 * - 加载态 / 错误态；
 * - 详情态：文件列表（ShareDetailScreen）。
 * 剪贴板检测：Android 的 ON_RESUME/剪贴板监听改为「窗口获得焦点时检测」（桌面端等价时机）。
 */
@Composable
fun ResolveScreen(
    viewModel: ResolveViewModel,
    onNavigateToDownload: () -> Unit
) {
    val state = viewModel.uiState
    val downloadError = viewModel.downloadError

    // 输入框状态提升到页面层：进入详情/文件夹再返回时不清空
    var link by rememberSaveable { mutableStateOf("") }
    var pwd by rememberSaveable { mutableStateOf("") }
    var pwdEdited by rememberSaveable { mutableStateOf(false) }

    // 剪贴板分享链接提示状态：待提示的剪贴板文本 + 已忽略的文本
    var clipboardSuggestion by rememberSaveable { mutableStateOf<String?>(null) }
    var ignoredClipboard by rememberSaveable { mutableStateOf<String?>(null) }

    // 窗口获得焦点时检测剪贴板（桌面端等价 Android ON_RESUME）；
    // 不做定时轮询：桌面剪贴板随时可读、无 Android 12+ 的隐私提示问题，焦点时机足够
    var windowFocused by remember { mutableStateOf(true) }

    val awtWindow = com.yunx.app.ui.LocalAppWindow.current
    androidx.compose.runtime.DisposableEffect(awtWindow) {
        val listener = object : java.awt.event.WindowFocusListener {
            override fun windowGainedFocus(e: java.awt.event.WindowEvent) {
                windowFocused = true
            }

            override fun windowLostFocus(e: java.awt.event.WindowEvent) {
                windowFocused = false
            }
        }
        awtWindow?.addWindowFocusListener(listener)
        onDispose { awtWindow?.removeWindowFocusListener(listener) }
    }

    val maybeSuggestClipboard: () -> Unit = {
        val text = DesktopClipboard.readText()
        if (text != null &&
            viewModel.uiState is ResolveUiState.Idle &&
            text.isNotBlank() &&
            text != link &&
            text != ignoredClipboard &&
            ShareLinkParser.parse(text.trim())?.platform.let { it == com.yunx.app.data.network.SharePlatform.QUARK || it == com.yunx.app.data.network.SharePlatform.BAIDU }
        ) {
            clipboardSuggestion = text.trim()
        }
    }
    // 获得焦点立即检测（复制链接后切回窗口即提示）
    LaunchedEffect(windowFocused) {
        if (windowFocused) maybeSuggestClipboard()
    }

    // 链接变化时自动匹配提取码（用户未手动输入时）
    LaunchedEffect(link) {
        if (!pwdEdited && pwd.isEmpty()) {
            ShareLinkParser.parse(link)?.pwd?.let { pwd = it }
        }
    }

    // 下载错误提示
    LaunchedEffect(downloadError) {
        downloadError?.let {
            SnackbarController.show(it)
            viewModel.consumeDownloadError()
        }
    }

    // 下载已入队：切换到下载页
    LaunchedEffect(viewModel.downloadStarted) {
        if (viewModel.downloadStarted) {
            onNavigateToDownload()
            viewModel.consumeDownloadStarted()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(140))
            },
            label = "resolveState"
        ) { s ->
            when (s) {
                is ResolveUiState.Detail -> ShareDetailScreen(
                    session = s.session,
                    files = s.files,
                    viewModel = viewModel,
                    onExit = { viewModel.backToInput() },
                    onBack = { viewModel.navigateBack() }
                )
                is ResolveUiState.Loading -> LoadingContent()
                is ResolveUiState.Error -> ResolveErrorContent(
                    message = s.message,
                    onRetry = { viewModel.startResolve(link, pwd.takeIf { it.isNotBlank() }) }
                )
                is ResolveUiState.Idle -> ResolveInputContent(
                    link = link,
                    onLinkChange = { link = it },
                    pwd = pwd,
                    onPwdChange = {
                        pwd = it
                        pwdEdited = true
                    },
                    onResolve = { viewModel.startResolve(link.trim(), pwd.takeIf { it.isNotBlank() }) },
                    onClearLink = {
                        link = ""
                        pwd = ""
                        pwdEdited = false
                    },
                    clipboardSuggestion = clipboardSuggestion,
                    onAcceptClipboard = {
                        clipboardSuggestion?.let {
                            link = it
                            // 自动匹配提取码
                            ShareLinkParser.parse(it)?.pwd?.let { p -> pwd = p }
                        }
                        clipboardSuggestion = null
                    },
                    onIgnoreClipboard = {
                        ignoredClipboard = clipboardSuggestion
                        clipboardSuggestion = null
                    }
                )
            }
        }
    }

    // 取链中：加载弹窗（临时转存 → 取直链，可能需要数秒）
    if (viewModel.isFetchingDownloadLink) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            title = { Text("正在获取下载链接") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("正在转存并获取直链，请稍候…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        )
    }

    // 下载直链弹窗
    viewModel.downloadLink?.let { link ->
        DownloadLinkDialog(
            link = link,
            onDownload = { viewModel.startDownload(link) },
            onDismiss = { viewModel.dismissDownloadDialog() }
        )
    }
}

/** 输入态：链接输入卡片 + 剪贴板提示卡片 */
@Composable
private fun ResolveInputContent(
    link: String,
    onLinkChange: (String) -> Unit,
    pwd: String,
    onPwdChange: (String) -> Unit,
    onResolve: () -> Unit,
    onClearLink: () -> Unit,
    clipboardSuggestion: String?,
    onAcceptClipboard: () -> Unit,
    onIgnoreClipboard: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题
            Column(
                modifier = Modifier.padding(top = 48.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "解析分享链接",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "支持夸克网盘 / 百度网盘分享链接（含提取码自动识别）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 剪贴板提示卡片
            if (clipboardSuggestion != null) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "检测到剪贴板中的分享链接",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = clipboardSuggestion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onAcceptClipboard) { Text("解析该链接") }
                            TextButton(onClick = onIgnoreClipboard) { Text("忽略") }
                        }
                    }
                }
            }

            // 链接输入卡片
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = link,
                        onValueChange = onLinkChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("分享链接") },
                        placeholder = { Text("粘贴夸克/百度分享链接…") },
                        singleLine = true,
                        trailingIcon = {
                            if (link.isNotEmpty()) {
                                IconButton(onClick = onClearLink) {
                                    Icon(Icons.Outlined.Close, contentDescription = "清空")
                                }
                            } else {
                                IconButton(onClick = {
                                    DesktopClipboard.readText()?.let { onLinkChange(it.trim()) }
                                }) {
                                    Icon(
                                        Icons.Outlined.ContentPaste,
                                        contentDescription = "从剪贴板粘贴"
                                    )
                                }
                            }
                        }
                    )
                    OutlinedTextField(
                        value = pwd,
                        onValueChange = onPwdChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("提取码（无提取码可留空）") },
                        placeholder = { Text("如：3v2f") },
                        singleLine = true
                    )
                    Button(
                        onClick = onResolve,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        enabled = link.isNotBlank()
                    ) {
                        Text("开始解析", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            // 使用说明
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "使用提示",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "· 解析需先在「设置 → 账号管理」登录对应网盘账号",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "· 带提取码的链接（如 链接 提取码：xxxx）会自动识别提取码",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "· 取直链时会临时转存到网盘「YunX临时转存」目录，完成后自动清理",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** 错误态 */
@Composable
private fun ResolveErrorContent(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "解析失败",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onRetry) { Text("重试") }
        }
    }
}

/** 加载态 */
@Composable
fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp))
            Text(
                text = "正在解析…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yunx.app.data.network.model.ShareFile
import com.yunx.app.data.network.model.ShareSession
import com.yunx.app.data.prefs.SettingsRepository
import com.yunx.app.ui.components.formatModifyTime
import com.yunx.app.ui.components.formatSize
import com.yunx.app.ui.viewmodel.ResolveViewModel
import kotlinx.coroutines.launch

/** 百度非会员限速阈值：>300MB 提示 */
private const val BAIDU_LIMIT_BYTES = 300L * 1024 * 1024

/**
 * 分享详情页（桌面版，自上游 ui/resolve/ShareDetailScreen 移植）：
 * 展示分享标题与文件列表，支持进入文件夹、点击文件获取下载直链、多选批量下载。
 * 桌面版差异：去掉书签/转存到网盘/其它网盘分支；多选以「多选」按钮 + 勾选框为主入口
 * （保留长按进入多选）；返回键由 onBack 回调承担（无 Android BackHandler）。
 */
@Composable
fun ShareDetailScreen(
    session: ShareSession,
    files: List<ShareFile>,
    viewModel: ResolveViewModel,
    onExit: () -> Unit,
    onBack: () -> Unit
) {
    val pathNames = viewModel.pathNames
    val settings = remember { SettingsRepository() }
    var baiduLimitDismissed by remember { mutableStateOf(settings.baiduLimitHintDismissed) }
    var showBaiduLimitDialog by remember { mutableStateOf(false) }
    var pendingBaiduAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    /** 百度分享下载前检查：>300MB 且未忽略时弹提示，确认后执行 */
    fun checkBaiduLimit(file: ShareFile, proceed: () -> Unit) {
        if (viewModel.isBaidu && !baiduLimitDismissed && file.fsize > BAIDU_LIMIT_BYTES) {
            pendingBaiduAction = proceed
            showBaiduLimitDialog = true
        } else {
            proceed()
        }
    }

    // 搜索过滤（本地过滤当前目录文件）
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    val displayFiles = remember(files, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) files else files.filter { it.fname.contains(q, ignoreCase = true) }
    }

    // 文件列表滚动状态 + 各目录滚动位置记忆（跨目录切换保留）
    val listState = rememberLazyListState()
    val scrollPositions = remember { mutableMapOf<String, Int>() }
    val currentDirKey = remember(pathNames) { pathNames.joinToString("/") }
    LaunchedEffect(currentDirKey) {
        listState.scrollToItem(scrollPositions[currentDirKey] ?: 0)
    }

    // 返回顶部按钮可见性（上滑离开顶部后显示）
    val showScrollTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 24.dp, end = 24.dp, top = 16.dp,
                bottom = if (viewModel.multiSelectMode) 110.dp else 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (viewModel.multiSelectMode) {
                            // 多选模式：取消选择
                            IconButton(onClick = { viewModel.exitMultiSelect() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "取消选择")
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "已选 ${viewModel.selected.size} 项",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (viewModel.selected.size == displayFiles.size) "已全选"
                                    else "点击文件行勾选/取消勾选",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { viewModel.toggleSelectAll(displayFiles) }) {
                                Text(if (viewModel.selected.size == displayFiles.size) "取消全选" else "全选")
                            }
                        } else {
                            IconButton(onClick = onExit) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.title.ifBlank { "分享内容" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (searchQuery.isBlank()) "共 ${files.size} 项 · ${viewModel.currentPlatformName}"
                                    else "匹配 ${displayFiles.size} / ${files.size} 项",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // 桌面版多选入口（替代 Android 长按）
                            IconButton(onClick = { viewModel.enterMultiSelectMode() }) {
                                Icon(
                                    imageVector = Icons.Outlined.Checklist,
                                    contentDescription = "多选",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { showSearch = !showSearch }) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = if (showSearch) "关闭搜索" else "搜索文件",
                                    tint = if (showSearch) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                    // 可点击面包屑（多选模式下隐藏）
                    if (!viewModel.multiSelectMode) {
                        CrumbBar(
                            rootTitle = session.title.ifBlank { "分享内容" },
                            pathNames = pathNames,
                            onNavigate = { level ->
                                scrollPositions[currentDirKey] = listState.firstVisibleItemIndex
                                viewModel.navigateToLevel(level)
                            }
                        )
                    }
                    // 搜索框（点击放大镜展开）
                    AnimatedVisibility(
                        visible = showSearch && !viewModel.multiSelectMode,
                        enter = expandVertically(tween(180)) + fadeIn(tween(180)),
                        exit = shrinkVertically(tween(140)) + fadeOut(tween(120))
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("搜索当前目录文件") },
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Filled.Close, contentDescription = "清空搜索")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = MaterialTheme.shapes.large
                            )
                        }
                    }
                }
            }

            // 返回上一级（单独列表项；根目录时不显示）
            if (pathNames.isNotEmpty()) {
                item {
                    BackToParentItem(onClick = {
                        scrollPositions[currentDirKey] = listState.firstVisibleItemIndex
                        onBack()
                    })
                }
            }

            if (displayFiles.isEmpty()) {
                item {
                    Text(
                        text = if (files.isEmpty()) "此目录为空" else "未找到匹配「${searchQuery.trim()}」的文件",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            items(displayFiles.size, key = { displayFiles[it].fid }) { index ->
                val file = displayFiles[index]
                ShareFileRow(
                    file = file,
                    onClick = {
                        if (viewModel.multiSelectMode) {
                            viewModel.toggleSelect(file)
                        } else if (file.isdir) {
                            scrollPositions[currentDirKey] = listState.firstVisibleItemIndex
                            viewModel.openFolder(file)
                        } else {
                            checkBaiduLimit(file) { viewModel.fetchDownloadLink(file) }
                        }
                    },
                    // 桌面版保留长按进入多选（鼠标按住不放）
                    onLongClick = if (!viewModel.multiSelectMode) {
                        { viewModel.enterMultiSelect(file) }
                    } else {
                        null
                    },
                    selected = viewModel.selected.contains(file),
                    showCheckbox = viewModel.multiSelectMode
                )
            }
        }

        // 返回顶部按钮（上滑离开顶部后显示；多选模式下上移避开底部批量栏）
        val listScope = rememberCoroutineScope()
        AnimatedVisibility(
            visible = showScrollTop,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 24.dp,
                    bottom = if (viewModel.multiSelectMode) 118.dp else 24.dp
                ),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp
            ) {
                IconButton(onClick = { listScope.launch { listState.animateScrollToItem(0) } }) {
                    Icon(
                        Icons.Outlined.VerticalAlignTop,
                        contentDescription = "返回顶部",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // 多选模式：底部批量操作栏（下载）
        if (viewModel.multiSelectMode) {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已选 ${viewModel.selected.size} 项",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.exitMultiSelect() }) { Text("取消") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            // 百度批量下载：选中项含 >300MB 文件时先弹限速提示
                            val hasBig = viewModel.selected.any { it.fsize > BAIDU_LIMIT_BYTES }
                            if (viewModel.isBaidu && !baiduLimitDismissed && hasBig) {
                                pendingBaiduAction = { viewModel.batchDownload() }
                                showBaiduLimitDialog = true
                            } else {
                                viewModel.batchDownload()
                            }
                        },
                        enabled = viewModel.selected.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("批量下载")
                    }
                }
            }
        }
    }

    // 百度 >300MB 限速提示弹窗（可勾选不再显示）
    if (showBaiduLimitDialog) {
        var neverShow by remember { mutableStateOf(baiduLimitDismissed) }
        AlertDialog(
            onDismissRequest = { showBaiduLimitDialog = false },
            title = { Text("下载大文件提示") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "百度网盘非会员超过300MB会被限速，下载速度可能较慢。是否继续下载？",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = neverShow, onCheckedChange = { neverShow = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("不再显示此提示", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBaiduLimitDialog = false
                        settings.baiduLimitHintDismissed = neverShow
                        baiduLimitDismissed = neverShow
                        pendingBaiduAction?.invoke()
                        pendingBaiduAction = null
                    }
                ) { Text("继续下载") }
            },
            dismissButton = {
                TextButton(onClick = { showBaiduLimitDialog = false }) { Text("取消") }
            }
        )
    }

    // 批量处理中：加载弹窗（批量下载显示获取进度，可中断）
    if (viewModel.isBatchWorking) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelBatch() }) {
                    Text("中断", color = MaterialTheme.colorScheme.error)
                }
            },
            title = { Text("批量处理中") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = viewModel.batchProgress?.let { "正在获取下载链接 $it" }
                            ?: "正在批量处理，请稍候…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        )
    }
}

@Composable
internal fun BackToParentItem(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowUpward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "返回上一级",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 可点击面包屑：根标题 > 目录1 > 目录2。
 * 非当前层可点击回退到对应目录；当前层高亮（文件夹图标 + 主题色）。
 * 横向滚动并自动定位到当前层。
 */
@Composable
internal fun CrumbBar(
    rootTitle: String,
    pathNames: List<String>,
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val crumbs = buildList {
        add(rootTitle.ifBlank { "根目录" })
        pathNames.forEach { add(it) }
    }
    val scroll = rememberScrollState()
    LaunchedEffect(crumbs.size, crumbs.lastOrNull()) {
        scroll.scrollTo(scroll.maxValue)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(start = 8.dp, top = 4.dp)
    ) {
        crumbs.forEachIndexed { i, name ->
            val isLast = i == crumbs.size - 1
            if (!isLast) {
                // 可点击层级：点击回退到该目录
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier
                        .clickable { onNavigate(i) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            } else {
                // 当前层：高亮 + 文件夹图标（不可点）
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ShareFileRow(
    file: ShareFile,
    onClick: () -> Unit,
    /** 长按进入多选（多选模式下为 null） */
    onLongClick: (() -> Unit)? = null,
    /** 多选模式：是否选中 */
    selected: Boolean = false,
    /** 是否显示行首复选框（仅多选模式传 true） */
    showCheckbox: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 多选模式：行首复选框
            if (showCheckbox) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = if (file.isdir) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (file.isdir) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (file.isdir) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fname,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
                Spacer(modifier = Modifier.height(2.dp))
                // 副标题行：文件夹/大小 + 修改时间
                Text(
                    text = buildString {
                        append(if (file.isdir) "文件夹" else formatSize(file.fsize))
                        val time = formatModifyTime(file.modifyTime)
                        if (time.isNotBlank()) {
                            append("  ·  ")
                            append(time)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

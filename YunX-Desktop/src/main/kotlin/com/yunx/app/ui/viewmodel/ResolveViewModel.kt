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

package com.yunx.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yunx.app.data.download.DownloadManager
import com.yunx.app.data.download.DownloadPlatform
import com.yunx.app.data.network.BaiduConstants
import com.yunx.app.data.network.QuarkCdn
import com.yunx.app.data.network.QuarkConstants
import com.yunx.app.data.network.ShareLinkParser
import com.yunx.app.data.network.SharePlatform
import com.yunx.app.data.network.model.DownloadLink
import com.yunx.app.data.network.model.ShareFile
import com.yunx.app.data.network.model.ShareSession
import com.yunx.app.data.repository.BaiduAccountRepository
import com.yunx.app.data.repository.BaiduResolveRepository
import com.yunx.app.data.repository.QuarkAccountRepository
import com.yunx.app.data.repository.QuarkResolveRepository
import com.yunx.app.data.repository.ShareResolveRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

sealed interface ResolveUiState {
    data object Idle : ResolveUiState
    data object Loading : ResolveUiState
    data class Detail(val session: ShareSession, val files: List<ShareFile>) : ResolveUiState
    data class Error(val message: String) : ResolveUiState
}

/**
 * 解析页 ViewModel（桌面版，夸克 + 百度）：
 * 分享解析状态机 + 目录导航 + 多选批量下载 + 下载直链。
 * 自上游 Android 版移植：去掉 androidx.lifecycle（进程常驻单例，作用域随应用存活），
 * 去掉 UC/迅雷/139/123 网盘分支与书签/转存到网盘（桌面版不移植）。
 */
class ResolveViewModel(
    private val quarkAccountRepository: QuarkAccountRepository,
    private val quarkResolveRepository: QuarkResolveRepository,
    private val baiduAccountRepository: BaiduAccountRepository,
    private val baiduResolveRepository: BaiduResolveRepository,
    private val downloadManager: DownloadManager
) {
    /** 应用级作用域（桌面进程常驻，无需手动清理） */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var uiState by mutableStateOf<ResolveUiState>(ResolveUiState.Idle)
        private set

    var downloadLink by mutableStateOf<DownloadLink?>(null)
        private set

    var downloadError by mutableStateOf<String?>(null)
        private set

    /** 获取下载直链中（UI 显示加载弹窗） */
    var isFetchingDownloadLink by mutableStateOf(false)
        private set

    /** 下载已入队事件：触发后由 UI 切换到下载页 */
    var downloadStarted by mutableStateOf(false)
        private set

    // ---------- 多选（解析页文件列表；桌面版以勾选框代替长按） ----------

    /** 多选模式 */
    var multiSelectMode by mutableStateOf(false)
        private set

    private val _selected = mutableStateListOf<ShareFile>()
    val selected: List<ShareFile> get() = _selected

    /** 批量处理中（UI 显示加载弹窗） */
    var isBatchWorking by mutableStateOf(false)
        private set

    /** 批量下载进度（如 "2/5"）；null 表示未显示进度 */
    var batchProgress by mutableStateOf<String?>(null)
        private set

    /** 批量处理中断请求（UI 点「中断」后置 true，批量循环中检查并跳出） */
    private var batchCancelRequested = false

    /** 中断当前批量处理 */
    fun cancelBatch() {
        batchCancelRequested = true
    }

    fun enterMultiSelect(file: ShareFile) {
        multiSelectMode = true
        _selected.clear()
        _selected.add(file)
    }

    /** 进入多选模式（不预选文件；桌面版「多选」按钮入口，替代 Android 长按） */
    fun enterMultiSelectMode() {
        multiSelectMode = true
    }

    fun toggleSelect(file: ShareFile) {
        if (_selected.contains(file)) _selected.remove(file) else _selected.add(file)
        if (_selected.isEmpty()) multiSelectMode = false
    }

    fun toggleSelectAll(files: List<ShareFile>) {
        if (_selected.size == files.size) _selected.clear() else {
            _selected.clear()
            _selected.addAll(files)
        }
    }

    fun exitMultiSelect() {
        multiSelectMode = false
        _selected.clear()
    }

    /** 批量下载：逐个取直链入队（选中文件夹时递归下载整个文件夹并保持目录结构，全部获取完再统一切到下载页） */
    fun batchDownload() {
        val files = _selected.toList()
        val s = session ?: return
        scope.launch {
            isBatchWorking = true
            batchProgress = "正在收集文件…"
            batchCancelRequested = false
            try {
                val credential = currentCredential()
                if (credential.isNullOrBlank()) {
                    downloadError = "请先登录网盘"
                    return@launch
                }
                // 夸克：取链与下载必须用同一份已刷新 Cookie（直链签名绑定取链时刻的 __puus）
                val quarkCred = if (currentPlatform == SharePlatform.QUARK) {
                    quarkAccountRepository.getFreshCookie() ?: credential
                } else {
                    credential
                }
                // 展开选中项：文件直接加入，文件夹递归收集（相对路径 = 文件夹名/子/...）
                val tasks = mutableListOf<Pair<ShareFile, String>>()
                for (file in files) {
                    if (file.isdir) {
                        collectShareFolder(s, file.fid, file.fname, quarkCred, tasks, 0)
                    } else {
                        tasks.add(file to "")
                    }
                }
                if (tasks.isEmpty()) {
                    downloadError = "所选文件夹为空"
                    exitMultiSelect()
                    return@launch
                }
                var okCount = 0
                var interrupted = false
                for ((index, task) in tasks.withIndex()) {
                    // 用户点击「中断」：停止剩余项，已入队的任务保留下载
                    if (batchCancelRequested) {
                        interrupted = true
                        downloadError = "已中断批量下载"
                        break
                    }
                    val (file, relPath) = task
                    batchProgress = "${index + 1}/${tasks.size}"
                    runCatching {
                        currentRepo().getShareDownloadLink(s, file, quarkCred).getOrNull()?.let { link ->
                            // 文件夹内文件用相对路径（保持目录结构）；根目录文件用取链返回的文件名
                            enqueueDownload(link, quarkCred, if (relPath.isBlank()) link.filename else relPath)
                            okCount++
                        }
                    }
                }
                if (!interrupted) {
                    downloadError = if (okCount > 0) "已加入 $okCount 个下载任务" else "获取下载链接失败"
                    // 全部获取完再一次性切到下载页
                    if (okCount > 0) downloadStarted = true
                }
                exitMultiSelect()
            } finally {
                isBatchWorking = false
                batchProgress = null
                batchCancelRequested = false
            }
        }
    }

    /**
     * 递归收集分享文件夹内所有文件（保持目录结构）。
     * @param dirFid 分享内目录 fid
     * @param prefix 相对路径前缀（如 "文件夹A/子目录"）
     * @param result 输出：文件 + 相对路径（"文件夹A/子目录/文件.mp4"）
     */
    private suspend fun collectShareFolder(
        s: ShareSession,
        dirFid: String,
        prefix: String,
        credential: String,
        result: MutableList<Pair<ShareFile, String>>,
        depth: Int
    ) {
        if (depth > 12) return
        val files = runCatching {
            currentRepo().listFiles(s, dirFid, credential).getOrNull() ?: emptyList()
        }.getOrDefault(emptyList())
        files.filter { !it.isdir }.forEach { result.add(it to "$prefix/${it.fname}") }
        files.filter { it.isdir }.forEach {
            collectShareFolder(s, it.fid, "$prefix/${it.fname}", credential, result, depth + 1)
        }
    }

    fun consumeDownloadStarted() {
        downloadStarted = false
    }

    fun consumeDownloadError() {
        downloadError = null
    }

    private var session: ShareSession? = null
    private var currentDirFid = QuarkConstants.DEFAULT_PDIR_FID
    private val dirStack = ArrayDeque<String>()

    /** 当前目录路径名栈（用于面包屑显示），如 [辅助工具, 专用模组] */
    var pathNames by mutableStateOf<List<String>>(emptyList())
        private set

    /** 当前解析平台（QUARK / BAIDU），由链接自动检测 */
    private var currentPlatform: SharePlatform = SharePlatform.QUARK

    /** 当前平台凭证（cookie） */
    private suspend fun currentCredential(): String? = when (currentPlatform) {
        SharePlatform.BAIDU -> baiduAccountRepository.getAccount()?.cookie
        else -> quarkAccountRepository.getAccount()?.cookie
    }

    private fun currentRepo(): ShareResolveRepository = when (currentPlatform) {
        SharePlatform.BAIDU -> baiduResolveRepository
        else -> quarkResolveRepository
    }

    private fun currentDefaultDirFid(): String = when (currentPlatform) {
        SharePlatform.BAIDU -> ""
        else -> QuarkConstants.DEFAULT_PDIR_FID
    }

    private fun platformName(): String = when (currentPlatform) {
        SharePlatform.BAIDU -> "百度网盘"
        else -> "夸克网盘"
    }

    /** 当前解析的平台名（UI 展示） */
    val currentPlatformName: String get() = platformName()

    /** 当前解析的是否为百度分享（UI 限速提示用） */
    val isBaidu: Boolean get() = currentPlatform == SharePlatform.BAIDU

    /** 开始解析：链接 → token →（密码）→ 根目录列表 */
    fun startResolve(link: String, pwd: String?) {
        scope.launch {
            uiState = ResolveUiState.Loading
            val parsed = ShareLinkParser.parse(link)
            if (parsed == null) {
                uiState = ResolveUiState.Error("无法识别分享链接")
                return@launch
            }
            when (parsed.platform) {
                SharePlatform.QUARK, SharePlatform.BAIDU -> Unit
                // 桌面版仅支持夸克/百度；其余平台给出明确提示
                else -> {
                    uiState = ResolveUiState.Error("桌面版暂不支持该网盘（仅支持夸克/百度）")
                    return@launch
                }
            }
            currentPlatform = parsed.platform
            val credential = currentCredential()
            if (credential.isNullOrBlank()) {
                uiState = ResolveUiState.Error("请先在「设置 → 账号管理」登录${platformName()}")
                return@launch
            }
            val repo = currentRepo()
            repo.createSession(link, pwd, credential)
                .onSuccess { s ->
                    session = s
                    currentDirFid = currentDefaultDirFid()
                    dirStack.clear()
                    pathNames = emptyList()
                    loadFiles(s, currentDirFid, credential, repo)
                }
                .onFailure { e ->
                    uiState = ResolveUiState.Error(e.message ?: "解析失败")
                }
        }
    }

    /** 进入文件夹 */
    fun openFolder(file: ShareFile) {
        val s = session ?: return
        dirStack.addLast(currentDirFid)
        pathNames = pathNames + file.fname
        currentDirFid = file.fid
        scope.launch {
            uiState = ResolveUiState.Loading
            val credential = currentCredential()
            if (credential.isNullOrBlank()) {
                uiState = ResolveUiState.Error("登录已失效，请重新登录")
                return@launch
            }
            loadFiles(s, file.fid, credential, currentRepo())
        }
    }

    /** 返回上级目录 */
    fun goBack() {
        val s = session ?: return
        if (dirStack.isEmpty()) return
        currentDirFid = dirStack.removeLast()
        pathNames = pathNames.dropLast(1)
        scope.launch {
            uiState = ResolveUiState.Loading
            val credential = currentCredential()
            if (credential.isNullOrBlank()) return@launch
            loadFiles(s, currentDirFid, credential, currentRepo())
        }
    }

    /** 返回：在子目录则返回上一级，在根目录则返回输入页 */
    fun navigateBack() {
        if (dirStack.isEmpty()) {
            backToInput()
        } else {
            goBack()
        }
    }

    /** 返回输入页 */
    fun backToInput() {
        session = null
        downloadLink = null
        pathNames = emptyList()
        exitMultiSelect()
        uiState = ResolveUiState.Idle
    }

    /**
     * 面包屑导航：点击第 level 级（0=分享根目录）回退到该目录并刷新列表。
     * 当前所在层（level == pathNames.size）无需操作。
     */
    fun navigateToLevel(level: Int) {
        val s = session ?: return
        if (level < 0 || level > pathNames.size) return
        if (level == pathNames.size) return
        // 弹出目录栈直到对应层级；level=0 时回到分享根目录
        while (dirStack.size > level) dirStack.removeLast()
        currentDirFid = if (dirStack.isEmpty()) currentDefaultDirFid() else dirStack.last()
        pathNames = pathNames.take(level)
        scope.launch {
            val credential = currentCredential() ?: return@launch
            loadFiles(s, currentDirFid, credential, currentRepo())
        }
    }

    /** 获取文件下载直链（夸克：临时转存后取；百度：转存 → locatedownload → 立即清理） */
    fun fetchDownloadLink(file: ShareFile) {
        scope.launch {
            downloadLink = null
            downloadError = null
            isFetchingDownloadLink = true
            try {
                val s = session
                if (s == null) {
                    downloadError = "请先解析分享"
                    return@launch
                }
                val credential = currentCredential()
                if (credential.isNullOrBlank()) {
                    downloadError = "登录已失效，请重新登录"
                    return@launch
                }
                // 夸克：取链前确保 __puus 新鲜（直链签名绑定取链时刻的 Cookie）
                val quarkCred = if (currentPlatform == SharePlatform.QUARK) {
                    quarkAccountRepository.getFreshCookie() ?: credential
                } else {
                    credential
                }
                currentRepo().getShareDownloadLink(s, file, quarkCred)
                    .onSuccess { downloadLink = it }
                    .onFailure { downloadError = it.message ?: "获取下载链接失败" }
            } finally {
                isFetchingDownloadLink = false
            }
        }
    }

    fun dismissDownloadDialog() {
        val link = downloadLink
        downloadLink = null
        // 弹窗被关闭（用户未开始下载）：清理夸克临时转存，避免云端残留
        if (link?.cleanupDirFid != null) {
            scope.launch {
                val credential = quarkAccountRepository.getAccount()?.cookie ?: return@launch
                link.cleanupDirFid?.let { dirFid ->
                    quarkResolveRepository.cleanupTempDir(dirFid, credential)
                }
            }
        }
    }

    /**
     * 将直链加入下载队列（携带对应平台凭证与 UA；夸克直链做 CDN 节点优选）。
     * 不触发切页 —— 批量下载全部入队后才统一切到下载页。
     */
    private suspend fun enqueueDownload(
        link: DownloadLink,
        credential: String,
        fileName: String = link.filename
    ) {
        val isBaidu = currentPlatform == SharePlatform.BAIDU
        val isQuark = currentPlatform == SharePlatform.QUARK
        val platform = if (isBaidu) DownloadPlatform.BAIDU else DownloadPlatform.QUARK
        // 夸克：取链与下载必须用同一份已刷新 Cookie（AlistGo/alist#830 类缺陷）；
        // getFreshCookie 有 90 分钟间隔保护，与取链处调用幂等，得到的是同一份。
        val effectiveCredential = if (isQuark) {
            quarkAccountRepository.getFreshCookie() ?: credential
        } else {
            credential
        }
        // 百度直链需 Cookie + 客户端 UA；夸克需 Cookie + API UA + 固定 Referer（防盗链）
        val headers = if (isBaidu) {
            mapOf(
                "Cookie" to credential,
                "User-Agent" to BaiduConstants.UA_NETDISK
            )
        } else {
            mapOf(
                "Cookie" to effectiveCredential,
                "User-Agent" to QuarkConstants.API_USER_AGENT,
                "Referer" to QuarkConstants.DOWNLOAD_REFERER
            )
        }
        // 夸克直链：CDN 节点优选（原样使用直链签名，不改写 URL）
        val effectiveUrl = if (isQuark) {
            QuarkCdn.fastest(link.downloadUrl, effectiveCredential)
        } else {
            link.downloadUrl
        }
        downloadManager.enqueue(
            url = effectiveUrl,
            fileName = fileName,
            headers = headers,
            size = link.size,
            platform = platform
        ) {
            // 下载完成：清理网盘临时转存目录（百度取链时已即时清理，仅夸克需要）；失败/取消不触发
            val dirFid = link.cleanupDirFid
            if (dirFid != null) {
                val credential2 = currentCredential()
                if (!credential2.isNullOrBlank()) {
                    quarkResolveRepository.cleanupTempDir(dirFid, credential2)
                }
            }
        }
    }

    /** 将直链加入下载队列（单文件下载：入队后立即切换到下载页） */
    fun startDownload(link: DownloadLink) {
        scope.launch {
            // 开始下载：先关闭弹窗（临时转存由下载完成 onComplete 清理，不在此时删）
            downloadLink = null
            val credential = currentCredential()
            if (credential.isNullOrBlank()) {
                downloadError = "请先登录网盘"
                return@launch
            }
            enqueueDownload(link, credential)
            downloadStarted = true
        }
    }

    private suspend fun loadFiles(
        s: ShareSession,
        dirFid: String,
        credential: String,
        repo: ShareResolveRepository
    ) {
        repo.listFiles(s, dirFid, credential)
            .onSuccess { files ->
                uiState = ResolveUiState.Detail(s, files)
            }
            .onFailure { e ->
                uiState = ResolveUiState.Error(e.message ?: "获取文件列表失败")
            }
    }
}

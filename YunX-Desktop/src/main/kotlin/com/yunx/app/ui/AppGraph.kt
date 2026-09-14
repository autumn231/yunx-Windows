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

package com.yunx.app.ui

import com.yunx.app.data.db.AppDatabase
import com.yunx.app.data.download.ChunkDownloader
import com.yunx.app.data.download.DownloadManager
import com.yunx.app.data.download.DownloadProgressEvent
import com.yunx.app.data.network.BaiduApi
import com.yunx.app.data.network.BaiduConstants
import com.yunx.app.data.network.HttpClients
import com.yunx.app.data.network.QuarkApi
import com.yunx.app.data.network.QuarkConstants
import com.yunx.app.data.prefs.SettingsRepository
import com.yunx.app.data.repository.BaiduAccountRepository
import com.yunx.app.data.repository.BaiduResolveRepository
import com.yunx.app.data.repository.QuarkAccountRepository
import com.yunx.app.data.repository.QuarkResolveRepository
import com.yunx.app.platform.DesktopPaths
import com.yunx.app.ui.login.CefCookieReader
import com.yunx.app.ui.viewmodel.BaiduAccountViewModel
import com.yunx.app.ui.viewmodel.DownloadViewModel
import com.yunx.app.ui.viewmodel.QuarkAccountViewModel
import com.yunx.app.ui.viewmodel.ResolveViewModel
import java.io.File

/**
 * 应用依赖容器（替代 Android 的 Application + ViewModelProvider.Factory）：
 * 进程常驻单例，组装 数据库 / API / 仓库 / 下载引擎 / ViewModel 全链路。
 */
class AppGraph {

    /** 设置持久化（java.util.prefs） */
    val settings = SettingsRepository()

    /** Room 数据库（桌面端 BundledSQLite 驱动） */
    val database = AppDatabase.get(File(DesktopPaths.dataDir, "yunx.db"))

    // ---------- 网络层（原样复用上游） ----------
    val quarkApi = QuarkApi()
    val baiduApi = BaiduApi()

    // ---------- 账号仓库（Cookie 校验/刷新/登出） ----------
    val quarkAccountRepository = QuarkAccountRepository(database.quarkAccountDao(), quarkApi)
    val baiduAccountRepository = BaiduAccountRepository(database.baiduAccountDao(), baiduApi)

    // ---------- 解析仓库（转存/取直链/清理） ----------
    val quarkResolveRepository = QuarkResolveRepository(quarkApi)
    val baiduResolveRepository = BaiduResolveRepository(baiduApi)

    // ---------- 下载引擎（分片并发 + 断点续传，设置动态生效） ----------
    val downloadManager = DownloadManager(
        dao = database.downloadTaskDao(),
        downloader = ChunkDownloader { HttpClients.downloadClient() },
        threadProvider = { platform -> settings.downloadThreadsFor(platform) },
        saveDirProvider = { settings.downloadDirUri },
        concurrencyProvider = { settings.maxConcurrentDownloads },
        speedLimitProvider = { settings.downloadSpeedLimit },
        retryCountProvider = { settings.downloadRetryCount },
        onProgressEvent = { event -> trayProgressListener?.invoke(event) }
    )

    /** 系统托盘进度监听（Main.kt 启动时注入，替代 Android 前台服务通知） */
    var trayProgressListener: ((DownloadProgressEvent) -> Unit)? = null

    // ---------- ViewModel（进程常驻，等价 Android Activity 作用域 ViewModel） ----------
    val resolveViewModel = ResolveViewModel(
        quarkAccountRepository = quarkAccountRepository,
        quarkResolveRepository = quarkResolveRepository,
        baiduAccountRepository = baiduAccountRepository,
        baiduResolveRepository = baiduResolveRepository,
        downloadManager = downloadManager
    )

    val downloadViewModel = DownloadViewModel(downloadManager)

    val quarkAccountViewModel = QuarkAccountViewModel(quarkAccountRepository)

    val baiduAccountViewModel = BaiduAccountViewModel(baiduAccountRepository)

    init {
        // 登出时清理内嵌浏览器 Cookie（替代 Android WebView CookieManager.removeAllCookies）
        quarkAccountRepository.cookieClearer = {
            CefCookieReader.clearCookies(QuarkConstants.COOKIE_DOMAIN)
        }
        baiduAccountRepository.cookieClearer = {
            CefCookieReader.clearCookies(BaiduConstants.COOKIE_DOMAIN)
        }
    }
}

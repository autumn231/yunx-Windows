/*
 * YunX (云析) - A network drive share-link parser and high-speed downloader for Android.
 * Copyright (C) 2026 CYQawa
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
 */

package com.yunx.app.platform

import java.io.File

/**
 * 桌面端目录约定（替代 Android Context 的 cacheDir / externalCacheDir / Download）：
 * - Windows：%APPDATA%\YunX（数据）/ %LOCALAPPDATA%\YunX\cache（缓存）
 * - 其他平台：~/.yunx / ~/.yunx-cache（开发调试用）
 */
object DesktopPaths {

    private val isWindows = System.getProperty("os.name").contains("Windows", ignoreCase = true)

    /** 应用数据目录（数据库、日志、密钥） */
    val dataDir: File by lazy {
        if (isWindows) {
            File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "YunX")
        } else {
            File(System.getProperty("user.home"), ".yunx")
        }.apply { mkdirs() }
    }

    /** 缓存目录（下载分片临时文件、合并中转文件、JCEF 安装目录） */
    val cacheDir: File by lazy {
        if (isWindows) {
            File(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "YunX/cache")
        } else {
            File(System.getProperty("user.home"), ".yunx-cache")
        }.apply { mkdirs() }
    }

    /** 默认下载保存目录：用户「下载」文件夹 */
    val defaultDownloadDir: File by lazy {
        val userHome = File(System.getProperty("user.home"))
        val win = File(System.getenv("USERPROFILE") ?: userHome.path, "Downloads")
        val guess = if (isWindows && win.isDirectory) win else File(userHome, "Downloads")
        if (guess.isDirectory) guess else userHome
    }
}

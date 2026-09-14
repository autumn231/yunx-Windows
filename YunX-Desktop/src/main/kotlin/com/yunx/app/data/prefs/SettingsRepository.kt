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

package com.yunx.app.data.prefs

import com.yunx.app.data.download.DownloadPlatform
import java.util.prefs.Preferences

/**
 * 应用设置（java.util.prefs 持久化，替代 SharedPreferences）。
 * 键名与默认值与上游 Android 版保持一致。
 */
class SettingsRepository {

    private val prefs: Preferences = Preferences.userNodeForPackage(SettingsRepository::class.java)

    /** 下载线程数（通用/手动添加，分片并发数），默认 32，上限 512 */
    var downloadThreads: Int
        get() = downloadThreadsFor(DownloadPlatform.GENERIC)
        set(value) = setDownloadThreads(DownloadPlatform.GENERIC, value)

    /** 获取指定平台的下载线程数；默认 32、上限 512 */
    fun downloadThreadsFor(platform: String): Int {
        return prefs.getInt(prefsKey(platform), DEFAULT_DOWNLOAD_THREADS)
            .coerceIn(1, MAX_DOWNLOAD_THREADS)
    }

    /** 设置指定平台的下载线程数 */
    fun setDownloadThreads(platform: String, value: Int) {
        prefs.putInt(prefsKey(platform), value.coerceIn(1, MAX_DOWNLOAD_THREADS))
    }

    private fun prefsKey(platform: String): String =
        if (platform.isBlank() || platform == DownloadPlatform.GENERIC) "download_threads"
        else "download_threads_$platform"

    /** 自定义下载保存目录（本地绝对路径）；null/空 = 用户「下载」文件夹 */
    var downloadDirUri: String?
        get() = prefs.get("download_dir_uri", null)
        set(value) {
            if (value == null) prefs.remove("download_dir_uri") else prefs.put("download_dir_uri", value)
        }

    /** 最大同时下载任务数（默认 1：前台任务吃满带宽，其余排队；参考 IDM 默认单任务满速） */
    var maxConcurrentDownloads: Int
        get() = prefs.getInt("max_concurrent_downloads", DEFAULT_MAX_CONCURRENT_DOWNLOADS)
        set(value) {
            prefs.putInt("max_concurrent_downloads", value.coerceIn(1, 10))
        }

    /** 下载速度限制（字节/秒；0 = 不限速） */
    var downloadSpeedLimit: Long
        get() = prefs.getLong("download_speed_limit", 0L)
        set(value) {
            prefs.putLong("download_speed_limit", value.coerceAtLeast(0L))
        }

    /** 下载失败后自动重试次数（默认 3，范围 0-10） */
    var downloadRetryCount: Int
        get() = prefs.getInt("download_retry_count", DEFAULT_DOWNLOAD_RETRY_COUNT)
        set(value) {
            prefs.putInt("download_retry_count", value.coerceIn(0, 10))
        }

    /** 关闭窗口时最小化到系统托盘（而非退出程序）；下载中仍建议保持托盘运行 */
    var minimizeToTray: Boolean
        get() = prefs.getBoolean("minimize_to_tray", true)
        set(value) {
            prefs.putBoolean("minimize_to_tray", value)
        }

    /** 深色模式：0=跟随系统，1=浅色，2=深色 */
    var darkMode: Int
        get() = prefs.getInt("dark_mode", 0)
        set(value) {
            prefs.putInt("dark_mode", value.coerceIn(0, 2))
        }

    /** 百度网盘大文件限速提示：是否已选择「不再显示」 */
    var baiduLimitHintDismissed: Boolean
        get() = prefs.getBoolean("baidu_limit_hint_dismissed", false)
        set(value) {
            prefs.putBoolean("baidu_limit_hint_dismissed", value)
        }

    companion object {
        const val DEFAULT_DOWNLOAD_THREADS = 32
        const val MAX_DOWNLOAD_THREADS = 512
        const val DEFAULT_MAX_CONCURRENT_DOWNLOADS = 1
        const val DEFAULT_DOWNLOAD_RETRY_COUNT = 3
    }
}

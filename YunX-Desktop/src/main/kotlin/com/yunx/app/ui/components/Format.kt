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

package com.yunx.app.ui.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** 文件大小格式化（自上游 ShareDetailScreen.formatSize 原样移植） */
fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var i = 0
    while (value >= 1024 && i < units.size - 1) {
        value /= 1024
        i++
    }
    return String.format("%.1f %s", value, units[i])
}

/** 下载速度格式化（自上游 DownloadScreen.formatSpeed 原样移植） */
fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0) return "0 B/s"
    val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
    var value = bytesPerSec.toDouble()
    var i = 0
    while (value >= 1024 && i < units.size - 1) {
        value /= 1024
        i++
    }
    return String.format("%.1f %s", value, units[i])
}

/** 剩余时间格式化（自上游 DownloadScreen.formatRemain 原样移植） */
fun formatRemain(millis: Long): String {
    if (millis < 0) return "计算中"
    val sec = millis / 1000
    return when {
        sec < 60 -> "${sec}秒"
        sec < 3600 -> "${sec / 60}分${sec % 60}秒"
        else -> "${sec / 3600}时${(sec % 3600) / 60}分"
    }
}

/**
 * 各网盘返回的时间字段格式不统一，统一解析为毫秒时间戳；无法识别返回 null。
 * 自上游 ShareDetailScreen.parseModifyTimeMillis 原样移植：
 * - 夸克：13 位毫秒时间戳；百度：10 位秒级时间戳
 * - 兼容 ISO 8601 / yyyy-MM-dd HH:mm:ss 等文本格式
 */
internal fun parseModifyTimeMillis(raw: String): Long? {
    val s = raw.trim()
    if (s.isEmpty()) return null

    // 1) 纯数字：时间戳（13 位毫秒 / 10 位秒）或紧凑日期串 yyyyMMddHHmmss
    if (s.all(Char::isDigit)) {
        return when (s.length) {
            13 -> s.toLongOrNull()
            10 -> s.toLongOrNull()?.times(1000L)
            14 -> runCatching {
                SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).apply {
                    isLenient = false
                }.parse(s)?.time
            }.getOrNull()
            // 其余长度按数值大小推断秒/毫秒（阈值≈1973 年的毫秒值）
            else -> s.toLongOrNull()?.let { if (it > 100_000_000_000L) it else it * 1000L }
        }
    }

    // 2) 文本时间：先剥离时区后缀，再按「长 → 短」模式尝试解析
    var work = s.replace('T', ' ')
    var tz: TimeZone? = null
    if (work.endsWith("Z", ignoreCase = true)) {
        tz = TimeZone.getTimeZone("UTC")
        work = work.dropLast(1)
    } else {
        val m = Regex("([+-]\\d{2}:?\\d{2})$").find(work)
        if (m != null) {
            tz = TimeZone.getTimeZone("GMT${m.groupValues[1]}")
            work = work.removeRange(m.range)
        }
    }
    // 去掉毫秒小数部分
    val body = work.trim().substringBefore('.')
    val zone: TimeZone? = tz

    val patterns = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy/MM/dd HH:mm:ss",
        "yyyy-MM-dd",
        "yyyy/MM/dd"
    )
    for (p in patterns) {
        val fmt = SimpleDateFormat(p, Locale.getDefault())
        fmt.isLenient = false
        if (zone != null) fmt.timeZone = zone
        runCatching { fmt.parse(body) }.getOrNull()?.let { return it.time }
    }
    return null
}

/**
 * 文件修改时间展示（列表副标题用，尽量紧凑）：
 * 今年内 → "MM-dd HH:mm"；跨年 → "yyyy-MM-dd"；无法解析 → 空串（调用方据此隐藏）。
 */
internal fun formatModifyTime(raw: String): String {
    val millis = parseModifyTimeMillis(raw) ?: return ""
    if (millis <= 0) return ""
    val cal = Calendar.getInstance()
    val currentYear = cal.get(Calendar.YEAR)
    cal.timeInMillis = millis
    val pattern = if (cal.get(Calendar.YEAR) == currentYear) "MM-dd HH:mm" else "yyyy-MM-dd"
    return runCatching {
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
    }.getOrDefault("")
}

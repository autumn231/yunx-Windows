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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * android.util.Log 的桌面端等价实现（原样保留调用点签名）：
 * - 输出到控制台（打包后由 jpackage 记录到安装目录 log）；
 * - 追加写入日志文件（滚动，单文件上限 4MB，保留 2 个历史）。
 */
object Log {

    private val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())

    @Volatile
    private var logFile: File? = null

    /** 由应用启动时注入日志目录；未注入时仅输出控制台 */
    fun initLogDir(dir: File) {
        runCatching {
            dir.mkdirs()
            logFile = File(dir, "yunx.log")
            rollIfNeeded()
        }
    }

    fun d(tag: String, msg: String) = write("D", tag, msg, null)
    fun i(tag: String, msg: String) = write("I", tag, msg, null)
    fun w(tag: String, msg: String) = write("W", tag, msg, null)
    fun e(tag: String, msg: String, tr: Throwable? = null) = write("E", tag, msg, tr)

    private fun write(level: String, tag: String, msg: String, tr: Throwable?) {
        val time = formatter.format(Instant.now())
        val line = "$time $level/$tag: ${com.yunx.app.util.LogRedactor.line(msg)}"
        println(line)
        tr?.let { println("$time $level/$tag: ${it.javaClass.name}: ${it.message}") }
        val file = logFile ?: return
        runCatching {
            synchronized(this) {
                rollIfNeeded()
                file.appendText(
                    buildString {
                        appendLine(line)
                        if (tr != null) {
                            appendLine("$time E/$tag: ${tr.stackTraceToString()}")
                        }
                    }
                )
            }
        }
    }

    private fun rollIfNeeded() {
        val file = logFile ?: return
        if (file.length() > 4 * 1024 * 1024) {
            val old = File(file.parentFile, "yunx.log.1")
            old.delete()
            file.renameTo(old)
        }
    }
}

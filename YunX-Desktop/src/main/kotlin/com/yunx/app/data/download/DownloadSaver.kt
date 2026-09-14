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

package com.yunx.app.data.download

import com.yunx.app.platform.DesktopPaths
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * 下载文件保存（桌面版，替代 Android MediaStore/SAF）：
 * - 直接写入用户选择的下载目录（默认「下载」文件夹）；
 * - 文件名冲突时自动追加「 (n)」序号；
 * - 临时文件先移动到目标目录的 .part 后缀，再原子重命名（避免残留半截文件被误认完成）。
 */
object DownloadSaver {

    /**
     * 把已合并完成的临时文件保存到下载目录。
     * @param fileName 目标文件名（含相对子路径，如「文件夹/文件.mp4」，会做安全净化）
     * @param merged 下载完成后的临时文件
     * @param saveDir 自定义下载目录（null = 用户「下载」文件夹）
     * @return 最终保存的绝对路径；失败返回 null
     */
    fun save(fileName: String, merged: File, saveDir: String?): String? {
        val safe = DownloadPathPolicy.sanitize(fileName, fallbackName = "download_${System.currentTimeMillis()}")
            ?: return null
        val baseDir = File(saveDir?.takeIf { it.isNotBlank() } ?: DesktopPaths.defaultDownloadDir.path)
        if (!baseDir.isDirectory && !baseDir.mkdirs()) return null

        val targetDir = if (safe.relativeDirectory.isBlank()) baseDir
        else File(baseDir, safe.relativeDirectory)
        if (!targetDir.isDirectory && !targetDir.mkdirs()) return null

        val target = uniqueTarget(targetDir.toPath(), safe.fileName)
        val partPath = resolveSibling(target, target.fileName.toString() + ".part")

        return runCatching {
            // 先复制为 .part（同盘原子移动优先），完成后重命名 → 目录里不出现半截「完成」文件
            val moved = moveAtomically(merged.toPath(), partPath)
            if (!moved) return null
            Files.move(partPath, target, StandardCopyOption.ATOMIC_MOVE)
            target.toFile().absolutePath
        }.getOrElse {
            runCatching { Files.deleteIfExists(partPath) }
            null
        }
    }

    /** 删除已保存到本地的文件（删除任务时可选） */
    fun delete(savedPath: String): Boolean = runCatching {
        val f = File(savedPath)
        f.exists() && f.delete()
    }.getOrDefault(false)

    /** 打开文件所在目录并选中（任务卡「打开位置」） */
    fun revealInExplorer(savedPath: String): Boolean = runCatching {
        val file = File(savedPath)
        if (!file.exists()) return false
        if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
            ProcessBuilder("explorer.exe", "/select,", file.absolutePath).start()
        } else {
            ProcessBuilder("xdg-open", file.parentFile?.absolutePath ?: file.absolutePath).start()
        }
        true
    }.getOrDefault(false)

    private fun uniqueTarget(dir: Path, fileName: String): Path {
        var candidate = dir.resolve(fileName)
        if (!Files.exists(candidate)) return candidate
        val dot = fileName.lastIndexOf('.')
        val base = if (dot > 0) fileName.substring(0, dot) else fileName
        val ext = if (dot > 0) fileName.substring(dot) else ""
        var i = 1
        while (true) {
            candidate = dir.resolve("$base ($i)$ext")
            if (!Files.exists(candidate)) return candidate
            i++
        }
    }

    private fun resolveSibling(path: Path, name: String): Path = path.resolveSibling(name)

    /** 同分区原子移动优先；跨分区回退 copy + delete */
    private fun moveAtomically(from: Path, to: Path): Boolean = runCatching {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: java.nio.file.AtomicMoveNotSupportedException) {
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING)
            Files.deleteIfExists(from)
        }
        true
    }.getOrDefault(false)
}

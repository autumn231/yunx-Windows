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

package com.yunx.app.platform

import com.yunx.app.data.download.DownloadProgressEvent
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities

/**
 * 系统托盘（AWT SystemTray，替代 Android 前台服务 + 通知栏进度）：
 * - 关闭主窗口后驻留托盘，下载后台继续；
 * - tooltip 实时显示下载进度（速度/百分比）；
 * - 全部下载完成时弹出系统通知；
 * - 右键菜单：打开主窗口 / 退出；双击图标打开主窗口。
 */
object SystemTrayManager {

    private var trayIcon: TrayIcon? = null

    /** 上一次进度事件的活动任务数（检测「全部完成」时机） */
    private val lastActiveCount = AtomicInteger(0)

    /** 注册托盘图标与菜单；返回是否成功（系统不支持托盘时 false，调用方退化为直接退出） */
    fun setup(onOpen: () -> Unit, onExit: () -> Unit): Boolean {
        if (!SystemTray.isSupported()) return false
        if (trayIcon != null) return true

        val popup = java.awt.PopupMenu()
        val openItem = java.awt.MenuItem("打开 YunX")
        openItem.addActionListener { onOpen() }
        val exitItem = java.awt.MenuItem("退出")
        exitItem.addActionListener { onExit() }
        popup.add(openItem)
        popup.addSeparator()
        popup.add(exitItem)

        return try {
            val icon = TrayIcon(appIconImage(), "YunX 云析", popup)
            icon.isImageAutoSize = true
            // 双击图标 = 打开主窗口
            icon.addActionListener { onOpen() }
            SystemTray.getSystemTray().add(icon)
            trayIcon = icon
            true
        } catch (e: Exception) {
            Log.e("SystemTray", "托盘注册失败", e)
            false
        }
    }

    /** 更新托盘提示（下载进度；回调在 IO 线程，内部切回 EDT） */
    fun updateProgress(event: DownloadProgressEvent) {
        val icon = trayIcon ?: return
        val becameIdle = event.activeTasks <= 0 && lastActiveCount.getAndSet(event.activeTasks) > 0
        SwingUtilities.invokeLater {
            if (event.activeTasks <= 0) {
                icon.toolTip = "YunX 云析"
                if (becameIdle) {
                    runCatching {
                        icon.displayMessage("下载完成", "所有下载任务已完成", TrayIcon.MessageType.INFO)
                    }
                }
            } else {
                val percent = if (event.percent >= 0) " ${event.percent}%" else ""
                val speed = if (event.speedText.isNotBlank()) " · ${event.speedText}" else ""
                icon.toolTip = "YunX · 下载中${percent}${speed}"
            }
        }
    }

    /** 移除托盘图标（退出前调用） */
    fun remove() {
        val icon = trayIcon ?: return
        runCatching { SystemTray.getSystemTray().remove(icon) }
        trayIcon = null
    }

    /** 是否已注册托盘 */
    val isRegistered: Boolean get() = trayIcon != null

    /**
     * 应用图标（程序化绘制，避免携带二进制资源）：
     * 蓝底圆角方块 + 白色「云」字，同时用于托盘与主窗口图标。
     */
    fun appIconImage(size: Int = 64): BufferedImage {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics2D = image.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.color = Color(0xFF1565C0.toInt())
            val radius = size / 5
            g.fillRoundRect(0, 0, size - 1, size - 1, radius, radius)
            g.color = Color.WHITE
            g.font = Font(Font.SANS_SERIF, Font.BOLD, (size * 0.62).toInt())
            val fm = g.fontMetrics
            val text = "云"
            val x = (size - fm.stringWidth(text)) / 2
            val y = (size - fm.height) / 2 + fm.ascent
            g.drawString(text, x, y)
        } finally {
            g.dispose()
        }
        return image
    }
}

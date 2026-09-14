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

package com.yunx.app.ui.login

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.cef.callback.CefCookieVisitor
import org.cef.misc.BoolRef
import org.cef.network.CefCookie
import org.cef.network.CefCookieManager
import java.util.Collections
import java.util.concurrent.CompletableFuture

/**
 * JCEF Cookie 读取器（替代 Android 的 android.webkit.CookieManager.getCookie(url)）。
 *
 * visitUrlCookies(url, includeHttpOnly) 语义与 WebView 的 CookieManager.getCookie 完全一致：
 * 返回「请求该 URL 时浏览器实际会携带的全部 Cookie（含 HttpOnly）」，
 * 格式化为 "k1=v1; k2=v2" 的请求头样式，与上游解析逻辑（__pus=/__puus=/BDUSS= 检测）无缝对接。
 *
 * 使用全局 CefCookieManager（进程内所有浏览器共享；登录窗口关闭后仍可访问，登出清理无需存活浏览器）。
 * visitor 回调发生在 CEF IO 线程，用 CompletableFuture 转为挂起等待，必须在非 UI 协程调用。
 */
object CefCookieReader {

    /** 读取指定 URL 会携带的全部 Cookie，格式化为请求头样式；无 Cookie / 超时返回空串 */
    suspend fun cookieHeader(url: String): String = withContext(Dispatchers.IO) {
        awaitCookies(url).orEmpty().joinToString("; ") { "${it.name}=${it.value}" }
    }

    /** 删除指定 URL 域下的全部 Cookie（登出时清理内嵌浏览器登录态，替代 WebView CookieManager.removeAllCookies） */
    suspend fun clearCookies(url: String): Boolean = withContext(Dispatchers.IO) {
        val target = awaitCookies(url).orEmpty().size
        if (target == 0) return@withContext true
        val future = CompletableFuture<Boolean>()
        val deleted = java.util.concurrent.atomic.AtomicInteger(0)
        val ok = manager()?.visitUrlCookies(url, true, object : CefCookieVisitor {
            override fun visit(cookie: CefCookie, count: Int, total: Int, delete: BoolRef): Boolean {
                delete.set(true)
                if (deleted.incrementAndGet() >= target) future.complete(true)
                return true
            }
        }) ?: false
        if (!ok) return@withContext false
        withTimeoutOrNull(3_000L) { future.get() } ?: false
    }

    /** 访问 URL Cookie 并等待完整列表；超时/管理器不可用返回 null */
    private suspend fun awaitCookies(url: String): List<CefCookie>? = withContext(Dispatchers.IO) {
        val manager = manager() ?: return@withContext null
        val future = CompletableFuture<List<CefCookie>>()
        val collected = Collections.synchronizedList(mutableListOf<CefCookie>())
        val ok = manager.visitUrlCookies(url, true, object : CefCookieVisitor {
            override fun visit(cookie: CefCookie, count: Int, total: Int, delete: BoolRef): Boolean {
                collected.add(cookie)
                // count 从 0 递增，total 为总数：最后一个 Cookie 到达时完成
                if (count == total - 1) future.complete(collected.toList())
                return true
            }
        })
        if (!ok) return@withContext null
        withTimeoutOrNull(5_000L) { future.get() }
    }

    private fun manager(): CefCookieManager? = runCatching {
        CefCookieManager.getGlobalManager()
    }.getOrNull()
}

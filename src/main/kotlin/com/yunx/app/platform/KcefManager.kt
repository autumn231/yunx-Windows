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

import com.yunx.app.data.network.BaiduConstants
import com.yunx.app.platform.Log
import dev.datlag.kcef.KCEF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * KCEF（JCEF 封装）生命周期管理（替代 Android WebView 运行时）：
 * - 首次运行自动下载 JCEF 运行时（约 150MB）到缓存目录，进度通过 StateFlow 暴露给登录页展示；
 * - 后台初始化，主窗口立即可用（登录页在未就绪时显示「浏览器组件准备中」）；
 * - UA 说明：CEF 的 userAgent 为进程级设置（登录窗口一次只开一个平台，无法按浏览器实例区分）。
 *   统一使用 BaiduConstants.UA_WEB（标准桌面 Chrome UA）：
 *   - 百度登录与上游完全一致；
 *   - 夸克登录 URL 自带 ?fr=pc&platform=pc 强制 PC 布局，标准 Chrome UA 即为 PC 网页环境，
 *     Cookie（__pus/__puus）提取不受影响（上游 QuarkPC 后缀仅影响页面渲染细节）。
 *   API 请求的 UA 由 OkHttp 层各平台常量携带（QuarkConstants.API_USER_AGENT 等），与此无关。
 */
object KcefManager {

    /** 初始化状态：NotReady（含下载进度）→ Ready / Failed */
    sealed interface State {
        /** 未就绪：downloadProgress 为 -1 表示尚未开始下载，0..100 为下载百分比，101+ 表示解压/安装中 */
        data class NotReady(val downloadProgress: Float) : State
        data object Ready : State
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.NotReady(-1f))
    val state: StateFlow<State> = _state.asStateFlow()

    @Volatile
    private var initStarted = false

    /** 后台初始化（幂等；在 IO 线程执行，主界面无需等待） */
    suspend fun ensureInitialized() {
        if (initStarted) return
        synchronized(this) {
            if (initStarted) return
            initStarted = true
        }
        withContext(Dispatchers.IO) {
            try {
                KCEF.init(
                    builder = {
                        // 运行时安装到缓存目录（%LOCALAPPDATA%\YunX\cache\kcef-bundle）
                        installDir(File(com.yunx.app.platform.DesktopPaths.cacheDir, "kcef-bundle"))
                        settings {
                            // 窗口化渲染（非 OSR）：登录页原生渲染，兼容性与字体最佳
                            windowlessRenderingEnabled = false
                            // Cookie 落盘：登录页跨启动记住账号（对齐 Android WebView CookieManager 持久化）
                            cachePath = File(
                                com.yunx.app.platform.DesktopPaths.dataDir,
                                "cef-cache"
                            ).absolutePath
                            locale = "zh-CN"
                            userAgent = BaiduConstants.UA_WEB
                        }
                        progress {
                            onDownloading { percent ->
                                _state.value = State.NotReady(percent)
                            }
                            onInitialized {
                                _state.value = State.Ready
                            }
                        }
                    },
                    onError = { throwable ->
                        Log.e("KcefManager", "KCEF init failed", throwable)
                        _state.value = State.Failed(
                            throwable?.message ?: "浏览器组件初始化失败"
                        )
                    },
                    onRestartRequired = {
                        Log.w("KcefManager", "KCEF restart required")
                        _state.value = State.Failed("浏览器组件需重启应用后生效，请重启 YunX")
                    }
                )
                // init 挂起返回即代表初始化完成；若未收到 onInitialized 回调，此处兜底置为就绪
                if (_state.value is State.NotReady) {
                    _state.value = State.Ready
                }
            } catch (e: Exception) {
                Log.e("KcefManager", "KCEF init exception", e)
                _state.value = State.Failed(e.message ?: "浏览器组件初始化失败")
            }
        }
    }

    /** 退出时释放（应用关闭前调用） */
    fun shutdown() {
        runCatching { KCEF.disposeBlocking() }
    }
}

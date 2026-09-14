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

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.yunx.app.data.network.BaiduConstants
import com.yunx.app.data.network.QuarkConstants
import com.yunx.app.data.repository.BaiduAccountRepository
import com.yunx.app.data.repository.QuarkAccountRepository

/**
 * 夸克网盘登录窗口（桌面版）：
 * - 内嵌浏览器加载夸克网盘官网，由用户手动登录；
 * - 自动登录检测：网页内登录完成后自动提取 Cookie 并校验落库（「保存」按钮保留作手动兜底）。
 * 对应上游 Android 版 QuarkLoginScreen。
 */
@Composable
fun QuarkLoginWindow(
    repository: QuarkAccountRepository,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    WebLoginWindow(
        config = WebLoginConfig(
            title = "夸克网盘登录",
            loginUrl = QuarkConstants.LOGIN_URL,
            cookieDomain = QuarkConstants.COOKIE_DOMAIN,
            isValidCookie = { QuarkConstants.isValidCookie(it) },
            validateAndSave = { repository.saveQuarkAccount(it) },
            manualCookieHint = "需包含 __pus= 与 __puus=",
            tutorialSteps = listOf(
                "在下方网页中登录夸克账号",
                "登录完成后将自动检测登录；若未自动登录，点右上角「保存」",
                "或点击「粘贴」图标，手动输入 Cookie（需含 __pus= 与 __puus=）",
                "Cookie 约 30 天有效，失效后需重新登录"
            )
        ),
        onBack = onBack,
        onSaved = onSaved
    )
}

/**
 * 百度网盘登录窗口（桌面版）：
 * - 内嵌浏览器加载百度网盘官网，由用户手动登录；
 * - 自动登录检测：网页内登录完成后自动提取 Cookie（关键字段 BDUSS/STOKEN）并校验落库；
 * - 进入时优先展示风控提示。
 * 对应上游 Android 版 BaiduLoginScreen。
 */
@Composable
fun BaiduLoginWindow(
    repository: BaiduAccountRepository,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    WebLoginWindow(
        config = WebLoginConfig(
            title = "百度网盘登录",
            loginUrl = BaiduConstants.LOGIN_URL,
            cookieDomain = BaiduConstants.COOKIE_DOMAIN,
            isValidCookie = { BaiduConstants.isValidCookie(it) },
            validateAndSave = { repository.saveBaiduAccount(it) },
            manualCookieHint = "需包含 BDUSS=",
            tutorialSteps = listOf(
                "在下方网页中登录百度账号",
                "登录完成后将自动检测登录；若未自动登录，点右上角「保存」",
                "或点击「粘贴」图标，手动输入 Cookie（需含 BDUSS=）",
                "Cookie 长期有效，失效后需重新登录"
            ),
            riskWarning = "百度网盘风控严重，可能导致你的账号被风控"
        ),
        onBack = onBack,
        onSaved = onSaved
    )
}

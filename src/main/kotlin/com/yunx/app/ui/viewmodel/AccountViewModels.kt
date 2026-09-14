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

package com.yunx.app.ui.viewmodel

import com.yunx.app.data.db.BaiduAccountEntity
import com.yunx.app.data.db.QuarkAccountEntity
import com.yunx.app.data.repository.BaiduAccountRepository
import com.yunx.app.data.repository.QuarkAccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 夸克账号 ViewModel：暴露登录态，供设置页（账号管理）与登录窗口共享。
 * 自上游 Android 版移植（去掉 androidx.lifecycle）。
 */
class QuarkAccountViewModel(
    private val repository: QuarkAccountRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val quarkAccount: StateFlow<QuarkAccountEntity?> = repository.observeAccount()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /** 保存夸克 Cookie；返回是否保存成功 */
    suspend fun saveQuarkAccount(cookie: String): Boolean =
        repository.saveQuarkAccount(cookie)

    /** 退出登录：清理内嵌浏览器 Cookie + 清除本地记录 */
    fun logout() {
        scope.launch { repository.logoutQuark() }
    }
}

/**
 * 百度账号 ViewModel：暴露登录态，供设置页（账号管理）与登录窗口共享。
 */
class BaiduAccountViewModel(
    private val repository: BaiduAccountRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val baiduAccount: StateFlow<BaiduAccountEntity?> = repository.observeAccount()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /** 保存百度 Cookie；返回是否保存成功 */
    suspend fun saveBaiduAccount(cookie: String): Boolean =
        repository.saveBaiduAccount(cookie)

    /** 退出登录：清理内嵌浏览器 Cookie + 清除本地记录 */
    fun logout() {
        scope.launch { repository.logoutBaidu() }
    }
}

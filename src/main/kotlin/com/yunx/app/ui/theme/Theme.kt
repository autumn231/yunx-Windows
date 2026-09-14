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

package com.yunx.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.yunx.app.data.prefs.SettingsRepository

/**
 * 主题（桌面版简化移植：上游的主题色设置/动态取色不移植，保留浅色/深色/跟随系统）：
 * 种子色对齐上游默认蓝（Material3 动态色系的默认蓝色调）。
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E4FF),
    onPrimaryContainer = Color(0xFF001C38),
    secondary = Color(0xFF565E71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDAE2F9),
    onSecondaryContainer = Color(0xFF131C2B),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1A1C1F),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1A1C1F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA4C9FF),
    onPrimary = Color(0xFF00315D),
    primaryContainer = Color(0xFF2C4A75),
    onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = Color(0xFFBEC6DC),
    onSecondary = Color(0xFF283041),
    secondaryContainer = Color(0xFF3E4759),
    onSecondaryContainer = Color(0xFFDAE2F9),
    surface = Color(0xFF1A1C1F),
    onSurface = Color(0xFFE2E2E5),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC4C6CF),
    background = Color(0xFF1A1C1F),
    onBackground = Color(0xFFE2E2E5),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

/**
 * 应用主题入口。
 * @param darkModeSetting 0=跟随系统，1=浅色，2=深色（SettingsRepository.darkMode）
 */
@Composable
fun YunXTheme(
    darkModeSetting: Int = 0,
    content: @Composable () -> Unit
) {
    val darkTheme = when (darkModeSetting) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}

/** 便捷重载：直接从设置仓库读取深色模式 */
@Composable
fun YunXTheme(
    settings: SettingsRepository,
    content: @Composable () -> Unit
) {
    YunXTheme(darkModeSetting = settings.darkMode, content = content)
}

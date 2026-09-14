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

package com.yunx.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import java.awt.Window

/**
 * 当前主窗口的 AWT 引用（焦点监听、JFileChooser 父窗口等用途）。
 * Compose Desktop 1.8 起 androidx.compose.ui.window.LocalWindow 为 internal，故自建同义 CompositionLocal，
 * 由 Main.kt 在 Window 内容中 provide。
 */
val LocalAppWindow = staticCompositionLocalOf<Window?> { null }

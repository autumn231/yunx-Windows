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

package com.yunx.app.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.yunx.app.data.security.CredentialCipher
import com.yunx.app.data.security.DesktopCredentialCipher
import java.io.File

/**
 * Room 数据库（桌面端，KMP BundledSQLite 驱动）。
 * 桌面版为全新安装（无 Android 迁移历史），version 从 1 起；
 * 实体/DAO 与上游完全一致（仅保留夸克/百度/下载任务三表）。
 */
@Database(
    entities = [QuarkAccountEntity::class, BaiduAccountEntity::class, DownloadTaskEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun rawQuarkAccountDao(): QuarkAccountDao

    abstract fun rawBaiduAccountDao(): BaiduAccountDao

    abstract fun downloadTaskDao(): DownloadTaskDao

    private lateinit var credentialCipher: CredentialCipher

    fun quarkAccountDao(): QuarkAccountDao = SecureAccountDaos.quark(rawQuarkAccountDao(), credentialCipher)
    fun baiduAccountDao(): BaiduAccountDao = SecureAccountDaos.baidu(rawBaiduAccountDao(), credentialCipher)

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** @param dbFile 数据库文件路径（应用数据目录/yunx.db） */
        fun get(dbFile: File): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder<AppDatabase>(
                    name = dbFile.absolutePath
                )
                    .setDriver(BundledSQLiteDriver())
                    .build()
                    .also { database ->
                        database.credentialCipher = DesktopCredentialCipher()
                        instance = database
                    }
            }
    }
}

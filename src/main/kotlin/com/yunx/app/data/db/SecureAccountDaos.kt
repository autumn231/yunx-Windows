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

import com.yunx.app.data.security.CredentialCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * DAO decorators: plaintext is exposed only in memory; every database write is encrypted.
 * （桌面版仅保留夸克/百度，逻辑与上游 SecureAccountDaos 一致）
 */
internal object SecureAccountDaos {
    fun quark(raw: QuarkAccountDao, cipher: CredentialCipher): QuarkAccountDao = object : QuarkAccountDao {
        override fun observeAccount(): Flow<QuarkAccountEntity?> = raw.observeAccount().map { value ->
            value?.let { decryptQuark(raw, cipher, it) }
        }
        override suspend fun upsert(account: QuarkAccountEntity) = withContext(Dispatchers.IO) {
            raw.upsert(encryptQuark(cipher, account))
        }
        override suspend fun getAccount(): QuarkAccountEntity? = raw.getAccount()?.let { decryptQuark(raw, cipher, it) }
        override suspend fun clear() = raw.clear()
    }

    fun baidu(raw: BaiduAccountDao, cipher: CredentialCipher): BaiduAccountDao = object : BaiduAccountDao {
        override fun observeAccount(): Flow<BaiduAccountEntity?> = raw.observeAccount().map { value ->
            value?.let { decryptBaidu(raw, cipher, it) }
        }
        override suspend fun upsert(account: BaiduAccountEntity) = withContext(Dispatchers.IO) {
            raw.upsert(encryptBaidu(cipher, account))
        }
        override suspend fun getAccount(): BaiduAccountEntity? = raw.getAccount()?.let { decryptBaidu(raw, cipher, it) }
        override suspend fun clear() = raw.clear()
    }

    private suspend fun decryptQuark(raw: QuarkAccountDao, cipher: CredentialCipher, stored: QuarkAccountEntity): QuarkAccountEntity? =
        withContext(Dispatchers.IO) {
            decryptOrClear(raw::clear) {
                val plain = stored.copy(cookie = cipher.decrypt(stored.cookie, "quark.cookie"))
                if (!cipher.isEncrypted(stored.cookie)) raw.upsert(encryptQuark(cipher, plain))
                plain
            }
        }

    private suspend fun decryptBaidu(raw: BaiduAccountDao, cipher: CredentialCipher, stored: BaiduAccountEntity): BaiduAccountEntity? =
        withContext(Dispatchers.IO) {
            decryptOrClear(raw::clear) {
                val plain = stored.copy(cookie = cipher.decrypt(stored.cookie, "baidu.cookie"))
                if (!cipher.isEncrypted(stored.cookie)) raw.upsert(encryptBaidu(cipher, plain))
                plain
            }
        }

    private fun encryptQuark(cipher: CredentialCipher, value: QuarkAccountEntity) =
        value.copy(cookie = cipher.encrypt(value.cookie, "quark.cookie"))

    private fun encryptBaidu(cipher: CredentialCipher, value: BaiduAccountEntity) =
        value.copy(cookie = cipher.encrypt(value.cookie, "baidu.cookie"))

    private suspend fun <T> decryptOrClear(clear: suspend () -> Unit, block: suspend () -> T): T? =
        try {
            block()
        } catch (error: Exception) {
            clear()
            null
        }
}

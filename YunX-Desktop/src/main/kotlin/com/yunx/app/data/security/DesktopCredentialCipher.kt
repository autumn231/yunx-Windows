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

package com.yunx.app.data.security

import com.yunx.app.platform.DesktopPaths
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-GCM 加密（替代 Android Keystore 版本）：
 * - 密文格式与上游完全一致（yunx:v1:iv:ct，AAD 绑定 purpose），便于上游 diff；
 * - 密钥为 256-bit AES，随机生成后持久化在应用数据目录（仅本机用户可读）。
 */
internal class DesktopCredentialCipher : CredentialCipher {

    @Volatile
    private var cachedKey: SecretKey? = null

    override fun encrypt(plaintext: String, purpose: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(purpose.toByteArray(Charsets.UTF_8))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return listOf(
            PREFIX,
            Base64.getEncoder().encodeToString(cipher.iv),
            Base64.getEncoder().encodeToString(ciphertext)
        ).joinToString(":")
    }

    override fun decrypt(stored: String, purpose: String): String {
        if (!isEncrypted(stored)) return stored
        val parts = stored.split(':', limit = 4)
        require(parts.size == 4 && parts[0] == "yunx" && parts[1] == "v1") {
            "Unsupported encrypted credential format"
        }
        val iv = Base64.getDecoder().decode(parts[2])
        val ciphertext = Base64.getDecoder().decode(parts[3])
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        cipher.updateAAD(purpose.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    override fun isEncrypted(stored: String): Boolean = stored.startsWith("$PREFIX:")

    private fun key(): SecretKey {
        cachedKey?.let { return it }
        synchronized(this) {
            cachedKey?.let { return it }
            val key = loadOrCreateKey()
            cachedKey = key
            return key
        }
    }

    private fun loadOrCreateKey(): SecretKey {
        val file = File(DesktopPaths.dataDir, "credential.key")
        if (file.isFile) {
            val bytes = Base64.getDecoder().decode(file.readText().trim())
            if (bytes.size == 32) {
                return javax.crypto.spec.SecretKeySpec(bytes, "AES")
            }
        }
        val generator = KeyGenerator.getInstance("AES")
        generator.init(256, SecureRandom())
        val key = generator.generateKey()
        runCatching {
            file.writeText(Base64.getEncoder().encodeToString(key.encoded))
            // 尽力限制为仅当前用户可读（Windows 上 NTFS 继承用户目录 ACL，Linux 上收紧权限）
            file.setReadable(false, false)
            file.setReadable(true, true)
            file.setWritable(false, false)
            file.setWritable(true, true)
        }
        return key
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREFIX = "yunx:v1"
    }
}

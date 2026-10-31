package com.recaitelab.app.data.local.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * API Key 的 Android Keystore 加解密。
 *
 * - 密钥保存在 AndroidKeyStore，别名 [KEY_ALIAS]，不导出、不落盘；
 * - AES-256-GCM，每次加密随机 IV；
 * - 密文格式为 `Base64(iv):Base64(ciphertext)`，与 PRD 约定一致。
 */
class ApiKeyCipher {

    /** 加密明文；调用方负责只在内存中保留明文。 */
    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val payload = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        return "$iv$SEPARATOR$payload"
    }

    /** 解密失败（密钥失效/数据损坏）返回 null，由调用方按“无 Key”处理。 */
    fun decrypt(encoded: String): String? = runCatching {
        val parts = encoded.split(SEPARATOR, limit = 2)
        require(parts.size == 2)
        val iv = Base64.decode(parts[0], Base64.DEFAULT)
        val ciphertext = Base64.decode(parts[1], Base64.DEFAULT)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }.getOrNull()

    private fun getOrCreateKey(): SecretKey = synchronized(LOCK) {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        existing?.secretKey ?: generateKey()
    }

    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEYSTORE_PROVIDER: String = "AndroidKeyStore"
        const val KEY_ALIAS: String = "recaitelab_api_key_v1"
        const val KEY_SIZE_BITS: Int = 256
        const val TRANSFORMATION: String = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS: Int = 128
        const val SEPARATOR: String = ":"
        val LOCK: Any = Any()
    }
}

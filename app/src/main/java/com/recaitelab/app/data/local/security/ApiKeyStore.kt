package com.recaitelab.app.data.local.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 加密后的 API Key 存储。
 *
 * 密文写入独立的 DataStore 文件 `secure_keys.preferences_pb`，键为 `api_key_<providerId>`；
 * 文件里只有 Keystore AES-GCM 密文，绝不出现明文。解密结果只存在于内存中的 [apiKeys]。
 */
class ApiKeyStore internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val cipher: ApiKeyCipher = ApiKeyCipher(),
) {

    constructor(context: Context) : this(context.secureKeysDataStore)

    /** providerId（UUID 字符串）→ 解密后的 Key。解密失败或损坏的条目会被忽略。 */
    val apiKeys: Flow<Map<String, String>> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            preferences.asMap().mapNotNull { (key, value) ->
                if (!key.name.startsWith(KEY_PREFIX)) return@mapNotNull null
                val encrypted = value as? String ?: return@mapNotNull null
                val plaintext = cipher.decrypt(encrypted) ?: return@mapNotNull null
                key.name.removePrefix(KEY_PREFIX) to plaintext
            }.toMap()
        }
        .flowOn(Dispatchers.IO)

    suspend fun put(providerId: UUID, apiKey: String) {
        withContext(Dispatchers.IO) {
            val key = keyOf(providerId)
            if (apiKey.isBlank()) {
                dataStore.edit { preferences -> preferences.remove(key) }
            } else {
                val encrypted = cipher.encrypt(apiKey)
                dataStore.edit { preferences -> preferences[key] = encrypted }
            }
        }
    }

    suspend fun remove(providerId: UUID) {
        withContext(Dispatchers.IO) {
            val key = keyOf(providerId)
            dataStore.edit { preferences -> preferences.remove(key) }
        }
    }

    private fun keyOf(providerId: UUID): Preferences.Key<String> =
        stringPreferencesKey(KEY_PREFIX + providerId)

    private companion object {
        const val KEY_PREFIX: String = "api_key_"
    }
}

private val Context.secureKeysDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_keys")

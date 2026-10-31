package com.recaitelab.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * 轻量偏好（DataStore Preferences）。
 *
 * 当前保存：
 * - [activeProviderId]：当前激活 Provider 的 UUID 字符串（键 `last_provider_id`）；
 * - [selectedModel]：最近一次选中的模型（键 `last_model_id`），作为跨会话的偏好镜像；
 * - [lastConversationId]：上次打开的会话 id（键 `last_conversation_id`），用于杀进程后恢复；
 * - [hasSeededDefaultProvider]：debug 默认 Provider 是否已播种，避免用户删除后又被重建。
 *
 * Provider 的完整配置与每个 Provider 的 selectedModel 仍在 Room；Key 不在本文件。
 */
class UserPreferencesRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) : ConversationPreferences {

    constructor(context: Context) : this(context.settingsDataStore)

    val activeProviderId: Flow<String?> = dataStore.safeData()
        .map { preferences -> preferences[KEY_ACTIVE_PROVIDER_ID] }

    val selectedModel: Flow<String?> = dataStore.safeData()
        .map { preferences -> preferences[KEY_SELECTED_MODEL] }

    override val lastConversationId: Flow<Long?> = dataStore.safeData()
        .map { preferences -> preferences[KEY_LAST_CONVERSATION_ID] }

    val hasSeededDefaultProvider: Flow<Boolean> = dataStore.safeData()
        .map { preferences -> preferences[KEY_DEFAULT_PROVIDER_SEEDED] ?: false }

    suspend fun setActiveProviderId(providerId: String?) {
        dataStore.edit { preferences ->
            if (providerId.isNullOrBlank()) {
                preferences.remove(KEY_ACTIVE_PROVIDER_ID)
            } else {
                preferences[KEY_ACTIVE_PROVIDER_ID] = providerId
            }
        }
    }

    suspend fun setSelectedModel(model: String?) {
        dataStore.edit { preferences ->
            if (model.isNullOrBlank()) {
                preferences.remove(KEY_SELECTED_MODEL)
            } else {
                preferences[KEY_SELECTED_MODEL] = model
            }
        }
    }

    override suspend fun setLastConversationId(conversationId: Long?) {
        dataStore.edit { preferences ->
            if (conversationId == null || conversationId <= 0L) {
                preferences.remove(KEY_LAST_CONVERSATION_ID)
            } else {
                preferences[KEY_LAST_CONVERSATION_ID] = conversationId
            }
        }
    }

    suspend fun markDefaultProviderSeeded() {
        dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_PROVIDER_SEEDED] = true
        }
    }

    /** 读取失败（例如磁盘 IO）时回退为空偏好，不让 UI 因 DataStore 异常崩溃。 */
    private fun DataStore<Preferences>.safeData(): Flow<Preferences> = data.catch { throwable ->
        if (throwable is IOException) {
            emit(emptyPreferences())
        } else {
            throw throwable
        }
    }

    private companion object {
        val KEY_ACTIVE_PROVIDER_ID: Preferences.Key<String> = stringPreferencesKey("last_provider_id")
        val KEY_SELECTED_MODEL: Preferences.Key<String> = stringPreferencesKey("last_model_id")
        val KEY_LAST_CONVERSATION_ID: Preferences.Key<Long> =
            longPreferencesKey("last_conversation_id")
        val KEY_DEFAULT_PROVIDER_SEEDED: Preferences.Key<Boolean> =
            booleanPreferencesKey("default_provider_seeded")
    }
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

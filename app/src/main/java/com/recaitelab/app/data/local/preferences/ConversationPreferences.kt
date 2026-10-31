package com.recaitelab.app.data.local.preferences

import kotlinx.coroutines.flow.Flow

/**
 * 会话仓库所需的最小偏好接口。
 *
 * 生产实现是 [UserPreferencesRepository]（DataStore）；单测可替换为内存实现，
 * 避免 JVM 单测加载 Android DataStore。
 */
interface ConversationPreferences {

    val lastConversationId: Flow<Long?>

    suspend fun setLastConversationId(conversationId: Long?)
}

package com.recaitelab.app.di

import android.content.Context
import com.recaitelab.app.BuildConfig
import com.recaitelab.app.data.local.RecAIteLabDatabase
import com.recaitelab.app.data.local.preferences.UserPreferencesRepository
import com.recaitelab.app.data.local.security.ApiKeyStore
import com.recaitelab.app.data.repository.ChatRepository
import com.recaitelab.app.data.repository.ConversationRepository
import com.recaitelab.app.data.repository.KnowledgeOrganizer
import com.recaitelab.app.data.repository.KnowledgeRepository
import com.recaitelab.app.data.repository.ProviderRepository
import com.recaitelab.app.domain.model.ProviderConfig
import com.recaitelab.app.domain.model.ProviderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 手动依赖容器（Hilt 未引入）。
 *
 * 初始化由 [com.recaitelab.app.RecAIteLabApplication] 在进程启动时调用，提供：
 * - Room 数据库与各 DAO；
 * - DataStore 偏好与 Keystore 加密 Key 存储；
 * - 应用级协程作用域、[ProviderRepository] 与 [ConversationRepository] 单例。
 *
 * 首次启动时用 `local.properties` 注入的 debug 配置播种一个默认 DeepSeek Provider；
 * 播种标记写入 DataStore，用户之后删除该 Provider 不会在下次启动时被重建。
 *
 * [ChatRepository] 只发起请求，会话与消息的持久化由单例 [ConversationRepository] 负责，
 * 因此 ChatRepository 仍随 ViewModel 创建，而会话状态可跨进程恢复。
 */
object AppContainer {

    private const val DEFAULT_PROVIDER_NAME = "DeepSeek"
    private const val FALLBACK_MODEL = "deepseek-chat"

    @Volatile
    private var initialized: Boolean = false

    private lateinit var database: RecAIteLabDatabase
    private lateinit var preferences: UserPreferencesRepository
    private lateinit var apiKeyStore: ApiKeyStore
    private lateinit var applicationScope: CoroutineScope

    /** 进程内共享的 Provider 配置仓库；所有 UI 都通过它管理 Provider。 */
    lateinit var providerRepository: ProviderRepository
        private set

    /** 进程内共享的会话与消息仓库；持有当前会话 id，跨 ViewModel 复用。 */
    lateinit var conversationRepository: ConversationRepository
        private set

    /** 进程内共享的知识库数据仓库；本阶段仅数据层，不接 UI。 */
    lateinit var knowledgeRepository: KnowledgeRepository
        private set

    /** 知识整理服务；后续由历史记录页调用，本阶段只装配。 */
    lateinit var knowledgeOrganizer: KnowledgeOrganizer
        private set

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val appContext = context.applicationContext
            database = RecAIteLabDatabase.getInstance(appContext)
            preferences = UserPreferencesRepository(appContext)
            apiKeyStore = ApiKeyStore(appContext)
            applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            providerRepository = ProviderRepository(
                providerDao = database.providerDao(),
                apiKeyStore = apiKeyStore,
                preferences = preferences,
                scope = applicationScope,
            )
            conversationRepository = ConversationRepository(
                conversationDao = database.conversationDao(),
                messageDao = database.messageDao(),
                preferences = preferences,
            )
            knowledgeRepository = KnowledgeRepository(database.knowledgeEntryDao())
            knowledgeOrganizer = KnowledgeOrganizer(
                conversationRepository = conversationRepository,
                providerRepository = providerRepository,
                knowledgeRepository = knowledgeRepository,
            )
            initialized = true
            seedDefaultProviderIfNeeded()
        }
    }

    fun createChatRepository(): ChatRepository =
        ChatRepository(providerRepository, conversationRepository)

    private fun seedDefaultProviderIfNeeded() {
        applicationScope.launch {
            try {
                if (preferences.hasSeededDefaultProvider.first()) return@launch
                if (database.providerDao().count() > 0) {
                    preferences.markDefaultProviderSeeded()
                    return@launch
                }
                // release 包不注入 Key，跳过播种，等用户在设置页手动添加。
                if (BuildConfig.DEEPSEEK_API_KEY.isBlank()) return@launch
                providerRepository.addProvider(createDefaultProvider())
                preferences.markDefaultProviderSeeded()
            } catch (_: Exception) {
                // 播种失败（如 Keystore 不可用）不阻塞启动；用户仍可在设置页手动添加 Provider。
            }
        }
    }

    private fun createDefaultProvider(): ProviderConfig {
        val model = BuildConfig.DEEPSEEK_MODEL.ifBlank { FALLBACK_MODEL }
        return ProviderConfig(
            name = DEFAULT_PROVIDER_NAME,
            baseUrl = BuildConfig.DEEPSEEK_BASE_URL,
            apiKey = BuildConfig.DEEPSEEK_API_KEY,
            type = ProviderType.OPENAI_COMPATIBLE,
            models = listOf(model),
            selectedModel = model,
        )
    }
}

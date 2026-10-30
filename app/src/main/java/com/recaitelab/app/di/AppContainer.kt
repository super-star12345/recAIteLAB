package com.recaitelab.app.di

import com.recaitelab.app.BuildConfig
import com.recaitelab.app.data.repository.ChatRepository
import com.recaitelab.app.data.repository.ProviderRepository
import com.recaitelab.app.domain.model.ProviderConfig
import com.recaitelab.app.domain.model.ProviderType

/**
 * 手动依赖容器（Hilt 未引入）。
 *
 * Provider 管理：
 * - 启动时用 `local.properties` 注入的 debug 配置播种一个默认 DeepSeek Provider；
 * - 之后所有 Provider 的增删改查、切换、模型选择都只走 [providerRepository]，
 *   不再直接读取 `BuildConfig.DEEPSEEK_*`；
 * - release 的 Key 为空，正式配置后续由 DataStore + 设置页写入。
 *
 * [ChatRepository] 持有单次会话的内存上下文，随 ViewModel 创建而创建，因此用工厂而不是单例。
 */
object AppContainer {

    private const val DEFAULT_PROVIDER_NAME = "DeepSeek"
    private const val FALLBACK_MODEL = "deepseek-chat"

    /** 进程内共享的 Provider 配置仓库；后续 UI 也通过它管理 Provider。 */
    val providerRepository: ProviderRepository by lazy {
        ProviderRepository(initialProviders = listOf(createDefaultProvider()))
    }

    fun createChatRepository(): ChatRepository = ChatRepository(providerRepository)

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

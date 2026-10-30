package com.recaitelab.app.data.remote

import com.recaitelab.app.domain.model.ProviderConfig
import com.recaitelab.app.domain.model.ProviderType

/**
 * 按 [ProviderConfig.type] 创建对应的 [ChatProvider]。
 *
 * `when` 是穷尽的：新增 [ProviderType] 而忘记补实现时会直接编译失败。
 */
fun createChatProvider(config: ProviderConfig): ChatProvider = when (config.type) {
    ProviderType.OPENAI_COMPATIBLE -> OpenAiCompatibleProvider(
        baseUrl = config.baseUrl,
        apiKey = config.apiKey,
    )
}

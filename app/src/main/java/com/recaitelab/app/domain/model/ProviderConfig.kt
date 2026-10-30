package com.recaitelab.app.domain.model

import java.util.UUID

/**
 * Provider 协议类型。
 *
 * MVP 只实现 OpenAI 兼容协议；Gemini / Anthropic 适配时在这里追加。
 */
enum class ProviderType {
    OPENAI_COMPATIBLE,
}

/**
 * 一个第三方 Provider 的配置。
 *
 * @property id 稳定标识；内存阶段用随机 UUID，接入 Room 后作为主键
 * @property name 显示名，例如 DeepSeek
 * @property baseUrl 接口根地址（不含 `/chat/completions`）
 * @property apiKey 用户自带的 Key，禁止打印或提交
 * @property type 协议类型，决定使用哪个 ChatProvider 实现
 * @property models 已知模型 id 列表
 * @property selectedModel 当前选中的模型；尚未选择时为 null
 */
data class ProviderConfig(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val type: ProviderType = ProviderType.OPENAI_COMPATIBLE,
    val models: List<String> = emptyList(),
    val selectedModel: String? = null,
)

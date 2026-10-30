package com.recaitelab.app.domain.model

import kotlinx.serialization.Serializable

/**
 * OpenAI 兼容 Chat Completions 接口中的一条消息。
 *
 * 字段名与请求/响应 JSON 保持一致，不做重命名。
 *
 * @property role 消息角色，取值为 `user` / `assistant` / `system`
 * @property content 消息正文，流式响应中为整段文本
 */
@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)

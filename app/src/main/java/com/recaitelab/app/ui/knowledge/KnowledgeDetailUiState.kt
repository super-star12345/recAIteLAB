package com.recaitelab.app.ui.knowledge

import com.recaitelab.app.domain.model.KnowledgeEntry

/**
 * 知识详情页状态。
 *
 * @property entry 条目内容；加载完成且存在时非空
 * @property isLoading 首次读取期间为 true
 * @property isNotFound 条目不存在（可能已被删除）
 * @property errorMessage 读取失败提示
 */
data class KnowledgeDetailUiState(
    val entry: KnowledgeEntry? = null,
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
    val errorMessage: String? = null,
)

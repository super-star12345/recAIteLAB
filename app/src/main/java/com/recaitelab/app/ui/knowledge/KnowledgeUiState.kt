package com.recaitelab.app.ui.knowledge

/**
 * 知识库列表页状态。
 *
 * @property entries 当前查询结果（全部或搜索结果）
 * @property query 搜索框内容；立即回显，仓库查询有 300ms 防抖
 * @property isLoading 首次读取 Room 期间为 true
 * @property pendingDelete 待确认删除的条目
 * @property errorMessage 加载/删除失败提示
 */
data class KnowledgeUiState(
    val entries: List<KnowledgeEntryUiModel> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val pendingDelete: KnowledgeEntryUiModel? = null,
    val errorMessage: String? = null,
)

/** 列表行数据；不把正文带进列表 UI。 */
data class KnowledgeEntryUiModel(
    val id: Long,
    val title: String,
    val summary: String,
    val tags: List<String>,
    val updatedAt: Long,
)

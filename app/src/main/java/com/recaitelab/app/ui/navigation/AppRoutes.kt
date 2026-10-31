package com.recaitelab.app.ui.navigation

import kotlinx.serialization.Serializable

/**
 * 一级 Tab 的导航路由。
 *
 * 使用 Navigation Compose 类型安全 API，路由对象由 kotlinx.serialization 生成序列化器，
 * 避免手写字符串 route 与参数拼接。
 */
@Serializable
data object ChatRoute

@Serializable
data object HistoryRoute

@Serializable
data object KnowledgeRoute

@Serializable
data object SettingsRoute

/** 知识详情二级页；[entryId] 为知识条目主键。 */
@Serializable
data class KnowledgeDetailRoute(val entryId: Long)

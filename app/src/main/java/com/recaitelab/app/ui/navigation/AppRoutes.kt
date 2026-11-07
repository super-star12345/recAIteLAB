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

/** 设置二级页：外观模式、配色方案、动态取色。 */
@Serializable
data object SettingsAppearanceRoute

/** 设置二级页：模型与 Provider 管理。 */
@Serializable
data object SettingsModelsRoute

/** 设置二级页：Prism 主题静态预览（T1，不接业务数据）。 */
@Serializable
data object SettingsPrismRoute

/** 知识详情二级页；[entryId] 为知识条目主键。 */
@Serializable
data class KnowledgeDetailRoute(val entryId: Long)

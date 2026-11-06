package com.recaitelab.app.ui.settings

import com.recaitelab.app.domain.model.AppColorTheme
import com.recaitelab.app.domain.model.AppearanceMode
import java.util.UUID

/**
 * 设置页状态。
 *
 * @property providers 列表展示模型，刻意不包含 apiKey
 * @property activeProviderId 当前激活 Provider 的 id
 * @property form 非空表示正在添加/编辑，null 表示列表模式
 * @property pendingDelete 待确认删除的 Provider
 * @property appearanceMode 外观模式：跟随系统 / 浅色 / 深色
 * @property colorTheme 配色方案：默认 / 护眼绿 / 商务蓝
 * @property dynamicColorEnabled 是否启用系统动态取色（仅 Android 12+）
 * @property isDynamicColorSupported 当前系统版本是否支持动态取色
 */
data class SettingsUiState(
    val providers: List<ProviderUiModel> = emptyList(),
    val activeProviderId: UUID? = null,
    val form: ProviderFormState? = null,
    val pendingDelete: ProviderUiModel? = null,
    val errorMessage: String? = null,
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val colorTheme: AppColorTheme = AppColorTheme.DEFAULT,
    val dynamicColorEnabled: Boolean = false,
    val isDynamicColorSupported: Boolean = false,
)

/** Provider 列表项；apiKey 不进入 UI 层。 */
data class ProviderUiModel(
    val id: UUID,
    val name: String,
    val baseUrl: String,
    val selectedModel: String?,
)

/**
 * 添加/编辑表单状态。
 *
 * 编辑时 [apiKey] 恒为空：留空表示保留原 Key，输入则覆盖。
 *
 * @property models 当前 Provider 的模型列表（拉取或手动维护）
 * @property selectedModel 默认选中模型，保存后写入 ProviderConfig
 * @property isFetchingModels 「拉取模型列表」进行中
 * @property modelFetchError 拉取失败提示；失败后仍可手动增删模型
 */
data class ProviderFormState(
    val providerId: UUID? = null,
    val name: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    val isNew: Boolean = true,
    val isSaving: Boolean = false,
    val models: List<String> = emptyList(),
    val selectedModel: String? = null,
    val newModelName: String = "",
    val newModelError: String? = null,
    val isFetchingModels: Boolean = false,
    val modelFetchError: String? = null,
    val nameError: String? = null,
    val baseUrlError: String? = null,
    val apiKeyError: String? = null,
)

/** 设置页事件，由 Composable 上抛给 ViewModel。 */
sealed interface SettingsEvent {
    data object AddProvider : SettingsEvent
    data class EditProvider(val provider: ProviderUiModel) : SettingsEvent
    data class DeleteProvider(val provider: ProviderUiModel) : SettingsEvent
    data class SetActive(val provider: ProviderUiModel) : SettingsEvent
    data class NameChange(val value: String) : SettingsEvent
    data class BaseUrlChange(val value: String) : SettingsEvent
    data class ApiKeyChange(val value: String) : SettingsEvent
    data object FetchModels : SettingsEvent
    data class NewModelNameChange(val value: String) : SettingsEvent
    data object AddModel : SettingsEvent
    data class RemoveModel(val model: String) : SettingsEvent
    data class SelectModel(val model: String) : SettingsEvent
    data object Save : SettingsEvent
    data object CancelForm : SettingsEvent
    data object ConfirmDelete : SettingsEvent
    data object CancelDelete : SettingsEvent
    data class AppearanceModeChange(val mode: AppearanceMode) : SettingsEvent
    data class ColorThemeChange(val theme: AppColorTheme) : SettingsEvent
    data class DynamicColorChange(val enabled: Boolean) : SettingsEvent
    data object ErrorShown : SettingsEvent
}

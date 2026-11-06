package com.recaitelab.app.ui.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recaitelab.app.data.local.preferences.UserPreferencesRepository
import com.recaitelab.app.data.remote.ApiException
import com.recaitelab.app.data.remote.ChatProvider
import com.recaitelab.app.data.remote.createChatProvider
import com.recaitelab.app.data.repository.ProviderRepository
import com.recaitelab.app.domain.model.AppColorTheme
import com.recaitelab.app.domain.model.AppearanceMode
import com.recaitelab.app.domain.model.ProviderConfig
import com.recaitelab.app.domain.model.ProviderType
import java.io.IOException
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 设置页 ViewModel：把 [ProviderRepository] 的 StateFlow 映射成 UI 状态。
 *
 * 安全约定：
 * - 列表只暴露 [ProviderUiModel]，不含 apiKey；
 * - 编辑时表单的 apiKey 为空，留空即保留原 Key；
 * - 拉取模型使用表单当前的 baseUrl / Key（编辑且留空时用已存 Key）；
 * - 任何异常信息都不包含 Key，也不写日志。
 */
class SettingsViewModel(
    private val providerRepository: ProviderRepository,
    private val preferences: UserPreferencesRepository,
    private val providerFactory: (ProviderConfig) -> ChatProvider = ::createChatProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            providers = providerRepository.providers.value.map { it.toUiModel() },
            activeProviderId = providerRepository.activeProvider.value?.id,
            isDynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        ),
    )

    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /** 每次进入/离开表单递增，用于丢弃过期的拉取结果。 */
    private var formGeneration = 0L

    init {
        viewModelScope.launch {
            combine(
                providerRepository.providers,
                providerRepository.activeProvider,
            ) { providers, active ->
                providers.map { it.toUiModel() } to active?.id
            }.collect { (providers, activeProviderId) ->
                _uiState.update {
                    it.copy(providers = providers, activeProviderId = activeProviderId)
                }
            }
        }

        viewModelScope.launch {
            combine(
                preferences.appearanceMode,
                preferences.colorTheme,
                preferences.dynamicColorEnabled,
            ) { mode, theme, dynamicColor ->
                Triple(mode, theme, dynamicColor)
            }.collect { (mode, theme, dynamicColor) ->
                _uiState.update {
                    it.copy(
                        appearanceMode = mode,
                        colorTheme = theme,
                        dynamicColorEnabled = dynamicColor,
                    )
                }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.AddProvider -> startCreate()
            is SettingsEvent.EditProvider -> startEdit(event.provider)
            is SettingsEvent.DeleteProvider -> _uiState.update {
                it.copy(pendingDelete = event.provider, errorMessage = null)
            }
            is SettingsEvent.SetActive -> setActive(event.provider)
            is SettingsEvent.NameChange -> updateForm {
                it.copy(name = event.value, nameError = null)
            }
            is SettingsEvent.BaseUrlChange -> updateForm {
                it.copy(baseUrl = event.value, baseUrlError = null)
            }
            is SettingsEvent.ApiKeyChange -> updateForm {
                it.copy(apiKey = event.value, apiKeyError = null)
            }
            SettingsEvent.FetchModels -> fetchModels()
            is SettingsEvent.NewModelNameChange -> updateForm {
                it.copy(newModelName = event.value, newModelError = null)
            }
            SettingsEvent.AddModel -> addModel()
            is SettingsEvent.RemoveModel -> removeModel(event.model)
            is SettingsEvent.SelectModel -> selectModel(event.model)
            SettingsEvent.Save -> save()
            SettingsEvent.CancelForm -> cancelForm()
            SettingsEvent.ConfirmDelete -> confirmDelete()
            SettingsEvent.CancelDelete -> _uiState.update { it.copy(pendingDelete = null) }
            is SettingsEvent.AppearanceModeChange -> setAppearanceMode(event.mode)
            is SettingsEvent.ColorThemeChange -> setColorTheme(event.theme)
            is SettingsEvent.DynamicColorChange -> setDynamicColorEnabled(event.enabled)
            SettingsEvent.ErrorShown -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun startCreate() {
        formGeneration++
        _uiState.update {
            it.copy(form = ProviderFormState(isNew = true), errorMessage = null)
        }
    }

    private fun startEdit(provider: ProviderUiModel) {
        val config = providerRepository.providerById(provider.id) ?: return
        formGeneration++
        _uiState.update {
            it.copy(
                form = ProviderFormState(
                    providerId = config.id,
                    name = config.name,
                    baseUrl = config.baseUrl,
                    isNew = false,
                    models = config.models,
                    selectedModel = config.selectedModel,
                ),
                errorMessage = null,
            )
        }
    }

    private fun cancelForm() {
        formGeneration++
        _uiState.update { it.copy(form = null) }
    }

    private fun updateForm(transform: (ProviderFormState) -> ProviderFormState) {
        _uiState.update { state ->
            val form = state.form ?: return@update state
            state.copy(form = transform(form))
        }
    }

    private fun fetchModels() {
        val form = _uiState.value.form ?: return
        if (form.isFetchingModels || form.isSaving) return

        val baseUrl = form.baseUrl.trim().trimEnd('/')
        val apiKey = form.apiKey.trim().ifBlank {
            form.providerId?.let { providerRepository.providerById(it)?.apiKey }.orEmpty()
        }

        if (!isValidHttpsUrl(baseUrl)) {
            updateForm {
                it.copy(
                    baseUrlError = "请输入有效的 https:// 地址",
                    modelFetchError = null,
                )
            }
            return
        }
        if (apiKey.isBlank()) {
            updateForm { it.copy(modelFetchError = "请先填写 API Key") }
            return
        }

        val generation = formGeneration
        updateForm { it.copy(isFetchingModels = true, modelFetchError = null) }

        viewModelScope.launch {
            try {
                val config = ProviderConfig(
                    id = form.providerId ?: UUID.randomUUID(),
                    name = form.name.ifBlank { "Provider" },
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                    type = ProviderType.OPENAI_COMPATIBLE,
                    models = form.models,
                    selectedModel = form.selectedModel,
                )
                val fetched = providerFactory(config).fetchModels()
                if (generation != formGeneration) return@launch

                _uiState.update { state ->
                    val current = state.form ?: return@update state
                    if (fetched.isEmpty()) {
                        state.copy(
                            form = current.copy(
                                isFetchingModels = false,
                                modelFetchError = "未拉取到模型，请手动添加",
                            ),
                        )
                    } else {
                        val selected = current.selectedModel
                            ?.takeIf { it in fetched }
                            ?: fetched.first()
                        state.copy(
                            form = current.copy(
                                models = fetched,
                                selectedModel = selected,
                                isFetchingModels = false,
                                modelFetchError = null,
                                newModelError = null,
                            ),
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generation != formGeneration) return@launch
                _uiState.update { state ->
                    state.copy(
                        form = state.form?.copy(
                            isFetchingModels = false,
                            modelFetchError = e.toModelFetchMessage(),
                        ),
                    )
                }
            }
        }
    }

    private fun addModel() {
        val form = _uiState.value.form ?: return
        val name = form.newModelName.trim()
        when {
            name.isEmpty() -> updateForm { it.copy(newModelError = "请输入模型名") }
            form.models.contains(name) -> updateForm { it.copy(newModelError = "模型已存在") }
            else -> updateForm {
                it.copy(
                    models = it.models + name,
                    selectedModel = it.selectedModel ?: name,
                    newModelName = "",
                    newModelError = null,
                )
            }
        }
    }

    private fun removeModel(model: String) {
        updateForm { form ->
            val models = form.models - model
            form.copy(
                models = models,
                selectedModel = if (form.selectedModel == model) {
                    models.firstOrNull()
                } else {
                    form.selectedModel
                },
            )
        }
    }

    private fun selectModel(model: String) {
        updateForm { form ->
            if (model in form.models) form.copy(selectedModel = model) else form
        }
    }

    private fun save() {
        val form = _uiState.value.form ?: return
        if (form.isSaving || form.isFetchingModels) return

        val name = form.name.trim()
        val baseUrl = form.baseUrl.trim().trimEnd('/')
        val nameError = if (name.isEmpty()) "请输入名称" else null
        val baseUrlError = when {
            baseUrl.isEmpty() -> "请输入 Base URL"
            !isValidHttpsUrl(baseUrl) -> "请输入有效的 https:// 地址"
            else -> null
        }
        val apiKeyError = if (form.isNew && form.apiKey.isBlank()) "请输入 API Key" else null

        if (nameError != null || baseUrlError != null || apiKeyError != null) {
            _uiState.update {
                it.copy(
                    form = form.copy(
                        nameError = nameError,
                        baseUrlError = baseUrlError,
                        apiKeyError = apiKeyError,
                    ),
                )
            }
            return
        }

        val models = form.models.distinct()
        val selectedModel = form.selectedModel?.takeIf { it in models } ?: models.firstOrNull()

        _uiState.update {
            it.copy(
                form = form.copy(
                    name = name,
                    baseUrl = baseUrl,
                    models = models,
                    selectedModel = selectedModel,
                    isSaving = true,
                    nameError = null,
                    baseUrlError = null,
                    apiKeyError = null,
                ),
            )
        }

        viewModelScope.launch {
            try {
                if (form.isNew) {
                    providerRepository.addProvider(
                        ProviderConfig(
                            name = name,
                            baseUrl = baseUrl,
                            apiKey = form.apiKey.trim(),
                            type = ProviderType.OPENAI_COMPATIBLE,
                            models = models,
                            selectedModel = selectedModel,
                        ),
                    )
                } else {
                    val providerId = form.providerId ?: error("Provider id 缺失")
                    val existing = providerRepository.providerById(providerId)
                        ?: error("Provider 不存在")
                    providerRepository.updateProvider(
                        existing.copy(
                            name = name,
                            baseUrl = baseUrl,
                            apiKey = if (form.apiKey.isBlank()) existing.apiKey else form.apiKey.trim(),
                            models = models,
                            selectedModel = selectedModel,
                        ),
                    )
                }
                formGeneration++
                _uiState.update { it.copy(form = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        form = it.form?.copy(isSaving = false),
                        errorMessage = "保存失败：${e.message ?: "未知错误"}",
                    )
                }
            }
        }
    }

    private fun setActive(provider: ProviderUiModel) {
        viewModelScope.launch {
            try {
                providerRepository.setActiveProvider(provider.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "切换 Provider 失败：${e.message ?: "未知错误"}")
                }
            }
        }
    }

    private fun confirmDelete() {
        val target = _uiState.value.pendingDelete ?: return
        viewModelScope.launch {
            try {
                providerRepository.deleteProvider(target.id)
                _uiState.update { it.copy(pendingDelete = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        pendingDelete = null,
                        errorMessage = "删除失败：${e.message ?: "未知错误"}",
                    )
                }
            }
        }
    }

    private fun setAppearanceMode(mode: AppearanceMode) {
        if (_uiState.value.appearanceMode == mode) return
        persistTheme { preferences.setAppearanceMode(mode) }
    }

    private fun setColorTheme(theme: AppColorTheme) {
        if (_uiState.value.colorTheme == theme) return
        persistTheme { preferences.setColorTheme(theme) }
    }

    private fun setDynamicColorEnabled(enabled: Boolean) {
        if (!_uiState.value.isDynamicColorSupported) return
        if (_uiState.value.dynamicColorEnabled == enabled) return
        persistTheme { preferences.setDynamicColorEnabled(enabled) }
    }

    /**
     * 主题偏好以 DataStore 为唯一数据源：写入成功后 Flow 回流更新 UiState，
     * MainActivity 收集到变化后立即重组 RecAIteLABTheme。
     */
    private fun persistTheme(write: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                write()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "保存主题设置失败，请稍后重试")
                }
            }
        }
    }

    /** 只接受 https 且带 host 的地址；调用方已 trim。 */
    private fun isValidHttpsUrl(value: String): Boolean = try {
        val uri = URI(value)
        uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    } catch (_: Exception) {
        false
    }

    private fun Throwable.toModelFetchMessage(): String = when (this) {
        is ApiException -> when (statusCode) {
            401, 403 -> "API Key 无效或无权限"
            404 -> "模型接口不存在，请检查 Base URL"
            429 -> "请求过于频繁，请稍后重试"
            in 500..599 -> "服务端错误（$statusCode）"
            else -> serverMessage.ifBlank { "请求失败（$statusCode）" }
        }
        is IOException -> "网络异常，请检查连接"
        else -> "拉取模型失败：${message ?: "未知错误"}"
    }

    private fun ProviderConfig.toUiModel(): ProviderUiModel = ProviderUiModel(
        id = id,
        name = name,
        baseUrl = baseUrl,
        selectedModel = selectedModel,
    )
}

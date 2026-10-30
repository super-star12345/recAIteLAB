package com.recaitelab.app.data.repository

import com.recaitelab.app.domain.model.ProviderConfig
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Provider 配置仓库（当前为内存实现，后续阶段换 Room）。
 *
 * - 写操作为 `suspend`，与未来 Room DAO 的写方法保持同一形态；
 * - 读操作通过 [providers] / [activeProvider] / [activeModel] 三个 StateFlow 实时观察；
 * - [activeProvider] 与 [activeModel] 始终跟随 [providers] 和当前激活 id，不会出现悬空引用。
 *
 * @param initialProviders 启动时的初始配置，第一个会被设为激活项
 */
class ProviderRepository(
    initialProviders: List<ProviderConfig> = emptyList(),
) {

    private val mutex = Mutex()

    private val _providers = MutableStateFlow(initialProviders)

    /** 全部 Provider，按添加顺序。 */
    val providers: StateFlow<List<ProviderConfig>> = _providers.asStateFlow()

    private val _activeProviderId = MutableStateFlow(initialProviders.firstOrNull()?.id)

    private val _activeProvider = MutableStateFlow(initialProviders.firstOrNull())

    /** 当前激活的 Provider；没有任何 Provider 时为 null。 */
    val activeProvider: StateFlow<ProviderConfig?> = _activeProvider.asStateFlow()

    private val _activeModel = MutableStateFlow(initialProviders.firstOrNull()?.selectedModel)

    /** 激活 Provider 当前选中的模型；未选择时为 null。 */
    val activeModel: StateFlow<String?> = _activeModel.asStateFlow()

    /** 新增 Provider；当前没有激活项时自动激活它。 */
    suspend fun addProvider(config: ProviderConfig) {
        mutex.withLock {
            require(_providers.value.none { it.id == config.id }) {
                "Provider 已存在: ${config.id}"
            }
            _providers.update { it + config }
            if (_activeProviderId.value == null) {
                _activeProviderId.value = config.id
            }
            publishActive()
        }
    }

    /** 按 id 全量更新；id 不存在时忽略。 */
    suspend fun updateProvider(config: ProviderConfig) {
        mutex.withLock {
            _providers.update { list ->
                list.map { if (it.id == config.id) config else it }
            }
            publishActive()
        }
    }

    /** 删除 Provider；删除的是激活项时，自动激活剩余的第一个。 */
    suspend fun deleteProvider(id: UUID) {
        mutex.withLock {
            val remaining = _providers.value.filterNot { it.id == id }
            if (remaining.size == _providers.value.size) return@withLock
            _providers.value = remaining
            if (_activeProviderId.value == id) {
                _activeProviderId.value = remaining.firstOrNull()?.id
            }
            publishActive()
        }
    }

    /** 切换激活 Provider；id 不存在时忽略。 */
    suspend fun setActiveProvider(id: UUID) {
        mutex.withLock {
            if (_providers.value.none { it.id == id }) return@withLock
            _activeProviderId.value = id
            publishActive()
        }
    }

    /** 设置某个 Provider 选中的模型；不影响其他 Provider。 */
    suspend fun setSelectedModel(providerId: UUID, model: String) {
        require(model.isNotBlank()) { "model 不能为空" }
        mutex.withLock {
            _providers.update { list ->
                list.map { if (it.id == providerId) it.copy(selectedModel = model) else it }
            }
            publishActive()
        }
    }

    /** 按 id 查询单个 Provider。 */
    fun providerById(id: UUID): ProviderConfig? =
        _providers.value.firstOrNull { it.id == id }

    private fun publishActive() {
        val provider = _providers.value.firstOrNull { it.id == _activeProviderId.value }
        _activeProvider.value = provider
        _activeModel.value = provider?.selectedModel
    }
}

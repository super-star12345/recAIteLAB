package com.recaitelab.app.data.repository

import com.recaitelab.app.data.local.dao.ProviderDao
import com.recaitelab.app.data.local.entity.toDomain
import com.recaitelab.app.data.local.entity.toEntity
import com.recaitelab.app.data.local.preferences.UserPreferencesRepository
import com.recaitelab.app.data.local.security.ApiKeyStore
import com.recaitelab.app.domain.model.ProviderConfig
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Provider 配置仓库：Room 持久化 + Keystore 加密 Key + DataStore 偏好。
 *
 * 对外接口与内存版保持一致：
 * - 写操作为 `suspend`，内部用 [mutex] 串行化；
 * - 读操作通过 [providers] / [activeProvider] / [activeModel] 三个 StateFlow 实时观察；
 * - [activeProvider] 与 [activeModel] 始终跟随 [providers] 和当前激活 id，不会出现悬空引用。
 *
 * 数据来源：
 * - Provider 配置：`providers` 表（不含 Key）；
 * - API Key：`ApiKeyStore`（Keystore AES-GCM 密文，存 DataStore `secure_keys`）；
 * - 激活 ProviderId / 最近选中模型：`UserPreferencesRepository`（DataStore）。
 *
 * @param scope 应用级协程作用域，用于持续合并 Room / DataStore 的变化
 */
class ProviderRepository(
    private val providerDao: ProviderDao,
    private val apiKeyStore: ApiKeyStore,
    private val preferences: UserPreferencesRepository,
    scope: CoroutineScope,
) {

    private val mutex = Mutex()

    private val _providers = MutableStateFlow<List<ProviderConfig>>(emptyList())

    /** 全部 Provider，按创建时间顺序。 */
    val providers: StateFlow<List<ProviderConfig>> = _providers.asStateFlow()

    private val _activeProvider = MutableStateFlow<ProviderConfig?>(null)

    /** 当前激活的 Provider；没有任何 Provider 时为 null。 */
    val activeProvider: StateFlow<ProviderConfig?> = _activeProvider.asStateFlow()

    private val _activeModel = MutableStateFlow<String?>(null)

    /** 激活 Provider 当前选中的模型；未选择时为 null。 */
    val activeModel: StateFlow<String?> = _activeModel.asStateFlow()

    init {
        scope.launch {
            combine(
                providerDao.observeAll(),
                apiKeyStore.apiKeys,
                preferences.activeProviderId,
                preferences.selectedModel,
            ) { rows, keys, activeProviderId, selectedModel ->
                val activeId = activeProviderId?.let {
                    runCatching { UUID.fromString(it) }.getOrNull()
                }
                val configs = rows.mapNotNull { row ->
                    row.toDomain(apiKey = keys[row.id].orEmpty())
                }.map { config ->
                    // 数据库中的 selectedModel 为空时，用 DataStore 的最近选择兜底。
                    if (config.id == activeId && config.selectedModel == null) {
                        config.copy(
                            selectedModel = selectedModel?.takeIf { it in config.models },
                        )
                    } else {
                        config
                    }
                }
                ProviderSnapshot(
                    providers = configs,
                    activeProviderId = activeId,
                )
            }.collect { snapshot -> publish(snapshot) }
        }
    }

    /** 新增 Provider；当前没有激活项时自动激活它。 */
    suspend fun addProvider(config: ProviderConfig) {
        mutex.withLock {
            val id = config.id.toString()
            require(providerDao.findById(id) == null) {
                "Provider 已存在: ${config.id}"
            }
            val now = System.currentTimeMillis()
            providerDao.insert(config.toEntity(createdAt = now, updatedAt = now))
            apiKeyStore.put(config.id, config.apiKey)
            if (providerDao.count() == 1) {
                preferences.setActiveProviderId(id)
                config.selectedModel?.let { model -> preferences.setSelectedModel(model) }
            }
        }
    }

    /** 按 id 全量更新；id 不存在时忽略。 */
    suspend fun updateProvider(config: ProviderConfig) {
        mutex.withLock {
            val id = config.id.toString()
            val existing = providerDao.findById(id) ?: return@withLock
            providerDao.update(
                config.toEntity(
                    createdAt = existing.createdAt,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            apiKeyStore.put(config.id, config.apiKey)
            if (preferences.activeProviderId.first() == id) {
                config.selectedModel?.let { model -> preferences.setSelectedModel(model) }
            }
        }
    }

    /** 删除 Provider；删除的是激活项时，自动激活剩余的第一个。 */
    suspend fun deleteProvider(id: UUID) {
        mutex.withLock {
            val idText = id.toString()
            if (providerDao.findById(idText) == null) return@withLock
            // 先删库，再删密文，避免留下无法关联的 Key。
            providerDao.deleteById(idText)
            apiKeyStore.remove(id)
            if (preferences.activeProviderId.first() == idText) {
                val next = providerDao.getAll().firstOrNull()
                preferences.setActiveProviderId(next?.id)
                next?.selectedModel?.let { model -> preferences.setSelectedModel(model) }
            }
        }
    }

    /** 切换激活 Provider；id 不存在时忽略。 */
    suspend fun setActiveProvider(id: UUID) {
        mutex.withLock {
            val entity = providerDao.findById(id.toString()) ?: return@withLock
            preferences.setActiveProviderId(entity.id)
            entity.selectedModel?.let { model -> preferences.setSelectedModel(model) }
        }
    }

    /** 设置某个 Provider 选中的模型；不影响其他 Provider。 */
    suspend fun setSelectedModel(providerId: UUID, model: String) {
        require(model.isNotBlank()) { "model 不能为空" }
        mutex.withLock {
            val idText = providerId.toString()
            if (providerDao.findById(idText) == null) return@withLock
            providerDao.updateSelectedModel(
                id = idText,
                model = model,
                updatedAt = System.currentTimeMillis(),
            )
            preferences.setSelectedModel(model)
        }
    }

    /** 按 id 查询单个 Provider；数据来自最近一次数据库快照。 */
    fun providerById(id: UUID): ProviderConfig? =
        _providers.value.firstOrNull { it.id == id }

    /**
     * 合并数据库与偏好，并维护激活项回退：
     * 存储的激活 id 失效（被删除 / 首次启动）时，自动回退到第一个 Provider 并写回偏好。
     */
    private suspend fun publish(snapshot: ProviderSnapshot) {
        val active = snapshot.providers.firstOrNull { it.id == snapshot.activeProviderId }
            ?: snapshot.providers.firstOrNull()

        _providers.value = snapshot.providers
        _activeProvider.value = active
        _activeModel.value = active?.selectedModel

        val resolvedId = active?.id?.toString()
        if (resolvedId != snapshot.activeProviderId?.toString()) {
            preferences.setActiveProviderId(resolvedId)
        }
    }

    private data class ProviderSnapshot(
        val providers: List<ProviderConfig>,
        val activeProviderId: UUID?,
    )
}

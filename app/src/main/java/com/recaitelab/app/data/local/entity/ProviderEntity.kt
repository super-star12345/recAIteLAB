package com.recaitelab.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.recaitelab.app.domain.model.ProviderConfig
import com.recaitelab.app.domain.model.ProviderType
import java.util.UUID

/**
 * Provider 配置表。
 *
 * - [id] 沿用领域层的 `UUID` 字符串，保证 [ProviderConfig] 与 Repository 接口不变；
 * - [models] / [selectedModel] 中的模块列表由 `Converters` 以 JSON 数组存为 TEXT；
 * - **不存储 apiKey**：Key 由 Keystore AES-GCM 加密后写入 DataStore `secure_keys`。
 */
@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val baseUrl: String,
    val type: ProviderType,
    val models: List<String>,
    val selectedModel: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

/** 实体 → 领域模型；[apiKey] 由密钥存储解密后注入。id 非法时返回 null。 */
fun ProviderEntity.toDomain(apiKey: String): ProviderConfig? {
    val providerId = runCatching { UUID.fromString(id) }.getOrNull() ?: return null
    return ProviderConfig(
        id = providerId,
        name = name,
        baseUrl = baseUrl,
        apiKey = apiKey,
        type = type,
        models = models,
        selectedModel = selectedModel,
    )
}

/** 领域模型 → 实体；创建时间由调用方沿用旧值。 */
fun ProviderConfig.toEntity(createdAt: Long, updatedAt: Long): ProviderEntity = ProviderEntity(
    id = id.toString(),
    name = name,
    baseUrl = baseUrl,
    type = type,
    models = models,
    selectedModel = selectedModel,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

package com.recaitelab.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.recaitelab.app.domain.model.KnowledgeEntry

/**
 * 知识条目表；与领域模型 [KnowledgeEntry] 的互转见文件末尾扩展函数。
 *
 * @property tags 由 `Converters` 以 JSON 数组存为 TEXT
 * @property sourceConversationId 来源会话；会话删除时保留条目并置空
 * @property providerId 整理所用 Provider 的 `UUID` 字符串；删除 Provider 时置空
 * @property model 整理所用模型快照
 */
@Entity(
    tableName = "knowledge_entries",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceConversationId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = ProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("sourceConversationId"),
        Index("providerId"),
        Index("updatedAt"),
    ],
)
data class KnowledgeEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val summary: String,
    val content: String,
    val tags: List<String> = emptyList(),
    val sourceConversationId: Long? = null,
    val providerId: String? = null,
    val model: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** 实体 → 领域模型。 */
fun KnowledgeEntryEntity.toDomain(): KnowledgeEntry = KnowledgeEntry(
    id = id,
    title = title,
    summary = summary,
    content = content,
    tags = tags,
    sourceConversationId = sourceConversationId,
    providerId = providerId,
    model = model,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

/** 领域模型 → 实体；id 为 0 时由 Room 自增。 */
fun KnowledgeEntry.toEntity(): KnowledgeEntryEntity = KnowledgeEntryEntity(
    id = id,
    title = title,
    summary = summary,
    content = content,
    tags = tags,
    sourceConversationId = sourceConversationId,
    providerId = providerId,
    model = model,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

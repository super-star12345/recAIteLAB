package com.recaitelab.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 会话表。
 *
 * @property providerId 当前 Provider 的 `UUID` 字符串；Provider 删除时置空
 * @property model 当前选中的模型 id（接口返回的原始名称）
 */
@Entity(
    tableName = "conversations",
    foreignKeys = [
        ForeignKey(
            entity = ProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("providerId"),
        Index("updatedAt"),
    ],
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val providerId: String? = null,
    val model: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

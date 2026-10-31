package com.recaitelab.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.recaitelab.app.data.local.entity.ConversationEntity
import com.recaitelab.app.data.local.model.ConversationSummary
import kotlinx.coroutines.flow.Flow

/** 会话读写；历史记录 UI 直接消费 [observeSummaries]。 */
@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    /**
     * 会话列表摘要：按 updatedAt 倒序，并带出最后一条消息的正文。
     * 用相关子查询取最后一条，避免 N+1 查询。
     */
    @Query(
        """
        SELECT c.id AS id,
               c.title AS title,
               c.updatedAt AS updatedAt,
               m.content AS lastMessage
        FROM conversations c
        LEFT JOIN messages m ON m.id = (
            SELECT m2.id
            FROM messages m2
            WHERE m2.conversationId = c.id
            ORDER BY m2.timestamp DESC, m2.id DESC
            LIMIT 1
        )
        ORDER BY c.updatedAt DESC, c.id DESC
        """,
    )
    fun observeSummaries(): Flow<List<ConversationSummary>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): ConversationEntity?

    /** 最近更新的会话；用于 DataStore 里没有有效 id 时兜底恢复。 */
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC, id DESC LIMIT 1")
    suspend fun getLatest(): ConversationEntity?

    @Insert
    suspend fun insert(entity: ConversationEntity): Long

    @Update
    suspend fun update(entity: ConversationEntity)

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String, updatedAt: Long)

    @Query("UPDATE conversations SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(id: Long, updatedAt: Long)

    @Query(
        "UPDATE conversations SET providerId = :providerId, model = :model, updatedAt = :updatedAt " +
            "WHERE id = :id",
    )
    suspend fun updateTarget(id: Long, providerId: String?, model: String?, updatedAt: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: Long)
}

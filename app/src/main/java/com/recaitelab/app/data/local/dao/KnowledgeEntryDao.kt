package com.recaitelab.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.recaitelab.app.data.local.entity.KnowledgeEntryEntity
import kotlinx.coroutines.flow.Flow

/** 知识条目读写；搜索匹配标题 / 摘要 / 正文。 */
@Dao
interface KnowledgeEntryDao {

    @Query("SELECT * FROM knowledge_entries ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<KnowledgeEntryEntity>>

    @Query(
        "SELECT * FROM knowledge_entries " +
            "WHERE title LIKE '%' || :query || '%' " +
            "OR summary LIKE '%' || :query || '%' " +
            "OR content LIKE '%' || :query || '%' " +
            "ORDER BY updatedAt DESC",
    )
    fun search(query: String): Flow<List<KnowledgeEntryEntity>>

    @Query("SELECT * FROM knowledge_entries WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<KnowledgeEntryEntity?>

    @Query("SELECT * FROM knowledge_entries WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): KnowledgeEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: KnowledgeEntryEntity): Long

    @Query("DELETE FROM knowledge_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}

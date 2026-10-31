package com.recaitelab.app.data.repository

import com.recaitelab.app.data.local.dao.KnowledgeEntryDao
import com.recaitelab.app.data.local.entity.toDomain
import com.recaitelab.app.data.local.entity.toEntity
import com.recaitelab.app.domain.model.KnowledgeEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 知识库数据仓库。
 *
 * 本阶段只提供持久化与查询能力，不涉及整理 Prompt、AI 调用或 UI。
 * 写入/查询均在 Room 的 suspend / Flow API 上完成，不阻塞主线程。
 */
class KnowledgeRepository(
    private val knowledgeEntryDao: KnowledgeEntryDao,
) {

    /** 全部知识条目，按 updatedAt 倒序。 */
    val entries: Flow<List<KnowledgeEntry>> = knowledgeEntryDao.observeAll()
        .map { entities -> entities.map { it.toDomain() } }

    /**
     * 按关键字搜索：标题、摘要或正文包含关键字即匹配（不区分 ASCII 大小写）。
     * 空白关键字返回全部条目。
     */
    fun search(query: String): Flow<List<KnowledgeEntry>> =
        knowledgeEntryDao.search(query.trim())
            .map { entities -> entities.map { it.toDomain() } }

    /** 新增或覆盖条目；返回 Room 主键。 */
    suspend fun insert(entry: KnowledgeEntry): Long =
        knowledgeEntryDao.upsert(entry.toEntity())

    /** 按 id 删除条目。 */
    suspend fun delete(id: Long) {
        knowledgeEntryDao.deleteById(id)
    }

    /** 按 id 查询单条；不存在时返回 null。 */
    suspend fun getById(id: Long): KnowledgeEntry? =
        knowledgeEntryDao.findById(id)?.toDomain()
}

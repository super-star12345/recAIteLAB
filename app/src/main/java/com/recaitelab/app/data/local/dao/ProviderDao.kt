package com.recaitelab.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.recaitelab.app.data.local.entity.ProviderEntity
import kotlinx.coroutines.flow.Flow

/** Provider 配置读写；Key 不在此表，见 `ApiKeyStore`。 */
@Dao
interface ProviderDao {

    @Query("SELECT * FROM providers ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers ORDER BY createdAt ASC")
    suspend fun getAll(): List<ProviderEntity>

    @Query("SELECT * FROM providers WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ProviderEntity?

    @Insert
    suspend fun insert(entity: ProviderEntity)

    @Update
    suspend fun update(entity: ProviderEntity)

    @Query("UPDATE providers SET selectedModel = :model, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSelectedModel(id: String, model: String?, updatedAt: Long)

    @Query("DELETE FROM providers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM providers")
    suspend fun count(): Int
}

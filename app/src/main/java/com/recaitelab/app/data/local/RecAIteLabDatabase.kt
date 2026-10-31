package com.recaitelab.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.recaitelab.app.data.local.dao.ConversationDao
import com.recaitelab.app.data.local.dao.KnowledgeEntryDao
import com.recaitelab.app.data.local.dao.MessageDao
import com.recaitelab.app.data.local.dao.ProviderDao
import com.recaitelab.app.data.local.entity.ConversationEntity
import com.recaitelab.app.data.local.entity.KnowledgeEntryEntity
import com.recaitelab.app.data.local.entity.MessageEntity
import com.recaitelab.app.data.local.entity.ProviderEntity

/**
 * 应用数据库 `recaitelab.db`。
 *
 * 版本 1：Provider / Conversation / Message / KnowledgeEntry 四张表。
 * schema 导出到 `app/schemas`，后续升级必须补 `Migration`，不要直接 destructive。
 */
@Database(
    entities = [
        ProviderEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        KnowledgeEntryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class RecAIteLabDatabase : RoomDatabase() {

    abstract fun providerDao(): ProviderDao

    abstract fun conversationDao(): ConversationDao

    abstract fun messageDao(): MessageDao

    abstract fun knowledgeEntryDao(): KnowledgeEntryDao

    companion object {

        const val DATABASE_NAME: String = "recaitelab.db"

        @Volatile
        private var instance: RecAIteLabDatabase? = null

        fun getInstance(context: Context): RecAIteLabDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RecAIteLabDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }
    }
}

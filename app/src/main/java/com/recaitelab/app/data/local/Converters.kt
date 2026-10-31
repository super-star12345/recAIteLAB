package com.recaitelab.app.data.local

import androidx.room.TypeConverter
import com.recaitelab.app.domain.model.ProviderType
import java.util.Locale
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Room 类型转换器。
 *
 * 集合与枚举一律以稳定字符串落库（JSON 数组 / 小写枚举名），不使用 ordinal，
 * 避免枚举顺序或内部格式变化影响已有数据。
 */
class Converters {

    private val json: Json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>?): String =
        json.encodeToString(ListSerializer(String.serializer()), value.orEmpty())

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), value)
        }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromProviderType(value: ProviderType?): String =
        (value ?: ProviderType.OPENAI_COMPATIBLE).name.lowercase(Locale.ROOT)

    @TypeConverter
    fun toProviderType(value: String?): ProviderType =
        ProviderType.entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: ProviderType.OPENAI_COMPATIBLE
}

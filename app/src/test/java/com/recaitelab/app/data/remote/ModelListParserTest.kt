package com.recaitelab.app.data.remote

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GET /models 响应解析回归测试：
 * 正确读取 data[].id、空列表、缺失/异常结构不崩溃（以 Result.failure 收敛）。
 */
class ModelListParserTest {

    @Test
    fun parsesAndSanitizesModelIds() {
        val raw = """
            {
              "object": "list",
              "data": [
                {"id": " gpt-4o ", "object": "model"},
                {"id": "deepseek-chat"},
                {"id": ""},
                {"id": "   "},
                {"id": "gpt-4o"}
              ]
            }
        """.trimIndent()

        assertEquals(
            listOf("gpt-4o", "deepseek-chat"),
            ModelListParser.parse(raw).getOrThrow(),
        )
    }

    @Test
    fun emptyDataReturnsEmptyList() {
        assertEquals(
            emptyList<String>(),
            ModelListParser.parse("""{"data":[]}""").getOrThrow(),
        )
    }

    @Test
    fun missingDataReturnsEmptyList() {
        assertEquals(
            emptyList<String>(),
            ModelListParser.parse("{}").getOrThrow(),
        )
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val raw = """{"object":"list","data":[{"id":"m1","owned_by":"local"}]}"""

        assertEquals(listOf("m1"), ModelListParser.parse(raw).getOrThrow())
    }

    @Test
    fun malformedJsonReturnsFailureInsteadOfThrowing() {
        val result = ModelListParser.parse("""{"data":[""")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SerializationException)
    }

    @Test
    fun wrongDataStructureReturnsFailure() {
        val result = ModelListParser.parse("""{"data":"not-a-list"}""")

        assertTrue(result.isFailure)
    }

    @Test
    fun nullDataReturnsFailure() {
        val result = ModelListParser.parse("""{"data":null}""")

        assertTrue(result.isFailure)
    }
}

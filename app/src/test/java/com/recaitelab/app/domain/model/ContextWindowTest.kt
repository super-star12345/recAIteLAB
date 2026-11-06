package com.recaitelab.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * 上下文截断回归测试：
 * 保留 system、历史条数上限、当前消息保留、顺序不变。
 */
class ContextWindowTest {

    private fun history(count: Int): List<ChatMessage> = List(count) { index ->
        ChatMessage(
            role = if (index % 2 == 0) "user" else "assistant",
            content = "history-$index",
        )
    }

    @Test
    fun keepsSystemRecentHistoryAndCurrentInOrder() {
        val system = ChatMessage(role = "system", content = "你是一个助手")
        val messages = buildList {
            add(system)
            addAll(history(25))
            add(ChatMessage(role = "user", content = "current"))
        }

        val truncated = ContextWindow.truncate(messages)

        // system 1 + 最近 20 条历史 + current 1 = 22。
        assertEquals(22, truncated.size)
        assertEquals("system", truncated.first().role)
        assertEquals("current", truncated.last().content)

        // 被丢弃的是最旧的 5 条历史（history-0..history-4），其余保持原顺序。
        val expected = messages.filterIndexed { index, _ -> index == 0 || index >= 6 }
        assertEquals(expected, truncated)
        assertEquals("history-5", truncated[1].content)
        assertEquals("history-24", truncated[20].content)
    }

    @Test
    fun keepsAtMostTwentyHistoryMessagesWithoutSystem() {
        val messages = history(25) + ChatMessage(role = "user", content = "current")

        val truncated = ContextWindow.truncate(messages)

        assertEquals(21, truncated.size)
        assertEquals("history-5", truncated.first().content)
        assertEquals("current", truncated.last().content)
    }

    @Test
    fun shortConversationIsUnchanged() {
        val messages = listOf(
            ChatMessage(role = "system", content = "sys"),
            ChatMessage(role = "user", content = "q1"),
            ChatMessage(role = "assistant", content = "a1"),
            ChatMessage(role = "user", content = "current"),
        )

        assertEquals(messages, ContextWindow.truncate(messages))
    }

    @Test
    fun onlySystemMessagesArePreserved() {
        val messages = listOf(
            ChatMessage(role = "system", content = "sys-1"),
            ChatMessage(role = "system", content = "sys-2"),
        )

        assertEquals(messages, ContextWindow.truncate(messages))
    }

    @Test
    fun zeroHistoryKeepsSystemAndCurrent() {
        val messages = listOf(
            ChatMessage(role = "system", content = "sys"),
            ChatMessage(role = "user", content = "history-0"),
            ChatMessage(role = "assistant", content = "history-1"),
            ChatMessage(role = "user", content = "current"),
        )

        val truncated = ContextWindow.truncate(messages, maxHistoryMessages = 0)

        assertEquals(
            listOf(
                ChatMessage(role = "system", content = "sys"),
                ChatMessage(role = "user", content = "current"),
            ),
            truncated,
        )
    }

    @Test
    fun emptyListStaysEmpty() {
        assertEquals(emptyList<ChatMessage>(), ContextWindow.truncate(emptyList()))
    }

    @Test
    fun originalListIsNotModified() {
        val messages = buildList {
            add(ChatMessage(role = "system", content = "sys"))
            addAll(history(25))
            add(ChatMessage(role = "user", content = "current"))
        }
        val before = messages.toList()

        val truncated = ContextWindow.truncate(messages)

        assertEquals(before, messages)
        assertNotSame(messages, truncated)
    }

    @Test
    fun multipleSystemMessagesAreAllPreserved() {
        val systems = listOf(
            ChatMessage(role = "system", content = "sys-1"),
            ChatMessage(role = "system", content = "sys-2"),
        )
        val messages = systems + history(5) + ChatMessage(role = "user", content = "current")

        val truncated = ContextWindow.truncate(messages)

        assertEquals(2 + 5 + 1, truncated.size)
        assertEquals(systems, truncated.take(2))
        assertEquals("current", truncated.last().content)
    }

    @Test
    fun historyLimitLargerThanConversationKeepsEverything() {
        val messages = listOf(
            ChatMessage(role = "system", content = "sys"),
            ChatMessage(role = "user", content = "q1"),
            ChatMessage(role = "assistant", content = "a1"),
            ChatMessage(role = "user", content = "current"),
        )

        assertEquals(
            messages,
            ContextWindow.truncate(messages, maxHistoryMessages = 100),
        )
    }

    @Test
    fun singleCurrentMessageIsKept() {
        val current = ChatMessage(role = "user", content = "only")
        assertEquals(listOf(current), ContextWindow.truncate(listOf(current)))
    }

    @Test
    fun lastAssistantMessageIsKeptAsCurrentForOrganizer() {
        val messages = listOf(
            ChatMessage(role = "system", content = "sys"),
            ChatMessage(role = "user", content = "q1"),
            ChatMessage(role = "assistant", content = "a1"),
        )

        assertEquals(
            listOf(
                ChatMessage(role = "system", content = "sys"),
                ChatMessage(role = "assistant", content = "a1"),
            ),
            ContextWindow.truncate(messages, maxHistoryMessages = 0),
        )
    }

    @Test
    fun negativeHistoryLimitThrows() {
        assertThrows(IllegalArgumentException::class.java) {
            ContextWindow.truncate(
                listOf(ChatMessage(role = "user", content = "q")),
                maxHistoryMessages = -1,
            )
        }
    }
}

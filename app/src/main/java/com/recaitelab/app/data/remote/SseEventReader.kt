package com.recaitelab.app.data.remote

import okio.BufferedSource

/**
 * 按 SSE 规范从响应体中读取事件数据。
 *
 * - 空行是事件分隔符；
 * - 只有 `data:` 行进入事件负载，`event:` / `id:` / `retry:` 与 `:` 开头的注释（心跳）行忽略；
 * - 同一事件的多条 `data:` 行按规范用 `\n` 连接。
 *
 * 与网络无关，便于后续单元测试覆盖。
 */
internal class SseEventReader(private val source: BufferedSource) {

    /**
     * 读取下一个事件的 data 负载；到达流末尾返回 null。
     *
     * 注意：这里只做阻塞读取，取消由调用方通过关闭连接（`Call.cancel()`）实现。
     */
    fun nextEventData(): String? {
        val data = StringBuilder()
        while (true) {
            val line = source.readUtf8Line() ?: break
            if (line.isEmpty()) {
                if (data.isNotEmpty()) {
                    return data.toString().trimEnd('\n')
                }
                continue
            }
            if (line.startsWith(":")) {
                continue
            }
            if (!line.startsWith(DATA_FIELD)) {
                continue
            }
            val value = line.substring(DATA_FIELD.length).trimStart(' ')
            if (value.isNotEmpty()) {
                data.append(value).append('\n')
            }
        }
        return data.toString().trimEnd('\n').takeIf { it.isNotEmpty() }
    }

    private companion object {
        const val DATA_FIELD = "data:"
    }
}

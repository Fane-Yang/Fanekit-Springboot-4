package com.fanekit.common.util.net.http

import okhttp3.sse.EventSource

/**
 * SSE 连接管理器
 */
class SseConnection(
    private val eventSource: EventSource
) {
    fun close() {
        eventSource.cancel()
    }

    fun isOpen(): Boolean {
        // EventSource 没有直接的状态检查方法，需要通过监听器跟踪状态
        return true // 实际实现中应该维护状态
    }
}
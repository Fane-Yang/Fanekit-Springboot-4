package com.fanekit.common.util.net.http

/**
 * SSE 事件监听器接口
 */
interface SseEventListener {
    fun onOpen()
    fun onMessage(data: String, id: String?, type: String?)
    fun onError(error: Throwable)
    fun onClosed()
}
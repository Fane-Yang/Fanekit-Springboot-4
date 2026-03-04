package com.fanekit.data.kafka.message

import java.time.LocalDateTime

/**
 * Kafka消息事件类 - 用于封装消息相关信息
 *
 * @author fanekit
 * @since 2024/1/1
 */
data class KafkaMessageEvent(
    val topic: String,
    val partition: Int,
    val offset: Long,
    val key: String?,
    val value: Any?,
    val timestamp: Long,
    val headers: Map<String, Any> = emptyMap(),
    val receivedAt: LocalDateTime = LocalDateTime.now()
) {

    /**
     * 获取消息的唯一标识
     */
    fun getMessageId(): String {
        return "${topic}-${partition}-${offset}"
    }

    /**
     * 检查消息是否包含指定的头信息
     */
    fun hasHeader(headerName: String): Boolean {
        return headers.containsKey(headerName)
    }

    /**
     * 获取指定的头信息
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getHeader(headerName: String, type: Class<T>): T? {
        return headers[headerName]?.let {
            if (type.isInstance(it)) it as T else null
        }
    }

    /**
     * 获取字符串类型的头信息
     */
    fun getStringHeader(headerName: String): String? {
        return getHeader(headerName, String::class.java)
    }

    /**
     * 转换为日志格式字符串
     */
    fun toLogString(): String {
        return "KafkaMessage[topic=$topic, partition=$partition, offset=$offset, key=$key, timestamp=$timestamp]"
    }
}

/**
 * Kafka批量消息事件类
 */
data class KafkaBatchMessageEvent(
    val messages: List<KafkaMessageEvent>,
    val batchSize: Int = messages.size,
    val receivedAt: LocalDateTime = LocalDateTime.now()
) {

    /**
     * 获取批量消息的主题列表
     */
    fun getTopics(): Set<String> {
        return messages.map { it.topic }.toSet()
    }

    /**
     * 按主题分组消息
     */
    fun groupByTopic(): Map<String, List<KafkaMessageEvent>> {
        return messages.groupBy { it.topic }
    }

    /**
     * 获取批量消息的偏移量范围
     */
    fun getOffsetRange(): Pair<Long, Long>? {
        if (messages.isEmpty()) return null
        val offsets = messages.map { it.offset }
        return Pair(offsets.minOrNull()!!, offsets.maxOrNull()!!)
    }
}

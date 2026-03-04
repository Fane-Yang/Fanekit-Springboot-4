package com.fanekit.data.kafka.consumer

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.support.Acknowledgment

/**
 * Kafka消息监听器基类 - 提供通用的消息处理功能
 *
 * @author fanekit
 * @since 2024/1/1
 */
abstract class AbstractKafkaMessageListener {

    protected val log: Logger = LoggerFactory.getLogger(this::class.java)

    /**
     * 处理消息的抽象方法，子类必须实现
     *
     * @param message 消息内容（可为空）
     * @param record 消费记录
     * @param acknowledgment 手动确认对象
     */
    abstract fun handleMessage(message: Any?, record: ConsumerRecord<String, Any?>, acknowledgment: Acknowledgment?)

    /**
     * 通用消息处理方法
     */
    protected fun processMessage(
        message: Any?,
        record: ConsumerRecord<String, Any?>,
        acknowledgment: Acknowledgment?,
        topic: String? = null,
        partition: Int? = null,
        offset: Long? = null,
        key: String? = null
    ) {
        try {
            log.debug("接收到Kafka消息 - 主题: {}, 分区: {}, 偏移量: {}, 键: {}",
                topic ?: record.topic(),
                partition ?: record.partition(),
                offset ?: record.offset(),
                key ?: record.key()
            )

            // 调用具体的消息处理逻辑
            handleMessage(message, record, acknowledgment)

            // 手动确认消息
            acknowledgment?.acknowledge()

            log.debug("消息处理完成 - 主题: {}, 偏移量: {}", record.topic(), record.offset())

        } catch (e: Exception) {
            log.error("消息处理失败 - 主题: {}, 偏移量: {}, 错误: {}",
                record.topic(), record.offset(), e.message, e)

            // 处理失败的消息
            handleProcessingError(message, record, e, acknowledgment)
        }
    }

    /**
     * 处理消息处理异常
     *
     * @param message 原始消息
     * @param record 消费记录
     * @param exception 异常信息
     * @param acknowledgment 确认对象
     */
    protected open fun handleProcessingError(
        message: Any?,
        record: ConsumerRecord<String, Any?>,
        exception: Exception,
        acknowledgment: Acknowledgment?
    ) {
        // 默认实现：记录错误并确认消息（防止重复消费）
        log.error("默认错误处理 - 确认失败的消息以防止重复消费")
        acknowledgment?.acknowledge()

        // 子类可以重写此方法实现自定义错误处理逻辑
        // 例如：发送到死信队列、重试机制等
    }

    /**
     * 验证消息是否有效
     */
    protected open fun isValidMessage(message: Any?, record: ConsumerRecord<String, Any?>): Boolean {
        if (message == null) {
            log.warn("接收到空消息 - 主题: {}, 偏移量: {}", record.topic(), record.offset())
            return false
        }
        return true
    }

    /**
     * 记录消息处理统计信息
     */
    protected fun logMessageStats(record: ConsumerRecord<String, Any?>, processingTimeMs: Long) {
        log.debug("消息处理统计 - 主题: {}, 分区: {}, 偏移量: {}, 处理时间: {}ms",
            record.topic(), record.partition(), record.offset(), processingTimeMs)
    }
}

/**
 * 简单的Kafka消息监听器实现示例
 */
abstract class SimpleKafkaMessageListener : AbstractKafkaMessageListener() {

    /**
     * 简化的消息处理方法，只需要实现业务逻辑
     */
    abstract fun processBusinessLogic(message: Any)

    override fun handleMessage(message: Any?, record: ConsumerRecord<String, Any?>, acknowledgment: Acknowledgment?) {
        if (!isValidMessage(message, record)) {
            acknowledgment?.acknowledge()
            return
        }

        val startTime = System.currentTimeMillis()

        try {
            // 执行业务逻辑（此处消息一定非空）
            processBusinessLogic(message!!)

            val processingTime = System.currentTimeMillis() - startTime
            logMessageStats(record, processingTime)

        } catch (e: Exception) {
            log.error("业务逻辑处理失败: {}", e.message, e)
            throw e // 重新抛出异常，让父类处理
        }
    }
}

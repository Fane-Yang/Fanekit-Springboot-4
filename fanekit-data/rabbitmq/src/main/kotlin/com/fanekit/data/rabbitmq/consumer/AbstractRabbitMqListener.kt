package com.fanekit.data.rabbitmq.consumer

import com.rabbitmq.client.Channel
import org.slf4j.LoggerFactory

/**
 * RabbitMQ 消费者基类

 * 提供通用的消息处理模板和手动确认机制
 */
abstract class AbstractRabbitMqListener {

    protected val log = LoggerFactory.getLogger(this.javaClass)

    /**
     * 安全处理消息，包含异常捕获和 Ack/Nack 逻辑
     * 子类应该在 @RabbitListener 方法中调用此方法
     *
     * @param message 消息内容
     * @param channel AMQP 通道
     * @param tag 消息投递标签
     * @param action 真正的业务处理逻辑
     */
    protected fun <T> processMessage(
        message: T,
        channel: Channel,
        tag: Long,
        action: (T) -> Unit
    ) {
        log.debug("接收 RabbitMQ 消息 - Tag: {}, Message: {}", tag, message)
        try {
            action(message)
            channel.basicAck(tag, false)
        } catch (e: Exception) {
            log.error("消息处理失败 - Tag: {}, Message: {}, Error: {}", tag, message, e.message, e)
            try {
                // 拒绝策略：不重回队列 (requeue=false)，通常会进入死信队列或丢弃
                channel.basicNack(tag, false, false)
            } catch (ex: Exception) {
                log.error("消息拒绝失败 - Tag: {}", tag, ex)
            }
        }
    }
}


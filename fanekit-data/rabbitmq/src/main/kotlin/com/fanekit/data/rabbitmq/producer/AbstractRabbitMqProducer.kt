package com.fanekit.data.rabbitmq.producer

import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.connection.CorrelationData
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

/**
 * RabbitMQ 生产者基类
 *
 * 提供通用的消息发送可以被继承使用
 */
abstract class AbstractRabbitMqProducer {

    protected val log = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    protected lateinit var rabbitTemplate: RabbitTemplate

    /**
     * 发送简单消息
     * @param exchange 交换机名称
     * @param routingKey 路由键
     * @param message 消息对象（支持 JSON 自动序列化）
     */
    fun send(exchange: String, routingKey: String, message: Any) {
        log.debug("发送 RabbitMQ 消息 - Exchange: {}, RoutingKey: {}", exchange, routingKey)
        rabbitTemplate.convertAndSend(exchange, routingKey, message)
    }

    /**
     * 发送带有关联 ID 的消息，用于 Confirmed 模式
     * @param exchange 交换机名称
     * @param routingKey 路由键
     * @param message 消息对象
     * @return 关联 ID (UUID)
     */
    fun sendWithConfirm(exchange: String, routingKey: String, message: Any): String {
        val correlationId = UUID.randomUUID().toString()
        val correlationData = CorrelationData(correlationId)

        log.debug("发送带确认机制的消息 - Exchange: {}, RoutingKey: {}, CorrelationId: {}", exchange, routingKey, correlationId)
        rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData)
        return correlationId
    }

    /**
     * 发送延时消息 (依赖 rabbitmq_delayed_message_exchange 插件)
     * @param exchange 交换机名称
     * @param routingKey 路由键
     * @param message 消息对象
     * @param delayMs 延时时间（毫秒）
     */
    fun sendDelayed(exchange: String, routingKey: String, message: Any, delayMs: Int) {
        log.debug("发送延时消息 - Exchange: {}, RoutingKey: {}, Delay: {}ms", exchange, routingKey, delayMs)
        rabbitTemplate.convertAndSend(exchange, routingKey, message) { msg ->
            msg.messageProperties.setHeader("x-delay", delayMs)
            msg
        }
    }
}


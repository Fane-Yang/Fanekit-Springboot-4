package com.fanekit.data.rabbitmq

import com.fanekit.data.rabbitmq.config.FastJson2MessageConverter
import org.slf4j.LoggerFactory
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * RabbitMQ 自动配置类
 * 提供基础的消息转换器配置
 */
@Configuration
class RabbitMqConfiguration {

    private val log = LoggerFactory.getLogger(RabbitMqConfiguration::class.java)

    /**
     * 配置消息转换器为 FastJson2MessageConverter
     * 这样可以直接发送对象，自动序列化为 JSON
     * Spring Boot 的 RabbitAutoConfiguration 会自动检测并设置到 RabbitTemplate 中
     */
    @Bean
    @ConditionalOnMissingBean
    fun messageConverter(): MessageConverter {
        log.info("初始化 RabbitMQ FastJson2 消息转换器")
        return FastJson2MessageConverter()
    }
}


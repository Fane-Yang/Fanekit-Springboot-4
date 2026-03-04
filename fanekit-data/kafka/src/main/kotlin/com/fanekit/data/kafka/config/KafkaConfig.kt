package com.fanekit.data.kafka.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ContainerProperties

@Configuration
@EnableKafka
class KafkaConfig(private val kafkaProperties: KafkaProperties) {

    @Bean
    @ConditionalOnMissingBean
    fun producerFactory(): ProducerFactory<String, String> =
        DefaultKafkaProducerFactory(kafkaProperties.buildProducerProperties())

    @Bean
    @ConditionalOnMissingBean
    fun kafkaTemplate(): KafkaTemplate<String, String> = KafkaTemplate(producerFactory())

    @Bean
    @ConditionalOnMissingBean
    fun consumerFactory(): ConsumerFactory<String, String> =
        DefaultKafkaConsumerFactory(kafkaProperties.buildConsumerProperties())

    /**
     * 标准监听器工厂（供 @KafkaListener 使用）
     */
    @Bean(name = ["kafkaListenerContainerFactory"]) // Spring 默认查找该名字
    @ConditionalOnMissingBean(name = ["kafkaListenerContainerFactory"])
    fun listenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.setConsumerFactory(consumerFactory())

        // 读取 spring.kafka.listener.* 配置
        val lp = kafkaProperties.listener
        // ack 模式
        factory.containerProperties.ackMode = lp.ackMode ?: ContainerProperties.AckMode.MANUAL_IMMEDIATE
        // 并发
        lp.concurrency?.let { factory.setConcurrency(it) }
        // 轮询超时
        lp.pollTimeout?.toMillis()?.let { factory.containerProperties.pollTimeout = it }
        // 单条/批量
        val isBatch = lp.type.name.equals("BATCH", ignoreCase = true)
        factory.setBatchListener(isBatch)
        // 空闲事件间隔
        lp.idleEventInterval?.toMillis()?.let { factory.containerProperties.idleEventInterval = it }

        return factory
    }
}

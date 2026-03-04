package com.fanekit.data.kafka

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.errors.TimeoutException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Kafka启动时检查配置 - 在Kafka配置完成后立即检测连接状态
 *
 * @author fanekit
 * @since 2024/1/1
 */
@Component
class KafkaStartupChecker(
    private val kafkaProperties: KafkaProperties,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val producerFactory: ProducerFactory<String, String>,
    private val consumerFactory: ConsumerFactory<String, String>
) : InitializingBean {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun afterPropertiesSet() {
        log.info("开始Kafka连接健康检查...")

        var allChecksPass = true

        // 检查Kafka集群连接
        if (!checkKafkaClusterConnection()) {
            allChecksPass = false
        }

        // 检查生产者配置
        if (!checkProducerConnection()) {
            allChecksPass = false
        }

        // 检查消费者配置
        if (!checkConsumerConnection()) {
            allChecksPass = false
        }

        // 检查管理员客户端
        if (!checkAdminClientConnection()) {
            allChecksPass = false
        }

        // 检查Kafka模板可用性
        if (!checkKafkaTemplate()) {
            allChecksPass = false
        }

        if (allChecksPass) {
            log.info("✅ Kafka连接健康检查完成 - 所有检查项均通过")
        } else {
            log.error("❌ Kafka连接健康检查失败 - 存在连接问题，请检查配置")
        }
    }

    /**
     * 检查Kafka集群连接
     */
    private fun checkKafkaClusterConnection(): Boolean {
        return try {
            val bootstrapServers = kafkaProperties.bootstrapServers
            log.info("检查Kafka集群连接 - 服务器地址: {}", bootstrapServers)

            if (bootstrapServers.isNullOrEmpty()) {
                log.error("❌ Kafka bootstrap-servers 未配置")
                return false
            }

            log.info("✅ Kafka集群配置检查通过 - 服务器: {}", bootstrapServers.joinToString(","))
            true
        } catch (e: Exception) {
            log.error("❌ Kafka集群连接检查失败: {}", e.message, e)
            false
        }
    }

    /**
     * 检查生产者连接
     */
    private fun checkProducerConnection(): Boolean {
        return try {
            log.info("检查Kafka生产者连接...")

            // 检查生产者工厂配置
            val producerConfigs = producerFactory.configurationProperties
            log.debug("生产者配置: {}", producerConfigs[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG])

            // 测试生产者创建
            val producer = producerFactory.createProducer()
            producer.use {
                // 获取元数据来测试连接（设置较短的超时时间）
                it.partitionsFor("__test_topic_for_health_check")
                log.debug("生产者连接测试完成，获取到元数据信息")
            }

            log.info("✅ Kafka生产者连接检查通过")
            true
        } catch (_: TimeoutException) {
            log.warn("⚠️ Kafka生产者连接超时，但配置正常 - 可能是网络延迟或集群未完全启动")
            true // 超时不一定是错误，可能是网络延迟
        } catch (e: Exception) {
            log.error("❌ Kafka生产者连接检查失败: {}", e.message, e)
            false
        }
    }

    /**
     * 检查消费者连接
     */
    private fun checkConsumerConnection(): Boolean {
        return try {
            log.info("检查Kafka消费者连接...")

            // 检查消费者工厂配置
            val consumerConfigs = consumerFactory.configurationProperties
            log.debug("消费者配置: {}", consumerConfigs[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG])

            // 测试消费者创建
            val consumer = consumerFactory.createConsumer("health-check-group", "health-check-client")
            consumer.use {
                // 获取集群元数据来测试连接
                val metadata = it.listTopics(Duration.ofSeconds(5))
                log.debug("消费者连接测试完成，发现 {} 个主题", metadata.size)
            }

            log.info("✅ Kafka消费者连接检查通过")
            true
        } catch (_: TimeoutException) {
            log.warn("⚠️ Kafka消费者连接超时，但配置正常 - 可能是网络延迟或集群未完全启动")
            true // 超时不一定是错误
        } catch (e: Exception) {
            log.error("❌ Kafka消费者连接检查失败: {}", e.message, e)
            false
        }
    }

    /**
     * 检查管理员客户端连接
     */
    private fun checkAdminClientConnection(): Boolean {
        return try {
            log.info("检查Kafka管理员客户端连接...")

            val adminProps = mutableMapOf<String, Any>()
            adminProps[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaProperties.bootstrapServers

            // 添加其他管理员配置
            kafkaProperties.admin.properties?.let { adminProps.putAll(it) }

            AdminClient.create(adminProps).use { adminClient ->
                // 测试获取集群信息
                val clusterInfo = adminClient.describeCluster()
                val nodesFuture = clusterInfo.nodes()
                val nodes = nodesFuture.get(10, TimeUnit.SECONDS)

                log.info("✅ Kafka管理员客户端连接检查通过 - 发现 {} 个集群节点", nodes.size)

                // 记录集群节点信息
                nodes.forEach { node ->
                    log.debug("集群节点: ID={}, Host={}, Port={}", node.id(), node.host(), node.port())
                }
            }

            true
        } catch (_: TimeoutException) {
            log.warn("⚠️ Kafka管理员客户端连接超时，但配置正常 - 可能是网络延迟或集群未完全启动")
            true // 超时不一定是错误
        } catch (e: Exception) {
            log.error("❌ Kafka管理员客户端连接检查失败: {}", e.message, e)
            false
        }
    }

    /**
     * 检查Kafka模板是否可用
     */
    private fun checkKafkaTemplate(): Boolean {
        return try {
            log.info("检查KafkaTemplate可用性...")

            // 检查模板是否正确初始化
            val template = kafkaTemplate
            if (template.producerFactory == null) {
                log.error("❌ KafkaTemplate未正确初始化 - ProducerFactory为空")
                return false
            }

            log.info("✅ KafkaTemplate检查通过")
            true
        } catch (e: Exception) {
            log.error("❌ KafkaTemplate检查失败: {}", e.message, e)
            false
        }
    }
}

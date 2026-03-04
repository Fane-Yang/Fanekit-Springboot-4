package com.fanekit.data.kafka.producer

import com.alibaba.fastjson2.JSON
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Service
class KafkaProducer(private val kafkaTemplate: KafkaTemplate<String, String>) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * 同步发送消息 - 使用自定义分区逻辑
     */
    fun sendMessageSync(topic: String, key: String, value: String, partitionCount: Int): SendResult<String, String> {
        if (partitionCount <= 0) {
            throw IllegalArgumentException("partitionCount must be greater than 0")
        }

        return try {
            // 自定义分区逻辑，这里通过 key 的 hash 值来选择分区，确保分区号合法
            val partition = abs(key.hashCode()) % partitionCount

            val producerRecord = ProducerRecord(
                topic,
                partition,
                key,
                value
            )

            // 发送消息并等待确认，失败时抛出异常
            val sendResult: SendResult<String, String> = kafkaTemplate.send(producerRecord).get()
            if (sendResult.recordMetadata == null) {
                throw RuntimeException("Message send failed - recordMetadata is null")
            }

            log.debug("消息发送成功 - 主题: {}, 分区: {}, 偏移量: {}",
                topic, sendResult.recordMetadata.partition(), sendResult.recordMetadata.offset())

            sendResult
        } catch (e: Exception) {
            log.error("同步发送消息失败 - 主题: {}, 键: {}, 错误: {}", topic, key, e.message, e)
            throw RuntimeException("Failed to send message to Kafka: ${e.message}", e)
        }
    }

    /**
     * 同步发送消息 - 不指定分区，由Kafka自动分配
     */
    fun sendMessageSync(topic: String, key: String, value: String): SendResult<String, String> {
        return try {
            val producerRecord = ProducerRecord(topic, key, value)

            val sendResult: SendResult<String, String> = kafkaTemplate.send(producerRecord).get()
            if (sendResult.recordMetadata == null) {
                throw RuntimeException("Message send failed - recordMetadata is null")
            }

            log.debug("消息发送成功 - 主题: {}, 分区: {}, 偏移量: {}",
                topic, sendResult.recordMetadata.partition(), sendResult.recordMetadata.offset())

            sendResult
        } catch (e: Exception) {
            log.error("同步发送消息失败 - 主题: {}, 键: {}, 错误: {}", topic, key, e.message, e)
            throw RuntimeException("Failed to send message to Kafka: ${e.message}", e)
        }
    }

    /**
     * 同步发送消息 - 指定具体分区
     */
    fun sendMessageSyncToPartition(topic: String, partition: Int, key: String, value: String): SendResult<String, String> {
        if (partition < 0) {
            throw IllegalArgumentException("partition must be non-negative")
        }

        return try {
            val producerRecord = ProducerRecord(topic, partition, key, value)

            val sendResult: SendResult<String, String> = kafkaTemplate.send(producerRecord).get()
            if (sendResult.recordMetadata == null) {
                throw RuntimeException("Message send failed - recordMetadata is null")
            }

            log.debug("消息发送成功 - 主题: {}, 指定分区: {}, 偏移量: {}",
                topic, sendResult.recordMetadata.partition(), sendResult.recordMetadata.offset())

            sendResult
        } catch (e: Exception) {
            log.error("同步发送消息到指定分区失败 - 主题: {}, 分区: {}, 键: {}, 错误: {}", topic, partition, key, e.message, e)
            throw RuntimeException("Failed to send message to Kafka partition $partition: ${e.message}", e)
        }
    }

    /**
     * 同步发送消息 - 支持超时设置
     */
    fun sendMessageSync(topic: String, key: String, value: String, timeoutMs: Long): SendResult<String, String> {
        return try {
            val producerRecord = ProducerRecord(topic, key, value)

            val sendResult: SendResult<String, String> = kafkaTemplate.send(producerRecord).get(timeoutMs, TimeUnit.MILLISECONDS)
            if (sendResult.recordMetadata == null) {
                throw RuntimeException("Message send failed - recordMetadata is null")
            }

            log.debug("消息发送成功 - 主题: {}, 分区: {}, 偏移量: {}, 超时: {}ms",
                topic, sendResult.recordMetadata.partition(), sendResult.recordMetadata.offset(), timeoutMs)

            sendResult
        } catch (e: Exception) {
            log.error("同步发送消息失败(超时{}ms) - 主题: {}, 键: {}, 错误: {}", timeoutMs, topic, key, e.message, e)
            throw RuntimeException("Failed to send message to Kafka within ${timeoutMs}ms: ${e.message}", e)
        }
    }

    /**
     * 异步发送消息 - 不指定分区，返回CompletableFuture
     */
    fun sendMessageAsync(topic: String, key: String, value: String): CompletableFuture<SendResult<String, String>> {
        return try {
            val producerRecord = ProducerRecord(topic, key, value)

            // 使用KafkaTemplate的send方法，它返回ListenableFuture，我们将其转换为CompletableFuture
            val future = kafkaTemplate.send(producerRecord)

            // 使用future.get()的方式在后台线程中处理
            CompletableFuture.supplyAsync {
                try {
                    val result = future.get()
                    if (result.recordMetadata == null) {
                        throw RuntimeException("Message send failed - recordMetadata is null")
                    }
                    log.debug("异步消息发送成功 - 主题: {}, 分区: {}, 偏移量: {}",
                        topic, result.recordMetadata.partition(), result.recordMetadata.offset())
                    result
                } catch (e: Exception) {
                    log.error("异步发送消息失败 - 主题: {}, 键: {}, 错误: {}", topic, key, e.message, e)
                    throw RuntimeException("Failed to send message to Kafka: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            log.error("异步发送消息启动失败 - 主题: {}, 键: {}, 错误: {}", topic, key, e.message, e)
            CompletableFuture.failedFuture(RuntimeException("Failed to start async message send: ${e.message}", e))
        }
    }

    /**
     * 异步发送消息 - 使用自定义分区逻辑
     */
    fun sendMessageAsync(topic: String, key: String, value: String, partitionCount: Int): CompletableFuture<SendResult<String, String>> {
        if (partitionCount <= 0) {
            return CompletableFuture.failedFuture(IllegalArgumentException("partitionCount must be greater than 0"))
        }

        return try {
            val partition = abs(key.hashCode()) % partitionCount
            val producerRecord = ProducerRecord(topic, partition, key, value)

            val future = kafkaTemplate.send(producerRecord)

            CompletableFuture.supplyAsync {
                try {
                    val result = future.get()
                    if (result.recordMetadata == null) {
                        throw RuntimeException("Message send failed - recordMetadata is null")
                    }
                    log.debug("异步消息发送成功(自定义分区) - 主题: {}, 分区: {}, 偏移量: {}",
                        topic, result.recordMetadata.partition(), result.recordMetadata.offset())
                    result
                } catch (e: Exception) {
                    log.error("异步发送消息失败(自定义分区) - 主题: {}, 键: {}, 错误: {}", topic, key, e.message, e)
                    throw RuntimeException("Failed to send message to Kafka: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            log.error("异步发送消息启动失败(自定义分区) - 主题: {}, 键: {}, 错误: {}", topic, key, e.message, e)
            CompletableFuture.failedFuture(RuntimeException("Failed to start async message send: ${e.message}", e))
        }
    }

    /**
     * 异步发送消息 - 指定具体分区
     */
    fun sendMessageAsyncToPartition(topic: String, partition: Int, key: String, value: String): CompletableFuture<SendResult<String, String>> {
        if (partition < 0) {
            return CompletableFuture.failedFuture(IllegalArgumentException("partition must be non-negative"))
        }

        return try {
            val producerRecord = ProducerRecord(topic, partition, key, value)

            val future = kafkaTemplate.send(producerRecord)

            CompletableFuture.supplyAsync {
                try {
                    val result = future.get()
                    if (result.recordMetadata == null) {
                        throw RuntimeException("Message send failed - recordMetadata is null")
                    }
                    log.debug("异步消息发送成功(指定分区) - 主题: {}, 分区: {}, 偏移量: {}",
                        topic, result.recordMetadata.partition(), result.recordMetadata.offset())
                    result
                } catch (e: Exception) {
                    log.error("异步发送消息失败(指定分区) - 主题: {}, 分区: {}, 键: {}, 错误: {}", topic, partition, key, e.message, e)
                    throw RuntimeException("Failed to send message to Kafka partition $partition: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            log.error("异步发送消息启动失败(指定分区) - 主题: {}, 分区: {}, 键: {}, 错误: {}", topic, partition, key, e.message, e)
            CompletableFuture.failedFuture(RuntimeException("Failed to start async message send: ${e.message}", e))
        }
    }

    /**
     * 发送对象消息 - 自动序列化为JSON
     */
    fun sendObjectMessage(topic: String, key: String, value: Any): SendResult<String, String> {
        return try {
            val jsonValue = JSON.toJSONString(value)
            sendMessageSync(topic, key, jsonValue)
        } catch (e: Exception) {
            log.error("发送对象消息失败 - 主题: {}, 键: {}, 对象类型: {}, 错误: {}",
                topic, key, value.javaClass.simpleName, e.message, e)
            throw RuntimeException("Failed to send object message to Kafka: ${e.message}", e)
        }
    }

    /**
     * 异步发送对象消息 - 自动序列化为JSON
     */
    fun sendObjectMessageAsync(topic: String, key: String, value: Any): CompletableFuture<SendResult<String, String>> {
        return try {
            val jsonValue = JSON.toJSONString(value)
            sendMessageAsync(topic, key, jsonValue)
        } catch (e: Exception) {
            log.error("异步发送对象消息失败 - 主题: {}, 键: {}, 对象类型: {}, 错误: {}",
                topic, key, value.javaClass.simpleName, e.message, e)
            CompletableFuture.failedFuture(RuntimeException("Failed to send object message to Kafka: ${e.message}", e))
        }
    }

    /**
     * 批量发送消息 - 同步
     */
    fun sendBatchMessagesSync(topic: String, messages: List<Pair<String, String>>): List<SendResult<String, String>> {
        if (messages.isEmpty()) {
            throw IllegalArgumentException("messages list cannot be empty")
        }

        val results = mutableListOf<SendResult<String, String>>()
        var successCount = 0

        for ((key, value) in messages) {
            try {
                val result = sendMessageSync(topic, key, value)
                results.add(result)
                successCount++
            } catch (e: Exception) {
                log.error("批量发送消息中的单条消息失败 - 主题: {}, 键: {}, 错误: {}", topic, key, e.message, e)
                throw RuntimeException("Batch message send failed at message with key '$key': ${e.message}", e)
            }
        }

        log.info("批量消息发送完成 - 主题: {}, 总数: {}, 成功: {}",
            topic, messages.size, successCount)

        return results
    }

    /**
     * 发送简单消息 - 只需要主题和消息内容，key 留空
     */
    fun sendSimpleMessage(topic: String, value: String): SendResult<String, String> {
        return sendMessageSync(topic, "", value)
    }

    /**
     * 异步发送简单消息 - 只需要主题和消息内容，key 留空
     */
    fun sendSimpleMessageAsync(topic: String, value: String): CompletableFuture<SendResult<String, String>> {
        return sendMessageAsync(topic, "", value)
    }
}

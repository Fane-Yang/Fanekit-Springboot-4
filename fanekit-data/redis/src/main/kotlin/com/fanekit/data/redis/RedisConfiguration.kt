package com.fanekit.data.redis

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.JSONWriter
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.nio.charset.StandardCharsets

/**
 * Redis配置类 - 使用Jedis连接池和FastJSON2作为序列化器
 * 支持自动适配单节点和集群模式
 *
 * @author fanekit
 * @since 2024/1/1
 */
@Configuration
class RedisConfiguration() {

    private val log = LoggerFactory.getLogger(RedisConfiguration::class.java)

//    /**
//     * 根据配置自动创建Redis连接工厂（单节点或集群）
//     */

    /**
     * 配置RedisTemplate，使用FastJSON2序列化
     */
    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory

        // 使用String序列化器序列化key
        val stringSerializer = StringRedisSerializer()
        template.keySerializer = stringSerializer
        template.hashKeySerializer = stringSerializer

        // 使用FastJSON2序列化器序列化value
        val fastjson2Serializer = FastJson2RedisSerializer(Any::class.java)
        template.valueSerializer = fastjson2Serializer
        template.hashValueSerializer = fastjson2Serializer

        template.afterPropertiesSet()
        return template
    }

    /**
     * FastJSON2 Redis序列化器
     */
    class FastJson2RedisSerializer<T : Any>(private val clazz: Class<T>) : RedisSerializer<T> {

        override fun serialize(t: T?): ByteArray {
            return if (t == null) {
                byteArrayOf()
            } else {
                JSON.toJSONString(t, JSONWriter.Feature.WriteClassName).toByteArray(StandardCharsets.UTF_8)
            }
        }

        override fun deserialize(bytes: ByteArray?): T? {
            return if (bytes == null || bytes.isEmpty()) {
                null
            } else {
                val str = String(bytes, StandardCharsets.UTF_8)
                JSON.parseObject(str, clazz, JSONReader.Feature.SupportAutoType)
            }
        }
    }
}

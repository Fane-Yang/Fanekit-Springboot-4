package com.fanekit.data.redis

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.data.redis.connection.RedisClusterNode
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

/**
 * Redis启动时检查配置 - 在Redis配置完成后立即检测
 *
 * @author fanekit
 * @since 2024/1/1
 */
@Component
class RedisStartupChecker(
    private val redisTemplate: RedisTemplate<String, Any>?,
    private val redisConnectionFactory: RedisConnectionFactory?,
    private val applicationContext: ApplicationContext
) : InitializingBean {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        // 在Bean属性设置完成后立即执行Redis检查
        checkRedisConfiguration()
    }

    private fun checkRedisConfiguration() {
        try {
            log.info("=== Redis配置检查信息 ===")

            // 检查RedisTemplate
            if (redisTemplate != null) {
                log.info("✓ RedisTemplate初始化成功: {}", redisTemplate.javaClass.simpleName)
                checkRedisTemplateConfiguration()
            } else {
                log.error("✗ RedisTemplate初始化失败")
                return
            }

            // 检查Redis连接工厂
            if (redisConnectionFactory != null) {
                log.info("✓ RedisConnectionFactory初始化成功: {}", redisConnectionFactory.javaClass.simpleName)
                checkRedisConnection()
            } else {
                log.error("✗ RedisConnectionFactory初始化失败")
                return
            }

            // 检查Redis连接池配置
            checkRedisConnectionPool()

            log.info("=== Redis配置检查完成 ===")
        } catch (e: Exception) {
            log.error("=== Redis配置检查失败 ===")
            log.error("错误信息: {}", e.message)
            log.error("请检查以下配置项:")
            log.error("1. Redis服务器是否启动")
            log.error("2. Redis连接配置是否正确")
            log.error("3. Redis相关依赖是否完整")
            log.error("4. 网络连接是否正常")
            log.warn("Redis检查失败，但应用将继续启动")
        }
    }

    private fun checkRedisTemplateConfiguration() {
        try {
            val keySerializer = redisTemplate?.keySerializer
            val valueSerializer = redisTemplate?.valueSerializer

            log.info("  Key序列化器: {}", keySerializer?.javaClass?.simpleName ?: "未配置")
            log.info("  Value序列化器: {}", valueSerializer?.javaClass?.simpleName ?: "未配置")

            // 检查序列化器是否使用FastJSON2
            if (valueSerializer?.javaClass?.simpleName?.contains("FastJson2") == true) {
                log.info("  ✓ 使用FastJSON2序列化器")
            } else {
                log.warn("  ⚠ 未使用FastJSON2序列化器")
            }
        } catch (e: Exception) {
            log.warn("RedisTemplate配置检查失败: {}", e.message)
        }
    }

    private fun checkRedisConnection() {
        try {
            val connection = redisConnectionFactory?.connection

            if (connection != null) {
                val info = connection.serverCommands().info()
                if (!info.isNullOrEmpty()) {
                    log.info("  ✓ Redis连接测试成功")
                    // 提取Redis服务器信息
                    val redisVersion = info.getProperty("redis_version") ?: "未知"
                    val redisMode = info.getProperty("redis_mode") ?: "未知"
                    log.info("  Redis版本: {}", redisVersion)
                    log.info("  Redis模式: {}", redisMode)
                    log.info("  Redis详细信息: {}", info.toString())
                    if (redisVersion == "未知" || redisMode == "未知") {
                        redisConnectionFactory.apply {
                            val clusterSize = clusterConnection.clusterGetClusterInfo().clusterSize ?: 0
                            if (clusterSize > 0) {
                                val nodes = clusterConnection.clusterGetNodes()
                                nodes.forEach { node ->
                                    log.info("  Redis节点列表: {}", node)
                                    log.info("  Redis节点版本: {}", info.getProperty("${node}.redis_version") ?: "未知")
                                    log.info("  Redis节点模式: {}", info.getProperty("${node}.redis_mode") ?: "未知")
                                }
                            }
                        }
                    }
                } else {
                    log.warn("  ⚠ Redis连接测试成功但无法获取服务器信息")
                }
                connection.close()
            } else {
                log.error("  ✗ 无法创建Redis连接")
            }
        } catch (e: Exception) {
            log.error("  ✗ Redis连接测试失败: {}", e.message)
        }
    }

    private fun checkRedisConnectionPool() {
        try {
            // 检查连接池类型
            val connectionFactoryClass = redisConnectionFactory?.javaClass?.simpleName
            log.info("  连接工厂类型: {}", connectionFactoryClass)

            when {
                connectionFactoryClass?.contains("Lettuce") == true -> {
                    log.info("  ✓ 使用Lettuce连接池")
                }
                connectionFactoryClass?.contains("Jedis") == true -> {
                    log.info("  ✓ 使用Jedis连接池")
                }
                else -> {
                    log.info("  使用其他连接池: {}", connectionFactoryClass)
                }
            }
        } catch (e: Exception) {
            log.warn("Redis连接池配置检查失败: {}", e.message)
        }
    }
}


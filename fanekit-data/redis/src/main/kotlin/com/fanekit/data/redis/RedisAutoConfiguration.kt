package com.fanekit.data.redis

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate

/**
 * Redis自动配置类
 *
 * @author fanekit
 * @since 2024/1/1
 */
@Configuration
@ConditionalOnClass(RedisTemplate::class)
@Import(RedisConfiguration::class)
class RedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun redisUtils(redisTemplate: RedisTemplate<String, Any>): RedisUtils {
        return RedisUtils(redisTemplate)
    }

    @Bean
    @ConditionalOnMissingBean
    fun redisStartupChecker(
        redisTemplate: RedisTemplate<String, Any>,
        redisConnectionFactory: RedisConnectionFactory,
        applicationContext: ApplicationContext
    ): RedisStartupChecker {
        return RedisStartupChecker(redisTemplate, redisConnectionFactory, applicationContext)
    }
}

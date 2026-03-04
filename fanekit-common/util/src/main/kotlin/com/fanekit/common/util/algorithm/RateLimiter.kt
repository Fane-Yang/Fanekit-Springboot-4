package com.fanekit.common.util.algorithm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 令牌桶算法
 * 主要用于并发限制，支持HTTP接口流量控制等
 * @property tokens 初始令牌数量
 * @property maxTokens 最大令牌数量
 * @property refillInterval 补充令牌间隔时间，单位为毫秒
 * @property addCounts 每间隔时间单位补充的令牌数量
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2025/02/18
 * @since 1.0.0
 * @version 1.0.0
 */
class RateLimiter(// 初始令牌数量
    private var tokens: Int = 100,
    private val maxTokens: Int = 100,
    private val refillInterval: Long = 1000L, // 补充令牌间隔时间 毫秒
    private val addCounts: Int = 10 // 每间隔时间单位补充的令牌数量
) {
    private val mutex = Mutex()
    init {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                delay(refillInterval)
                mutex.withLock {
                    tokens = maxTokens.coerceAtMost(tokens + addCounts) // 每秒补充 addCounts 个令牌
                }
            }
        }
    }

    suspend fun tryAcquire(): Boolean {
        return mutex.withLock {
            if (tokens > 0) {
                tokens--
                true
            } else {
                false
            }
        }
    }
}
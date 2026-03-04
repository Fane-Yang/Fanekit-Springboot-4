package com.fanekit.data.redis

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * Redis工具类 - 提供常用的Redis操作方法
 *
 * @author fanekit
 * @since 2024/1/1
 */
@Component
class RedisUtils(private val redisTemplate: RedisTemplate<String, Any>) {

    // =============================常用操作=============================

    /**
     * 指定缓存失效时间
     * @param key 键
     * @param time 时间(秒)
     */
    fun expire(key: String, time: Long): Boolean {
        return try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS)
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 根据key 获取过期时间
     * @param key 键 不能为null
     * @return 时间(秒) 返回0代表为永久有效
     */
    fun getExpire(key: String): Long {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS)
    }

    /**
     * 判断key是否存在
     * @param key 键
     * @return true 存在 false不存在
     */
    fun hasKey(key: String): Boolean {
        return try {
            redisTemplate.hasKey(key)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 删除缓存
     * @param key 可以传一个值 或多个
     */
    fun del(vararg key: String) {
        if (key.isNotEmpty()) {
            if (key.size == 1) {
                redisTemplate.delete(key[0])
            } else {
                redisTemplate.delete(key.toList())
            }
        }
    }

    // ============================String=============================

    /**
     * 普通缓存获取
     * @param key 键
     * @return 值
     */
    fun get(key: String): Any? {
        return if (key.isEmpty()) null else redisTemplate.opsForValue().get(key)
    }

    /**
     * 普通缓存放入
     * @param key 键
     * @param value 值
     * @return true成功 false失败
     */
    fun set(key: String, value: Any): Boolean {
        return try {
            redisTemplate.opsForValue().set(key, value)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 普通缓存放入并设置时间
     * @param key 键
     * @param value 值
     * @param time 时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    fun set(key: String, value: Any, time: Long): Boolean {
        return try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS)
            } else {
                set(key, value)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 递增
     * @param key 键
     * @param delta 要增加几(大于0)
     */
    fun incr(key: String, delta: Long): Long {
        require(delta >= 0) { "递增因子必须大于0" }
        return redisTemplate.opsForValue().increment(key, delta)!!
    }

    /**
     * 递减
     * @param key 键
     * @param delta 要减少几(小于0)
     */
    fun decr(key: String, delta: Long): Long {
        require(delta >= 0) { "递减因子必须大于0" }
        return redisTemplate.opsForValue().increment(key, -delta)!!
    }

    // ================================Hash=================================

    /**
     * HashGet
     * @param key 键 不能为null
     * @param item 项 不能为null
     */
    fun hget(key: String, item: String): Any? {
        return redisTemplate.opsForHash<Any, Any>().get(key, item)
    }

    /**
     * 获取hashKey对应的所有键值
     * @param key 键
     * @return 对应的多个键值
     */
    fun hmget(key: String): Map<Any, Any> {
        return redisTemplate.opsForHash<Any, Any>().entries(key)
    }

    /**
     * HashSet
     * @param key 键
     * @param map 对应多个键值
     */
    fun hmset(key: String, map: Map<String, Any>): Boolean {
        return try {
            redisTemplate.opsForHash<Any, Any>().putAll(key, map)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * HashSet 并设置时间
     * @param key 键
     * @param map 对应多个键值
     * @param time 时间(秒)
     * @return true成功 false失败
     */
    fun hmset(key: String, map: Map<String, Any>, time: Long): Boolean {
        return try {
            redisTemplate.opsForHash<Any, Any>().putAll(key, map)
            if (time > 0) {
                expire(key, time)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     * @param key 键
     * @param item 项
     * @param value 值
     * @return true 成功 false失败
     */
    fun hset(key: String, item: String, value: Any): Boolean {
        return try {
            redisTemplate.opsForHash<Any, Any>().put(key, item, value)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     * @param key 键
     * @param item 项
     * @param value 值
     * @param time 时间(秒) 注意:如果已存在的hash表有时间,这里将会替换原有的时间
     * @return true 成功 false失败
     */
    fun hset(key: String, item: String, value: Any, time: Long): Boolean {
        return try {
            redisTemplate.opsForHash<Any, Any>().put(key, item, value)
            if (time > 0) {
                expire(key, time)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 删除hash表中的值
     * @param key 键 不能为null
     * @param item 项 可以使多个 不能为null
     */
    fun hdel(key: String, vararg item: Any) {
        redisTemplate.opsForHash<Any, Any>().delete(key, *item)
    }

    /**
     * 判断hash表中是否有该项的值
     * @param key 键 不能为null
     * @param item 项 不能为null
     * @return true 存在 false不存在
     */
    fun hHasKey(key: String, item: String): Boolean {
        return redisTemplate.opsForHash<Any, Any>().hasKey(key, item)
    }

    // ============================Set=============================

    /**
     * 根据key获取Set中的所有值
     * @param key 键
     */
    fun sGet(key: String): Set<Any>? {
        return try {
            redisTemplate.opsForSet().members(key)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 根据value从一个set中查询,是否存在
     * @param key 键
     * @param value 值
     * @return true 存在 false不存在
     */
    fun sHasKey(key: String, value: Any): Boolean {
        return try {
            redisTemplate.opsForSet().isMember(key, value) ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 将数据放入set缓存
     * @param key 键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    fun sSet(key: String, vararg values: Any): Long {
        return try {
            redisTemplate.opsForSet().add(key, *values) ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * 将set数据放入缓存
     * @param key 键
     * @param time 时间(秒)
     * @param values 值 可以是多个
     * @return 成功个数
     */
    fun sSetAndTime(key: String, time: Long, vararg values: Any): Long {
        return try {
            val count = redisTemplate.opsForSet().add(key, *values) ?: 0
            if (time > 0) expire(key, time)
            count
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * 获取set缓存的长度
     * @param key 键
     */
    fun sGetSetSize(key: String): Long {
        return try {
            redisTemplate.opsForSet().size(key) ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * 移除值为value的
     * @param key 键
     * @param values 值 可以是多个
     * @return 移除的个数
     */
    fun setRemove(key: String, vararg values: Any): Long {
        return try {
            redisTemplate.opsForSet().remove(key, *values) ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    // ===============================List=================================

    /**
     * 获取list缓存的内容
     * @param key 键
     * @param start 开始
     * @param end 结束 0 到 -1代表所有值
     */
    fun lGet(key: String, start: Long, end: Long): List<Any>? {
        return try {
            redisTemplate.opsForList().range(key, start, end)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取list缓存的长度
     * @param key 键
     */
    fun lGetListSize(key: String): Long {
        return try {
            redisTemplate.opsForList().size(key) ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * 通过索引 获取list中的值
     * @param key 键
     * @param index 索引 index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
     */
    fun lGetIndex(key: String, index: Long): Any? {
        return try {
            redisTemplate.opsForList().index(key, index)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 将list放入缓存
     * @param key 键
     * @param value 值
     * @param time 时间(秒)
     */
    fun lSet(key: String, value: Any, time: Long): Boolean {
        return try {
            redisTemplate.opsForList().rightPush(key, value)
            if (time > 0) expire(key, time)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 将list放入缓存
     * @param key 键
     * @param value 值
     */
    fun lSet(key: String, value: Any): Boolean {
        return try {
            redisTemplate.opsForList().rightPush(key, value)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 将list放入缓存
     * @param key 键
     * @param value 值
     * @param time 时间(秒)
     */
    fun lSet(key: String, value: List<Any>, time: Long): Boolean {
        return try {
            redisTemplate.opsForList().rightPushAll(key, value)
            if (time > 0) expire(key, time)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 将list放入缓存
     * @param key 键
     * @param value 值
     */
    fun lSet(key: String, value: List<Any>): Boolean {
        return try {
            redisTemplate.opsForList().rightPushAll(key, value)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 根据索引修改list中的某条数据
     * @param key 键
     * @param index 索引
     * @param value 值
     */
    fun lUpdateIndex(key: String, index: Long, value: Any): Boolean {
        return try {
            redisTemplate.opsForList().set(key, index, value)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 移除N个值为value
     * @param key 键
     * @param count 移除多少个
     * @param value 值
     * @return 移除的个数
     */
    fun lRemove(key: String, count: Long, value: Any): Long {
        return try {
            redisTemplate.opsForList().remove(key, count, value) ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
}

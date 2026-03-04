package com.fanekit.common.util.algorithm.snowflake

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.atomic.AtomicLong

/**
 * 雪花算法，用于生成唯一ID
 * 时间戳 默认为 2025年2月1日
 * 默认每毫秒可以生成 16384 个ID
 * 可用时间的计算方式为
 * 时间戳位数 = 64 - 1(符号位) - 1(机器ID位) - 1(数据中心位) - 14(序列号位) = 47位
 * 则雪花算法能够使用的时间为 2^时间戳位数 毫秒
 * 默认参数下可从2025年2月1日起使用 4467 年
 *
 * @param machineId 机器ID,默认为0
 * @param dataCenterId 数据中心ID，默认为0
 * @param maxBackoffTime 允许回拨的最大时间，单位为毫秒,超出此时间10倍以内可以缓慢修复，否则会抛出异常
 * @param epoch 自定义纪元时间戳 ，默认2025年2月1日 1738377600000L
 * @param machineIdBits 机器ID的位数，默认1，即机器ID可用范围为0-1
 * @param dataCenterIdBits 数据中心位数，默认1，即数据中心ID可用范围为0-1
 * @param sequenceBits 序列号的位数，默认14位 16384 个 (2^14-1=16383, 即0->16383)
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2025/02/02
 * @since 1.0.0
 * @version 1.0.0
 */
open class Snowflake(
    private val machineId: Long = 0L, // 机器ID 默认值，可以根据实际情况修改
    private val dataCenterId: Long = 0L, // 数据中心ID 默认值，可以根据实际情况修改
    private val maxBackoffTime: Long = 60000L,  // 允许回拨的最大时间，单位为毫秒
    private val epoch: Long = 1738377600000L, // 自定义纪元时间戳 默认2025年2月1日
    private val machineIdBits: Int = 1, // 机器ID的位数，默认1位 (2^1-1=1, 即0-1)
    private val dataCenterIdBits: Int = 1, // 数据中心ID的位数，默认1位 (2^1-1=1, 即0-1)
    private val sequenceBits: Int = 14 // 序列号的位数，默认14位 16384 个 (2^14-1=16383, 即0->16383)
) {
    private val log = LoggerFactory.getLogger(Snowflake::class.java)

    private var maxMachineId = (1L shl machineIdBits) - 1
    private var maxDataCenterId = (1L shl dataCenterIdBits) - 1
    private var maxSequence = (1L shl sequenceBits) - 1

    private val sequence = AtomicLong(0) // 当前序列号

    private var lastTimestamp = -1L

    /**
     * 初始化雪花算法
     */
    init {
        if (machineId > maxMachineId || machineId < 0) throw IllegalArgumentException("Machine ID exceeds the limit")
        if (dataCenterId > maxDataCenterId || dataCenterId < 0) throw IllegalArgumentException("Data Center ID exceeds the limit")
    }

    // 生成唯一ID
    @Synchronized // 简单的方法加锁，解决并发冲突问题，但是性能可能有影响，每秒2000-5000个ID时无影响。
    fun generateId(): Long {
        var timestamp = System.currentTimeMillis() - epoch // 获取当前时间戳

        // 发生了时钟回拨
        if (timestamp < lastTimestamp) {
            // 如果回拨时间超出容忍范围，跳过等待，直接加1毫秒
            val backoffTime = lastTimestamp - timestamp
            // 判断超出10倍则报错
            if (backoffTime > maxBackoffTime) {
                throw IllegalStateException("Backoff time exceeds the limit")
            }
            runBlocking {
                // 未超出最大允许的时钟回拨时间，则降低速率缓慢修复，每次生成ID让系统多花费1毫秒,直到系统时间追赶上上次ID时间
                delay(1)
            }
            timestamp = lastTimestamp
        }

        if (timestamp == lastTimestamp) {
            // 同一毫秒内，增加序列号
            val nextSequence = sequence.incrementAndGet()
            if (nextSequence > maxSequence) {
                // 如果序列号溢出，直接加1毫秒，跳到下一毫秒
                timestamp = lastTimestamp + 1
                sequence.set(0) // 重置序列号
            }
        } else {
            // 不同毫秒，重置序列号
            sequence.set(0)
        }

        lastTimestamp = timestamp

        // 在 Kotlin 中，shl 是一个左移位运算符，用于将一个整数的二进制表示向左移动指定的位数。它与 Java 中的 << 运算符功能相同
        return (timestamp shl (machineIdBits + dataCenterIdBits + sequenceBits)) or
                (dataCenterId shl (machineIdBits + sequenceBits)) or
                (machineId shl sequenceBits) or
                sequence.get()
    }

    // 时钟回拨等待
    private suspend fun waitForNextMillis(lastTimestamp: Long): Long {
        var timestamp = System.currentTimeMillis() - epoch
        while (timestamp <= lastTimestamp) {
            // 每次修复回拨时间10MS
            delay(1)
            timestamp = System.currentTimeMillis() - epoch
        }
        return timestamp
    }


    /**
     * 从雪花算法生成的ID中提取时间戳（毫秒）
     */
    fun extractTimestamp(snowflakeId: Long): Long {
        val timestampShift = machineIdBits + dataCenterIdBits + sequenceBits
        return (snowflakeId shr timestampShift) + epoch
    }

    /**
     * 从雪花算法生成的ID中提取时间戳（相对于epoch的毫秒数）
     */
    fun extractRelativeTimestamp(snowflakeId: Long): Long {
        val timestampShift = machineIdBits + dataCenterIdBits + sequenceBits
        return snowflakeId shr timestampShift
    }

    /**
     * 从雪花算法生成的ID中提取LocalDateTime对象
     */
    fun extractDateTime(snowflakeId: Long): LocalDateTime {
        val timestamp = extractTimestamp(snowflakeId)
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    }

    /**
     * 从雪花算法生成的ID中提取Date对象
     */
    fun extractDate(snowflakeId: Long): Date {
        val timestamp = extractTimestamp(snowflakeId)
        return Date(timestamp)
    }

    /**
     * 从雪花算法生成的ID中提取Instant对象
     */
    fun extractInstant(snowflakeId: Long): Instant {
        val timestamp = extractTimestamp(snowflakeId)
        return Instant.ofEpochMilli(timestamp)
    }

    /**
     * 从雪花算法生成的ID中提取数据中心ID
     */
    fun extractDataCenterId(snowflakeId: Long): Long {
        val dataCenterIdShift = machineIdBits + sequenceBits
        val dataCenterIdMask = (1L shl dataCenterIdBits) - 1
        return (snowflakeId shr dataCenterIdShift) and dataCenterIdMask
    }

    /**
     * 从雪花算法生成的ID中提取机器ID
     */
    fun extractMachineId(snowflakeId: Long): Long {
        val machineIdShift = sequenceBits
        val machineIdMask = (1L shl machineIdBits) - 1
        return (snowflakeId shr machineIdShift) and machineIdMask
    }

    /**
     * 从雪花算法生成的ID中提取序列号
     */
    fun extractSequence(snowflakeId: Long): Long {
        val sequenceMask = (1L shl sequenceBits) - 1
        return snowflakeId and sequenceMask
    }

    /**
     * 从雪花算法ID中提取各个组成部分
     */
    fun parseSnowflakeId(snowflakeId: Long): SnowflakeIdInfo {
        return SnowflakeIdInfo(
            timestamp = extractTimestamp(snowflakeId),
            dataCenterId = extractDataCenterId(snowflakeId),
            machineId = extractMachineId(snowflakeId),
            sequence = extractSequence(snowflakeId)
        )
    }


    /**
     * 获取雪花算法ID的详细信息（包含时间对象）
     */
    fun getDetailedInfo(snowflakeId: Long): com.fanekit.common.util.algorithm.snowflake.DetailedSnowflakeInfo {
        return com.fanekit.common.util.algorithm.snowflake.DetailedSnowflakeInfo(
            snowflakeId = snowflakeId,
            timestamp = extractTimestamp(snowflakeId),
            relativeTimestamp = extractRelativeTimestamp(snowflakeId),
            dateTime = extractDateTime(snowflakeId),
            date = extractDate(snowflakeId),
            instant = extractInstant(snowflakeId),
            dataCenterId = extractDataCenterId(snowflakeId),
            machineId = extractMachineId(snowflakeId),
            sequence = extractSequence(snowflakeId)
        )
    }

}






//fun main() {
//    val snowflake = Snowflake()
//    val id = snowflake.generateId()
//
//    println("生成的ID: $id")
//
//    // 单独提取各部分
//    println("时间戳: ${snowflake.extractTimestamp(id)}")
//    println("数据中心ID: ${snowflake.extractDataCenterId(id)}")
//    println("机器ID: ${snowflake.extractMachineId(id)}")
//    println("序列号: ${snowflake.extractSequence(id)}")
//
//    // 提取时间对象
//    println("LocalDateTime: ${snowflake.extractDateTime(id)}")
//    println("Date: ${snowflake.extractDate(id)}")
//    println("Instant: ${snowflake.extractInstant(id)}")
//
//    // 获取完整信息
//    println("详细信息: ${snowflake.getDetailedInfo(id)}")
//}
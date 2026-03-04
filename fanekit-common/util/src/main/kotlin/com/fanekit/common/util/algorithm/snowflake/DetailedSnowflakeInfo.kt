package com.fanekit.common.util.algorithm.snowflake

import java.time.Instant
import java.time.LocalDateTime
import java.util.Date

/**
 * 详细的雪花算法ID信息
 * 包含了雪花ID的各个组成部分以及转换后的时间信息。
 * 适用于需要对雪花ID进行详细解析和时间转换的场景。
 * @property snowflakeId 原始雪花ID
 * @property timestamp 完整时间戳（毫秒）
 * @property relativeTimestamp 相对于epoch的时间戳，从起始时间到现在的毫秒数
 * @property dateTime LocalDateTime对象，表示本地时间
 * @property date Date对象，表示日期和时间
 * @property instant Instant对象，表示UTC时间线上的时间点，与时区无关
 * @property dataCenterId 数据中心ID
 * @property machineId 机器ID
 * @property sequence 序列号
 * @author FaneKit  // DO NOT REPLACE
 * @date 2025/07/17
 * @since 1.0.0
 * @version 1.0.0
 */
data class DetailedSnowflakeInfo(
    val snowflakeId: Long,           // 原始ID
    val timestamp: Long,             // 完整时间戳（毫秒）
    val relativeTimestamp: Long,     // 相对于epoch的时间戳，从起始时间到现在的毫秒数
    val dateTime: LocalDateTime,     // LocalDateTime对象
    val date: Date,                  // Date对象
    val instant: Instant,            // Instant对象，UTC时间线上的时间点，与时区无关
    val dataCenterId: Long,          // 数据中心ID
    val machineId: Long,             // 机器ID
    val sequence: Long               // 序列号
)
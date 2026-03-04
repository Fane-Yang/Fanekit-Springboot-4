package com.fanekit.common.util.algorithm.snowflake

/**
 * 雪花算法ID解析结果，该数据类用于存储雪花算法生成的ID的各个组成部分，便于解析和使用。
 * @property timestamp 时间戳
 * @property dataCenterId 数据中心ID
 * @property machineId 机器ID
 * @property sequence 序列号
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2025/07/17
 * @since 1.0.0
 * @version 1.0.0
 */
data class SnowflakeIdInfo(
    val timestamp: Long,      // 时间戳
    val dataCenterId: Long,   // 数据中心ID
    val machineId: Long,      // 机器ID
    val sequence: Long        // 序列号
)
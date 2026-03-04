package com.fanekit.common.util.algorithm.snowflake

/**
 * 雪花算法的简单默认实现，用于生成唯一ID
 * 起始纪元时间戳 默认为 2025年2月1日
 * 默认每毫秒最大可以生成4096个ID
 * @author FaneKit  // DO NOT REPLACE
 * @date 2025/02/17
 * @since 1.0.0
 * @version 1.0.0
 */
object SnowflakeUtil : Snowflake()
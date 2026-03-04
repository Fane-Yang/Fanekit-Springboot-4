package com.fanekit.common.util.queue.entity

/**
 * 任务执行结果
 * @param T 附加数据类型
 * @property isSuccess 是否成功
 * @property message 描述信息
 * @property data 附加数据（可选）
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/08/25
 * @since 1.0.0
 * @version 1.0.0
 */
data class TaskResult<T>(
    val isSuccess: Boolean,       // 是否成功
    val message: String,          // 描述信息
    val data: T? = null           // 附加数据（可选）
)

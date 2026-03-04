package com.fanekit.common.util.queue.entity

import kotlinx.coroutines.CompletableDeferred

/**
 * 单线程队列任务实体类
 * 用于存储任务数据和对应的 [CompletableDeferred] 对象，
 * 以便在任务完成时返回结果。
 * @param T 任务数据类型
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/08/25
 * @since 1.0.0
 * @version 1.0.0
 */
data class SingleThreadQueueTask<T>(val data: T, val deferred: CompletableDeferred<Result<T>>)
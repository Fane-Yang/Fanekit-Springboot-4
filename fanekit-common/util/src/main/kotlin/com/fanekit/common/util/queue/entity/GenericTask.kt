package com.fanekit.common.util.queue.entity

import kotlinx.coroutines.CompletableDeferred

/**
 * 通用任务实体类，包含调用者、逻辑处理函数和结果的 CompletableDeferred
 * @param T 调用者类型
 * @param R 任务结果类型
 * @property caller 调用者实例
 * @property logic 任务逻辑处理函数，返回 TaskResult<R>
 * @property deferred 用于异步获取任务结果的 CompletableDeferred
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/08/25
 * @since 1.0.0
 * @version 1.0.0
 */
data class GenericTask<T, R>(
    val caller: T,
    val logic: suspend (T) -> TaskResult<R>, // 逻辑返回 TaskResult
    val deferred: CompletableDeferred<TaskResult<R>> // 明确泛型为 TaskResult<R>
)
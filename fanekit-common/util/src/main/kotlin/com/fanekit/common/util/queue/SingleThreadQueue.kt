package com.fanekit.common.util.queue

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import com.fanekit.common.util.queue.entity.SingleThreadQueueTask

/**
 * 单线程任务队列
 * 用于确保任务按顺序处理，避免并发问题。
 *
 * 使用协程实现，不会阻塞主线程。
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/08/26
 * @since 1.0.0
 * @version 1.0.0
 */
open class SingleThreadQueue<T>(
    private val scope: CoroutineScope,
    private val processor: suspend (T) -> Result<T>) {

    private val singleThreadQueueTaskQueue = Channel<SingleThreadQueueTask<T>>(Channel.Factory.UNLIMITED) // 队列
    private val processingSet = mutableSetOf<T>() // 记录正在处理的任务，避免重复


    init {
        // 启动单线程消费者
        scope.launch {
            for (task in singleThreadQueueTaskQueue) {
                if (processingSet.contains(task.data)) {
                    task.deferred.complete(Result.failure(Exception("Duplicate task detected")))
                    continue
                }
                try {
                    processingSet.add(task.data)
                    val result = processor(task.data)
                    task.deferred.complete(result)
                } catch (e: Exception) {
                    task.deferred.complete(Result.failure(e))
                } finally {
                    processingSet.remove(task.data)
                }
            }
        }
    }

    // 提交任务
    suspend fun submit(taskData: T): Result<T> {
        val deferred = CompletableDeferred<Result<T>>()
        val singleThreadQueueTask = SingleThreadQueueTask(taskData, deferred)
        singleThreadQueueTaskQueue.send(singleThreadQueueTask)
        return deferred.await()
    }
}
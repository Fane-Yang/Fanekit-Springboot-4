package com.fanekit.common.util.thread

import kotlinx.coroutines.*

/**
 * 线程管理类
 * 提供协程相关的线程管理方法，支持在不同线程调度器上运行任务。
 * 确保为单例模式，避免多次创建实例。
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/06/18
 * @since 1.0.0
 * @version 1.0.0
 */
object ThreadManager {

//    // IO 调度器（适合 IO 密集型任务）
//    private val ioDispatcher = Dispatchers.IO
//
//    // CPU 密集型任务调度器
//    private val defaultDispatcher = Dispatchers.Default

    /**
     * 调用协程的runBlocking方法来执行代码，会阻塞线程
     */
    fun runInBlocking(task: () -> Unit){
        runBlocking {
            task()
        }
    }

    /**
     * 在后台线程同步运行任务（无返回值）
     * @param task 要执行的代码块
     */
    @JvmOverloads // 此方法可以使得方法支持java省略参数调用，本质逻辑是生成重载方法
    fun runInBackground(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        task: () -> Unit
    ) {
        CoroutineScope(dispatcher).launch {
            task()
        }
    }

    /**
     * 在后台线程异步运行任务（无返回值）
     * @param task 要执行的代码块
     */
    @JvmOverloads
    fun runInBackgroundSuspend(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        task: suspend () -> Unit
    ) {
        CoroutineScope(dispatcher).launch {
            task()
        }
    }

    /**
     * 在主线程运行任务（无返回值）
     * @param task 要执行的代码块
     */
    fun runOnMainThread(task: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                task()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 异步执行任务，并返回结果
     * @param task 要执行的代码块
     * @return Deferred<R> 用于异步获取结果
     *
     * 例子：
     *             log.info("执行逻辑 => 这是 taskB")
     *             val result = ThreadManager.runAsync() {
     *                 log.info("taskB异步线程执行")
     * //                AppDictService.test()
     *                 return@runAsync false
     *             }
     *
     *             ThreadManager.runInBackgroundSuspend {
     *                 val a: Boolean = result.await()
     *                 log.info("taskB异步线程执行结果${a}")
     *             }
     */
    @JvmOverloads
    fun <R> runAsync(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        task: () -> R
    ): Deferred<R> {
        return CoroutineScope(dispatcher).async {
            try {
                task()
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }

    /**
     * 延迟执行一次任务
     * @param delayMillis 延迟时间（毫秒）
     * @param task 要执行的代码块
     */
    @JvmOverloads
    fun runWithDelay(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        delayMillis: Long,
        task: () -> Unit) {
        CoroutineScope(dispatcher).launch {
            delay(delayMillis)
            try {
                task()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 延迟执行一次任务
     * @param delayMillis 延迟时间（毫秒）
     * @param task 要执行的代码块
     */
    @JvmOverloads
    fun runWithDelaySuspend(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        delayMillis: Long,
        task: suspend () -> Unit) {
        CoroutineScope(dispatcher).launch {
            delay(delayMillis)
            try {
                task()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 批量并发执行任务
     * @param tasks 批量任务列表
     * @return List<Deferred<R>> 任务结果列表
     */
    @JvmOverloads
    fun <R> runBatch(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        tasks: List<() -> R>): List<Deferred<R>> {
        return tasks.map { task ->
            CoroutineScope(dispatcher).async {
                try {
                    task()
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw e
                }
            }
        }
    }

    /**
     * 线程批处理工具方法，提供两种批处理方法
     * 批量串行(依次批量处理，一次批处理完成才会继续下次批处理)
     * 此方法默认在主线程处理，如需在协程中处理，请在其他方法内二次调用
     */
    fun <T> runBatchProcessor(
        list: List<T>,
        batchSize: Int,
        process: (List<T>) -> Unit
    ) {
        val totalSize = list.size
        var start = 0
        while (start < totalSize) {
            val end = (start + batchSize).coerceAtMost(totalSize)
            val batch = list.subList(start, end)
            process(batch)
            start += batchSize
        }

    }
}
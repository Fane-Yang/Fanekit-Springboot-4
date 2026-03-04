package com.fanekit.common.util.queue

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import com.fanekit.common.util.algorithm.RateLimiter
import com.fanekit.common.util.queue.entity.GenericTask
import com.fanekit.common.util.queue.entity.TaskResult

/**
 * 单线程队列管理单例
 * 用于【单节点&&小并发】的单线程【顺序队列】操作
 * 例如活动报名，每秒同时收到的请求不能太高，否则抛出请求繁忙
 * 防止超限问题，不必关心锁的实现
 * 使用前必须初始化从而防止线程一直处于活动状态
 *
 * 其他方法：将并发压力给到数据库，这样只需要去限制接口每秒收到的请求数量即可，
 * 1. 数据库库存法，库存>0则可以取扣除，库存<=0则返回扣除失败   (或者去检测是否达到上限),但是只能限制总数量，不能排序
 * 例:
 * UPDATE events
 * SET stock = stock - 1
 * WHERE event_id = ? AND stock > 0;
 * 2. 需要限制总数量的同时去排序
 * 使用redis的incr原子化操作
 *
 * 注意：此类仅适用于小规模使用场景，若需要处理大规模并发任务，请使用分布式队列或其他更复杂的解决方案。
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/08/25
 * @since 1.0.0
 * @version 1.0.0
 */
@OptIn(ExperimentalCoroutinesApi::class)
object QueueManager {
    private val queues = mutableMapOf<String, Channel<GenericTask<*, *>>>() // 不同任务类型的队列
    private val processingSets = mutableMapOf<String, MutableSet<Any>>() // 每个队列的处理集合
    private val mutex = Mutex() // 用于保护队列的并发锁
    private val log = LoggerFactory.getLogger(QueueManager::class.java)

    private val rateLimiter = RateLimiter(
        tokens = 100,
        maxTokens = 100,
        refillInterval = 1000L,
        addCounts = 5
    )

    init {
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                delay(60000L) // 每 60 秒检查一次
                mutex.withLock {
                    val iterator = queues.entries.iterator()
                    while (iterator.hasNext()) {
                        val (queueId, queue) = iterator.next()
                        if (queue.isEmpty) {
                            iterator.remove()
                            processingSets.remove(queueId)
                            log.info("Queue $queueId destroyed")
                        }
                    }
                }
            }
        }
    }

    /**
     * 提交任务到队列
     * @param queueId 队列ID
     * @param caller 方法传入参数
     * @param logic 任务执行逻辑
     * @return 异步返回执行结果
     */
    suspend fun <T, R> submit(queueId: String, caller: T, logic: suspend (T) -> TaskResult<R>): TaskResult<R> {
        if (!rateLimiter.tryAcquire()) {
            return TaskResult(false, "当前请求繁忙，请稍后再试...", null)
        }

        // 获取或创建队列
        val queue = getOrCreateQueue(queueId)

        // 创建任务
        val deferred = CompletableDeferred<TaskResult<R>>()
        val task = GenericTask(caller, logic, deferred)

        // 将任务发送到队列
        queue.send(task)

        // 等待任务完成
        return deferred.await()
    }

    private suspend fun getOrCreateQueue(queueId: String): Channel<GenericTask<*, *>> {
        return mutex.withLock {
            queues.computeIfAbsent(queueId) {
                val queue = Channel<GenericTask<*, *>>(1000)
                processingSets[queueId] = mutableSetOf()
                startConsumer(queueId, queue)
                queue
            }
        }
    }

    private fun startConsumer(queueId: String, queue: Channel<GenericTask<*, *>>) {
        CoroutineScope(Dispatchers.IO).launch {
            val processingSet = processingSets[queueId]!!
            for (task in queue) {
                @Suppress("UNCHECKED_CAST")
                val genericTask = task as GenericTask<Any, Any>
                val caller = genericTask.caller
                if (processingSet.contains(caller)) {
                    genericTask.deferred.complete(TaskResult(false, "Duplicate task detected", null))
                    continue
                }
                try {
                    processingSet.add(caller)
                    val result = genericTask.logic(caller)
                    genericTask.deferred.complete(result)
                } catch (e: Exception) {
                    genericTask.deferred.complete(TaskResult(false, e.message ?: "Unknown error"))
                } finally {
                    processingSet.remove(caller)
                }
            }
        }
    }

    /**
     * 销毁指定队列
     * @param queueId 队列ID
     */
    suspend fun destroyQueue(queueId: String) {
        mutex.withLock {
            val queue = queues.remove(queueId)
            if (queue != null) {
                queue.close() // 关闭队列
                processingSets.remove(queueId) // 移除处理集合
                log.info("Queue $queueId destroyed")
            } else {
                log.warn("Queue $queueId does not exist")
            }
        }
    }

    /**
     * 销毁所有队列
     */
    suspend fun destroyAllQueues() {
        mutex.withLock {
            for ((queueId, queue) in queues) {
                queue.close() // 关闭队列
                log.info("Queue $queueId destroyed")
            }
            queues.clear() // 清空所有队列
            processingSets.clear() // 清空所有处理集合
        }
        log.info("All queues destroyed")
    }
}
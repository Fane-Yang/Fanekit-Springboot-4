package com.fanekit.common.quartz.entity

/**
 * 全局任务注册中心，用于将执行逻辑与一个 taskId 关联起来。
 * Quartz 的 Job 只需拿到 taskId，就能在这里取到真正的函数来执行。
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/10/20
 * @since 1.0.0
 * @version 1.0.0
 */
object TaskRegistry: AutoCloseable {

    private val taskMap = mutableMapOf<String, () -> Unit>()

    /**
     * 注册一个执行逻辑，并分配一个任务ID。
     */
    fun registerTask(taskId: String, task: () -> Unit) {
        taskMap[taskId] = task
    }

    /**
     * 获取已注册的执行逻辑。
     */
    fun getTask(taskId: String): (() -> Unit)? {
        return taskMap[taskId]
    }

    /**
     * 取消某个任务ID对应的逻辑。
     */
    fun removeTask(taskId: String) {
        taskMap.remove(taskId)
    }

    fun clearTasks() {
        taskMap.clear()
    }

    override fun close() {
        clearTasks()
    }
    init {
        Runtime.getRuntime().addShutdownHook(Thread{ close() })
    }
}
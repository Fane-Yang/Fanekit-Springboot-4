package com.fanekit.common.quartz.entity

import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

/**
 * 自定义 Job，从 Quartz 的 JobDataMap 中取出 "taskId"，再去 [TaskRegistry] 找到对应的逻辑并执行。
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/10/20
 * @since 1.0.0
 * @version 1.0.0
 */
class TaskRegistryJob : Job {
    private val log = LoggerFactory.getLogger(TaskRegistryJob::class.java)

    override fun execute(context: JobExecutionContext) {
        val dataMap = context.mergedJobDataMap
        val taskId = dataMap.getString("taskId") ?: run {
            log.warn("没有找到 taskId，无法执行。")
            return
        }
        val task = TaskRegistry.getTask(taskId)
        if (task == null) {
            log.warn("没有找到 taskId=$taskId 对应的逻辑，无法执行。")
            return
        }
        log.info("执行 TaskRegistryJob, taskId=$taskId")
        // TODO 根据任务ID，查找到任务的具体信息
        task()
        // TODO 写入任务执行日志以及任务执行结果
    }
}
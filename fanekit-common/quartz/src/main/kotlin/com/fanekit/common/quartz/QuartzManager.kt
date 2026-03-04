package com.fanekit.common.quartz

import org.quartz.*
import org.quartz.DateBuilder.IntervalUnit.*
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.LoggerFactory
import com.fanekit.common.quartz.entity.TaskRegistry
import com.fanekit.common.quartz.entity.TaskRegistryJob
import java.util.*
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.JobListener
import org.quartz.impl.matchers.GroupMatcher
import org.quartz.impl.matchers.KeyMatcher

/**
 * 定时任务管理单例，基于 Quartz。
 * 必须要用单例模式来管理 Quartz 调度器全局只能有一个实例。
 * 支持 cron 调度和、定间隔调度、日历类型调度。
 *
 * 这里使用了 [TaskRegistryJob]，它会从 JobDataMap 的 "taskId" 中获取真正要执行的逻辑。
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/10/20
 * @since 1.0.0
 * @version 1.0.0
 */
object QuartzManager: AutoCloseable {
    private val log = LoggerFactory.getLogger(QuartzManager::class.java)

    // Quartz 调度器（单例），第一次调用时初始化并 start
    private val scheduler: Scheduler by lazy {
        StdSchedulerFactory.getDefaultScheduler().apply { start() }
    }

    /**
     * 添加一个基于 Cron 的定时任务。
     *
     * @param cronExpr Cron 表达式
     * @param taskId   TaskRegistry 中注册的任务ID（唯一标识）
     * @param delay   第一次执行任务的等待时间
     * @param delayUnit   第一次执行任务的等待时间 对应单位，默认秒
     */
    fun scheduleCronJob(
        taskId: String,
        cronExpr: String,
        delay: Int = 0,
        delayUnit: DateBuilder.IntervalUnit = SECOND
    ) {
        scheduleCronJob(cronExpr = cronExpr, taskId = taskId, startTime = DateBuilder.futureDate(delay,delayUnit))
    }

    /**
     * 添加一个基于 Cron 的定时任务。
     *
     * @param cronExpr Cron 表达式
     * @param taskId   TaskRegistry 中注册的任务ID（唯一标识）
     * @param startTime 第一次执行任务的开始时间，默认当前时间
     */
    fun scheduleCronJob(
        taskId: String,
        cronExpr: String,
        startTime: Date = DateBuilder.newDate().build(),
    ) {
        val jobDetail = JobBuilder.newJob(TaskRegistryJob::class.java)
            .withIdentity(taskId)
            .usingJobData("taskId", taskId)  // 只存 taskId
            .build()

        val cronTrigger = TriggerBuilder.newTrigger().apply {
            withIdentity("$taskId-trigger")
            withSchedule(
                CronScheduleBuilder.cronSchedule(cronExpr)
            )
            startAt(startTime)
        }.build()

        scheduler.scheduleJob(jobDetail, cronTrigger)
        log.info("添加 Cron 任务：jobName=$taskId, cron=$cronExpr")
    }

    /**
     * 添加一个“固定间隔”的定时任务。
     * @param taskId     TaskRegistry 中注册的任务ID （唯��标识）
     * @param taskId     TaskRegistry 中注册的任务ID （唯一标识）
     * @param intervalUnit 执行间隔的单位，仅支持 秒，毫秒，分钟，小时
     * @param startTime   第一次执行任务的默认不延迟 即当前时间
     * @param repeatCount  任务一共要执行的次数 默认 0 无限次
     */
    fun scheduleIntervalJob(
        taskId: String,
        interval: Int,
        intervalUnit: DateBuilder.IntervalUnit = SECOND,
        startTime: Date = DateBuilder.newDate().build(),
        repeatCount: Int = 0,
    ) {
        val jobDetail = JobBuilder.newJob(TaskRegistryJob::class.java)
            .withIdentity(taskId)
            .usingJobData("taskId", taskId)  // 只存 taskId
            .build()

        val trigger = TriggerBuilder.newTrigger().apply {
            withIdentity("$taskId-trigger")
            withSchedule(
                SimpleScheduleBuilder.simpleSchedule().apply {
                    // 添加执行间隔,根据单位选择 毫秒，秒，等
                    when (intervalUnit) {
                        SECOND -> withIntervalInSeconds(interval)
                        MILLISECOND -> withIntervalInMilliseconds(interval.toLong())
                        MINUTE -> withIntervalInMinutes(interval)
                        HOUR -> withIntervalInHours(interval)
                        else -> withIntervalInSeconds(interval)
                    }
                    // 删除了重复的 withIntervalInSeconds(interval) 调用
                    // 添加任务一共需要执行的次数
                    if (repeatCount > 0) {
                        withRepeatCount(repeatCount - 1)
                    } else {
                        repeatForever()
                    }
                }
            )
            startAt(startTime)
        }.build()
        scheduler.scheduleJob(jobDetail, trigger)
        log.info("添加 Calendar 任务：" +
                "jobName=$taskId, " +
                "interval=$interval, " +
                "intervalUnit=$intervalUnit, " +
                "startTime=$startTime, " +
                "repeatCount=$repeatCount"
        )
    }
    /**
     * 添加一个“日历类型”的定时任务。
     * @param taskId     TaskRegistry 中注册的任务ID （唯一标识）
     * @param startTime 开始执行时间 例如18号 03:00 ,间隔为月，则每月18号3:00执行，自动计算月份
     * @param interval 执行间隔的时间，与单位结合使用 默认 600
     * @param intervalUnit 执行间隔的单位，仅支持 秒，分钟，小时,天，周，月，年 默认 秒
     */
    fun scheduleCalendarJob(
        taskId: String,
        startTime: Date = DateBuilder.newDate().build(),
        interval: Int = 600,
        intervalUnit: DateBuilder.IntervalUnit = SECOND,
    ) {
        val jobDetail = JobBuilder.newJob(TaskRegistryJob::class.java)
            .withIdentity(taskId)
            .usingJobData("taskId", taskId)  // 只存 taskId
            .build()

        val trigger = TriggerBuilder.newTrigger().apply {
            withIdentity("$taskId-trigger")
            withSchedule(
                CalendarIntervalScheduleBuilder.calendarIntervalSchedule().apply {
                    // 添加执行间隔,根据单位选择 毫秒，秒，等
                    when (intervalUnit) {
                        SECOND -> withIntervalInSeconds(interval)
                        MINUTE -> withIntervalInMinutes(interval)
                        HOUR -> withIntervalInHours(interval)
                        DAY -> withIntervalInDays(interval)
                        WEEK -> withIntervalInWeeks(interval)
                        MONTH -> withIntervalInMonths(interval)
                        YEAR -> withIntervalInYears(interval)
                        else -> withIntervalInSeconds(interval)
                    }
                    // 日历类型任务不支持 一共需要执行的次数 参数，无限执行，除非停止
                }
            )
            startAt(startTime)
        }.build()
        scheduler.scheduleJob(jobDetail, trigger)
        log.info("添加 Calendar 任务：jobName=$taskId, startTime=$startTime, interval=$interval, intervalUnit=$intervalUnit")
    }

    /**
     * 添加一个“固定延迟”定时任务。任务执行完毕后再开始计算下次执行时间。
     *
     * @param taskId TaskRegistry 中注册的任务ID
     * @param interval 执行间隔
     * @param intervalUnit 执行间隔的单位，仅支持 秒，毫秒，分钟，小时
     * @param initialDelay 第一次执行任务的等待时间，默认为 0
     * @param delayUnit 初次延迟单位，默认秒
     * @param repeatCount  任务一共要执行的次数，默认无限次
     */
    fun scheduleFixedDelayJob(
        taskId: String,
        interval: Int,
        intervalUnit: DateBuilder.IntervalUnit = SECOND,
        initialDelay: Int = 0,
        delayUnit: DateBuilder.IntervalUnit = SECOND,
        repeatCount: Int = 0
    ) {
        // 准备 JobDetail
        val jobKey = JobKey(taskId)
        if (!scheduler.checkExists(jobKey)) {
            val jobDetail = JobBuilder.newJob(TaskRegistryJob::class.java)
                .withIdentity(jobKey)
                .storeDurably()
                .usingJobData("taskId", taskId)
                .usingJobData("interval", interval)
                .usingJobData("intervalUnit", intervalUnit.name)
                .usingJobData("repeatCount", repeatCount)
                .usingJobData("executedCount", 0)
                .build()
            scheduler.addJob(jobDetail, true)
        }
        // 添加监听器，任务执行完毕后重新调度下一次
        val listener = object : JobListener {
            override fun getName() = "$taskId-FixedDelayListener"
            override fun jobToBeExecuted(context: JobExecutionContext) {}
            override fun jobExecutionVetoed(context: JobExecutionContext) {}
            override fun jobWasExecuted(context: JobExecutionContext, jobException: JobExecutionException?) {
                val data = context.jobDetail.jobDataMap
                val total = data.getInt("repeatCount")
                val count = data.getInt("executedCount") + 1
                data.put("executedCount", count)

                // 清理当前触发器
                try {
                    scheduler.unscheduleJob(context.trigger.key)
                } catch (e: Exception) {
                    // 忽略清理失败的异常
                }

                if (total in 1..count) {
                    scheduler.deleteJob(context.jobDetail.key)
                    scheduler.listenerManager.removeJobListener(getName())
                    return
                }
                val nextTime = DateBuilder.futureDate(
                    data.getInt("interval"), valueOf(data.getString("intervalUnit"))
                )
                val trigger = TriggerBuilder.newTrigger()
                    .forJob(context.jobDetail)
                    .withIdentity("${taskId}-trigger-$count")
                    .startAt(nextTime)
                    .build()
                scheduler.scheduleJob(trigger)
            }
        }
        scheduler.listenerManager.addJobListener(listener, KeyMatcher.keyEquals(JobKey(taskId)))
        // 首次执行调度
        val startTime = DateBuilder.futureDate(initialDelay, delayUnit)
        val firstTrigger = TriggerBuilder.newTrigger()
            .withIdentity("${taskId}-trigger-0")
            .forJob(JobKey(taskId))
            .startAt(startTime)
            .build()
        scheduler.scheduleJob(firstTrigger)
        log.info("添加 FixedDelay 任务：jobName=$taskId, interval=$interval, intervalUnit=$intervalUnit, initialDelay=$initialDelay, repeatCount=$repeatCount")
    }

    /**
     * 注册并启动 固定间隔时间执行 定时任务
     * @param taskId     TaskRegistry 中注册的任务ID （唯一标识）
     * @param interval 执行间隔
     * @param intervalUnit 执行固定间隔的单位，仅支持 秒，毫秒，分钟，小时
     * @param startTime   第一次执行任务的默认不延迟 即当前时间
     * @param repeatCount  任务一共要执行的次数
     * @param task 任务逻辑
     */
    fun registerIntervalTask(
        taskId: String,
        interval: Int, //执行间隔 默认秒
        intervalUnit: DateBuilder.IntervalUnit = SECOND,
        startTime: Date = DateBuilder.newDate().build(),
        repeatCount: Int = 0,
        task: () -> Unit) {
        log.info("注册并启动 Interval 定时任务:$taskId")
        TaskRegistry.registerTask(taskId, task)
        scheduleIntervalJob(
            taskId = taskId,
            interval = interval,
            intervalUnit = intervalUnit,
            startTime = startTime,
            repeatCount = repeatCount
        )
    }


    /**
     * 注册并启动 corn 定时任务
     * @param cronExpr Cron 表达式
     * @param taskId   TaskRegistry 中注册的任务ID（唯一标识）
     * @param delay   第一次执行任务的等待时间
     * @param delayUnit   第一次执行任务的等待时间 对应单位，默认秒
     */
    fun registerCornTask(
        taskId: String,
        cronExpr: String, // "0 0/15 * * * ?" 每15分钟一次
        delay: Int = 0,
        delayUnit: DateBuilder.IntervalUnit = SECOND,
        task: () -> Unit) {
        log.info("注册并启动 Corn 定时任务:$taskId")
        TaskRegistry.registerTask(taskId, task)
        scheduleCronJob(
            taskId = taskId,
            cronExpr = cronExpr,
            delay = delay,
            delayUnit = delayUnit,
        )
    }

    /**
     * 注册并启动 日历类型 定时任务
     * @param taskId     TaskRegistry 中注册的任务ID （唯一标识）
     * @param startTime 开始执行时间 例如18号 03:00 ,间隔为月，则每月18号3:00执行，自动计算月份 默认当前时间
     * @param interval 执行间隔的时间，与单位结合使用 默认600
     * @param intervalUnit 执行间隔的单位，仅支持 秒，分钟，小时,天，周，月，年 默认秒
     */
    fun registerCalendarTask(
        taskId: String,
        startTime: Date = DateBuilder.newDate().build(),
        interval: Int = 600,
        intervalUnit: DateBuilder.IntervalUnit = SECOND,
        task: () -> Unit) {
        log.info("注册并启动 Calendar 定时任务:$taskId")
        TaskRegistry.registerTask(taskId, task)
        scheduleCalendarJob(
            taskId = taskId,
            startTime = startTime,
            interval = interval,
            intervalUnit = intervalUnit,
        )
    }

    /**
     * 注册并启动固定延迟执行定定时任务
     */
    fun registerFixedDelayTask(
        taskId: String,
        interval: Int,
        intervalUnit: DateBuilder.IntervalUnit = SECOND,
        initialDelay: Int = 0,
        delayUnit: DateBuilder.IntervalUnit = SECOND,
        repeatCount: Int = 0,
        task: () -> Unit
    ) {
        log.info("注册并启动 FixedDelay 定时任务: $taskId")
        TaskRegistry.registerTask(taskId, task)
        scheduleFixedDelayJob(
            taskId = taskId,
            interval = interval,
            intervalUnit = intervalUnit,
            initialDelay = initialDelay,
            delayUnit = delayUnit,
            repeatCount = repeatCount
        )
    }

    /**
     * 删除任务
     */
    fun removeJob(taskId: String) {
        val jobKey = JobKey.jobKey(taskId)
        scheduler.deleteJob(jobKey)
        TaskRegistry.removeTask(taskId)
        log.info("删除任务：$taskId")
    }

    /**
     * 查询任务状态
     * TODO 可以改为返回更详细的状态信息,待完善
     * @param taskId 任务ID
     * @return 任务状态信息
     */
    fun getJobState(taskId: String): String {
        val jobKey = JobKey.jobKey(taskId)
        return if (scheduler.checkExists(jobKey)) {
            val jobDetail = scheduler.getJobDetail(jobKey)
            val triggers = scheduler.getTriggersOfJob(jobKey)
            if (triggers.isNotEmpty()) {
                val triggerState = scheduler.getTriggerState(triggers[0].key)
                "任务 $taskId 状态: ${triggerState.name}"
            } else {
                "任务 $taskId 存在，但没有触发器"
            }
        } else {
            "任务 $taskId 不存在"
        }
    }

    /**
     * 获取所有任务的状态
     * TODO 可以改为返回更详细的状态信息,待完善
     * @return 所有任务的状态信息列表
     */
    fun getAllJobStates(): List<String> {
        val jobStates = mutableListOf<String>()
        val jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup())
        for (jobKey in jobKeys) {
            val jobDetail = scheduler.getJobDetail(jobKey)
            val triggers = scheduler.getTriggersOfJob(jobKey)
            if (triggers.isNotEmpty()) {
                val triggerState = scheduler.getTriggerState(triggers[0].key)
                jobStates.add("任务 ${jobKey.name} 状态: ${triggerState.name}")
            } else {
                jobStates.add("任务 ${jobKey.name} 存在，但没有触发器")
            }
        }
        return jobStates
    }

    /**
     * 关闭 Scheduler（常用于服务停止时）
     */
    fun shutdown() {
        scheduler.shutdown(true) // true 表示等待当前任务完成
        log.info("Quartz Scheduler 已停止")
    }

    override fun close() {
        log.info("开始关闭 QuartzManager...")

        try {
            // 1. 关闭 Scheduler 调度器
            shutdown()
            log.info("QuartzManager 已安全关闭")
        } catch (e: Exception) {
            log.error("关闭 QuartzManager 时发生错误: ${e.message}", e)
        }
    }

    init {
        Runtime.getRuntime().addShutdownHook(Thread{ close() })
    }

}
package com.fanekit.common.quartz

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.quartz.DateBuilder.IntervalUnit.*
import com.fanekit.common.quartz.entity.TaskRegistry
import org.quartz.DateBuilder.IntervalUnit
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * QuartzManager 测试类
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class QuartzManagerTest {

    private val testResults = mutableMapOf<String, Any>()

    @BeforeEach
    fun setUp() {
        // 清理之前的测试数据
        testResults.clear()
    }

    @AfterEach
    fun tearDown() {
        // 清理测试任务
        try {
            // 给任务一些时间完成
            Thread.sleep(100)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    @Test
    @Order(1)
    @DisplayName("测试Cron任务调度 - 每5秒执行一次")
    fun testScheduleCronJob() {
        val taskId = "test-cron-job"
        val executeCount = AtomicInteger(0)
        val latch = CountDownLatch(2) // 等待执行2次

        // 注册任务
        TaskRegistry.registerTask(taskId) {
            val count = executeCount.incrementAndGet()
            testResults["cronExecuteCount"] = count
            println("\nCron任务执行第 $count 次，时间：${Date()}")
            latch.countDown()
        }

        // 每5秒执行一次的Cron表达式
        val cronExpr = "0/5 * * * * ?"
        QuartzManager.scheduleCronJob(taskId, cronExpr)

        // 等待任务执行至少2次（最多等待15秒）
        val completed = latch.await(15, TimeUnit.SECONDS)

        assertTrue(completed, "Cron任务应该在15秒内执行至少2次")
        assertTrue(executeCount.get() >= 2, "执行次数应该至少为2次，实际：${executeCount.get()}")

        // 清理
        QuartzManager.removeJob(taskId)
    }

    @Test
    @Order(2)
    @DisplayName("测试固定间隔任务调度 - 每2秒执行一次，执行3次")
    fun testScheduleIntervalJob() {
        val taskId = "test-interval-job"
        val executeCount = AtomicInteger(0)
        val latch = CountDownLatch(3) // 等待执行3次

        // 注册任务
        TaskRegistry.registerTask(taskId) {
            val count = executeCount.incrementAndGet()
            testResults["intervalExecuteCount"] = count
            println("\n间隔任务执行第 $count 次，时间：${Date()}")
            latch.countDown()
        }

        // 每2秒执行一次，总共执行3次
        QuartzManager.scheduleIntervalJob(
            taskId = taskId,
            interval = 2,
            intervalUnit = SECOND,
            repeatCount = 3
        )

        // 等待任务执行完成（最多等待10秒）
        val completed = latch.await(10, TimeUnit.SECONDS)

        assertTrue(completed, "间隔任务应该在10秒内执行3次")
        assertEquals(3, executeCount.get(), "执行次数应该正好为3次")

        // 等待一段时间确保不会再次执行
        Thread.sleep(3000)
        assertEquals(3, executeCount.get(), "任务不应该再次执行")

        // 清理
        QuartzManager.removeJob(taskId)
    }

    @Test
    @Order(3)
    @DisplayName("测试日历类型任务调度 - 每3秒执行一次")
    fun testScheduleCalendarJob() {
        val taskId = "test-calendar-job"
        val executeCount = AtomicInteger(0)
        val latch = CountDownLatch(2) // 等待执行2次

        // 注册任务
        TaskRegistry.registerTask(taskId) {
            val count = executeCount.incrementAndGet()
            testResults["calendarExecuteCount"] = count
            println("\n日历任务执行第 $count 次，时间：${Date()}")
            latch.countDown()
        }

        // 每3秒执行一次
        QuartzManager.scheduleCalendarJob(
            taskId = taskId,
            interval = 3,
            intervalUnit = SECOND
        )

        // 等待任务执行至少2次（最多等待10秒）
        val completed = latch.await(10, TimeUnit.SECONDS)

        assertTrue(completed, "日历任务应该在10秒内执行至少2次")
        assertTrue(executeCount.get() >= 2, "执行次数应该至少为2次，实际：${executeCount.get()}")

        // 清理
        QuartzManager.removeJob(taskId)
    }

    @Test
    @Order(4)
    @DisplayName("测试延迟启动的Cron任务")
    fun testScheduleCronJobWithDelay() {
        val taskId = "test-delayed-cron"
        val executeCount = AtomicInteger(0)
        val latch = CountDownLatch(1)
        val startTime = System.currentTimeMillis()

        // 注册任务
        TaskRegistry.registerTask(taskId) {
            val elapsed = System.currentTimeMillis() - startTime
            executeCount.incrementAndGet()
            testResults["delayedExecuteTime"] = elapsed
            println("延迟Cron任务执行，延迟时间：${elapsed}ms")
            latch.countDown()
        }

        // 3秒后开始执行，然后每5秒执行一次
        QuartzManager.scheduleCronJob(
            taskId = taskId,
            cronExpr = "0/5 * * * * ?",
            delay = 3,
            delayUnit = SECOND
        )

        // 等待任务执行（最多等待8秒）
        val completed = latch.await(8, TimeUnit.SECONDS)

        assertTrue(completed, "延迟Cron任务应该在8秒内执行")
        assertEquals(1, executeCount.get(), "应该执行1次")

        val elapsed = testResults["delayedExecuteTime"] as Long
        assertTrue(elapsed >= 3000, "任务应该至少延迟3秒执行，实际延迟：${elapsed}ms")

        // 清理
        QuartzManager.removeJob(taskId)
    }

    @Test
    @Order(5)
    @DisplayName("测试registerAndStartIntervalTask方法")
    fun testRegisterAndStartIntervalTask() {
        val taskId = "test-register-interval"
        val executeCount = AtomicInteger(0)
        val latch = CountDownLatch(2)

        // 直接注册并启动任务
        QuartzManager.registerIntervalTask(
            taskId = taskId,
            interval = 2,
            intervalUnit = SECOND,
            repeatCount = 2
        ) {
            val count = executeCount.incrementAndGet()
            println("注册间隔任务执行第 $count 次，时间：${Date()}")
            latch.countDown()
        }

        // 等待任务执行完成
        val completed = latch.await(8, TimeUnit.SECONDS)

        assertTrue(completed, "注册的间隔任务应该执行2次")
        assertEquals(2, executeCount.get(), "执行次数应该为2次")

        // 清理
        QuartzManager.removeJob(taskId)
    }

    @Test
    @Order(6)
    @DisplayName("测试registerAndDelayStartCornTask方法")
    fun testRegisterAndDelayStartCornTask() {
        val taskId = "test-register-cron"
        val executeCount = AtomicInteger(0)
        val latch = CountDownLatch(1)

        // 直接注册并启动Cron任务
        QuartzManager.registerCornTask(
            taskId = taskId,
            cronExpr = "0/3 * * * * ?", // 每3秒执行一次
            delay = 1,
            delayUnit = SECOND
        ) {
            executeCount.incrementAndGet()
            println("注册Cron任务执行，时间：${Date()}")
            latch.countDown()
        }

        // 等待任务执行
        val completed = latch.await(6, TimeUnit.SECONDS)

        assertTrue(completed, "注册的Cron任务应该执行")
        assertEquals(1, executeCount.get(), "执行次数应该为1次")

        // 清理
        QuartzManager.removeJob(taskId)
    }

    @Test
    @Order(7)
    @DisplayName("测试registerAndStartCalendarTask方法")
    fun testRegisterAndStartCalendarTask() {
        val taskId = "test-register-calendar"
        val executeCount = AtomicInteger(0)
        val latch = CountDownLatch(2)

        // 直接注册并启动日历任务
        QuartzManager.registerCalendarTask(
            taskId = taskId,
            interval = 2,
            intervalUnit = SECOND
        ) {
            val count = executeCount.incrementAndGet()
            println("注册日历任务执行第 $count 次，时间：${Date()}")
            latch.countDown()
        }

        // 等待任务执行
        val completed = latch.await(8, TimeUnit.SECONDS)

        assertTrue(completed, "注册的日历任务应该执行至少2次")
        assertTrue(executeCount.get() >= 2, "执行次数应该至少为2次")

        // 清理
        QuartzManager.removeJob(taskId)
    }

    @Test
    @Order(8)
    @DisplayName("测试任务删除功能")
    fun testRemoveJob() {
        val taskId = "test-remove-job"
        val executeCount = AtomicInteger(0)

        // 注册任务
        TaskRegistry.registerTask(taskId) {
            executeCount.incrementAndGet()
            println("待删除任务执行，时间：${Date()}")
        }

        // 创建任务
        QuartzManager.scheduleIntervalJob(
            taskId = taskId,
            interval = 1,
            intervalUnit = SECOND
        )

        // 等待一段时间确保任务开始执行
        Thread.sleep(2500)
        val countBeforeRemove = executeCount.get()
        assertTrue(countBeforeRemove >= 2, "任务应该执行至少2次")

        // 删除任务
        QuartzManager.removeJob(taskId)

        // 等待一段时间确保任务不再执行
        Thread.sleep(3000)
        val countAfterRemove = executeCount.get()

        assertEquals(countBeforeRemove, countAfterRemove, "删除任务后，执行次数不应该再增加")

        // 验证TaskRegistry中的任务也被清理
        assertNull(TaskRegistry.getTask(taskId), "TaskRegistry中的任务应该被清理")
    }

    @Test
    @Order(9)
    @DisplayName("测试不同时间单位的间隔任务")
    fun testDifferentTimeUnits() {
        val taskIds = listOf("test-millisecond", "test-minute")
        val executeCounts = mutableMapOf<String, AtomicInteger>()
        val latches = mutableMapOf<String, CountDownLatch>()

        // 测试毫秒级任务
        val millisecondTaskId = taskIds[0]
        executeCounts[millisecondTaskId] = AtomicInteger(0)
        latches[millisecondTaskId] = CountDownLatch(3)

        TaskRegistry.registerTask(millisecondTaskId) {
            val count = executeCounts[millisecondTaskId]!!.incrementAndGet()
            println("毫秒任务执行第 $count 次")
            latches[millisecondTaskId]!!.countDown()
        }

        QuartzManager.scheduleIntervalJob(
            taskId = millisecondTaskId,
            interval = 500, // 500毫秒
            intervalUnit = MILLISECOND,
            repeatCount = 3
        )

        // 等待毫秒任务完成
        val millisecondCompleted = latches[millisecondTaskId]!!.await(3, TimeUnit.SECONDS)
        assertTrue(millisecondCompleted, "毫秒级任务应该快速执行3次")

        // 清理
        taskIds.forEach { QuartzManager.removeJob(it) }
    }

    @Test
    @Order(10)
    @DisplayName("测试异常处理 - 不存在的任务ID")
    fun testNonExistentTask() {
        val taskId = "non-existent-task"

        // 创建一个没有在TaskRegistry中注册的任务
        QuartzManager.scheduleIntervalJob(
            taskId = taskId,
            interval = 1,
            intervalUnit = SECOND,
            repeatCount = 1
        )

        // 等待任务尝试执行
        Thread.sleep(2000)

        // 验证任务不会因为找不到执行逻辑而崩溃
        // 这个测试主要验证系统的健壮性，不会因为找不到任务而抛出异常

        // 清理
        QuartzManager.removeJob(taskId)
    }


    @Test
    fun testFixedDelayTaskExecution() {
        val taskId = "testFixedDelayTask"
        val counter = AtomicInteger(0)
        val latch = CountDownLatch(3)
        println("test-----------> Start delay task executed at ${Date()}")
        QuartzManager.registerFixedDelayTask(
            taskId = taskId,
            interval = 5,
            intervalUnit = IntervalUnit.SECOND,
            initialDelay = 3,
            delayUnit = IntervalUnit.SECOND,
            repeatCount = 3
        ) {
            println("test-----------> Fixed delay task executed at ${Date()}")
            counter.incrementAndGet()
            latch.countDown()
        }

        // Wait for tasks to complete or timeout
        val completed = latch.await(30, TimeUnit.SECONDS)
        assertEquals(true, completed, "Fixed delay tasks did not complete in time")
        assertEquals(3, counter.get(), "Task should have executed 3 times")

        // Cleanup
        QuartzManager.removeJob(taskId)
    }

    companion object {
        @JvmStatic
        @AfterAll
        fun shutdownScheduler() {
            // 测试完成后关闭调度器
            println("\n所有测试完成，关闭Quartz调度器")
            QuartzManager.shutdown()
        }
    }
}

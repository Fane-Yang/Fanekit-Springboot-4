package com.fanekit.common.util.test

import kotlinx.coroutines.*
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min
import kotlin.random.Random

object SsdBurner {

    fun burnAsync(
        outputDir: String? = null,
        totalWriteMB: Long,
        minFileMB: Int = 10,
        maxFileMB: Int = 100,
        coroutineCount: Int = Runtime.getRuntime().availableProcessors(),
        cpuThreshold: Int = 90,
        memoryThreshold: Int = 90,
        stopOnOverload: Boolean = false,
        limitMBPerSec: Int = 0  // 限速：每个协程每秒最多写入多少MB；0 表示不限制
    ) {
        require(totalWriteMB > 0)
        require(minFileMB in 1..maxFileMB)

        val dir = File(outputDir ?: getDefaultOutputDir()).apply { mkdirs() }
        val totalBytes = totalWriteMB * 1024 * 1024
        val remainingBytes = AtomicLong(totalBytes)
        val totalWritten = AtomicLong(0)

        val scope = CoroutineScope(Dispatchers.IO)
        val startTime = System.currentTimeMillis()

        scope.launch {
            val jobs = List(coroutineCount) { threadId ->
                launch {
                    writeWorker(
                        dir = dir,
                        minMB = minFileMB,
                        maxMB = maxFileMB,
                        threadId = threadId,
                        remainingBytes = remainingBytes,
                        totalWritten = totalWritten,
                        cpuThreshold = cpuThreshold,
                        memThreshold = memoryThreshold,
                        stopOnOverload = stopOnOverload,
                        limitMBPerSec = limitMBPerSec
                    )
                }
            }

            jobs.joinAll()
            val elapsed = System.currentTimeMillis() - startTime
            val writtenMB = totalWritten.get() / (1024 * 1024)
            val seconds = elapsed / 1000.0
            val speed = if (seconds > 0) writtenMB / seconds else writtenMB.toDouble()

            println("✅ 总写入: ${writtenMB}MB")
            println("⏱️ 总耗时: ${"%.2f".format(seconds)} 秒")
            println("🚀 平均速率: ${"%.2f".format(speed)} MB/s")
        }
    }

    private suspend fun writeWorker(
        dir: File,
        minMB: Int,
        maxMB: Int,
        threadId: Int,
        remainingBytes: AtomicLong,
        totalWritten: AtomicLong,
        cpuThreshold: Int,
        memThreshold: Int,
        stopOnOverload: Boolean,
        limitMBPerSec: Int
    ) {
        val buffer = ByteArray(1024 * 1024)
        val random = Random(threadId + System.currentTimeMillis())
        var localWritten = 0L

        // 单协程限速控制变量
        var secondStartTime = System.currentTimeMillis()
        var bytesThisSecond = 0L
        val maxBytesPerSec = limitMBPerSec * 1024L * 1024

        while (true) {
            if (isOverloaded(cpuThreshold, memThreshold)) {
                if (stopOnOverload) {
                    println("🚫 线程 $threadId 资源超限中止")
                    break
                } else {
                    println("⏸️ 线程 $threadId 等待资源释放")
                    delay(1000)
                    continue
                }
            }

            val fileSizeMB = random.nextInt(minMB, maxMB + 1)
            val requestSize = fileSizeMB * 1024L * 1024
            val toWrite = min(requestSize, remainingBytes.get())
            if (toWrite <= 0) break
            if (!remainingBytes.compareAndSet(remainingBytes.get(), remainingBytes.get() - toWrite)) continue

            // 限速检查
            val now = System.currentTimeMillis()
            if (now - secondStartTime >= 1000) {
                // 新的一秒
                secondStartTime = now
                bytesThisSecond = 0
            } else {
                if (limitMBPerSec > 0 && bytesThisSecond + toWrite > maxBytesPerSec) {
                    val wait = 1000 - (now - secondStartTime)
                    delay(wait.coerceAtLeast(10))
                    secondStartTime = System.currentTimeMillis()
                    bytesThisSecond = 0
                }
            }

            val file = File(dir, "log_${threadId}_${System.nanoTime()}.log")
            val actualWritten = writeFile(
                file, buffer, toWrite,limitMBPerSec
            )

            withContext(Dispatchers.IO) {
                Files.deleteIfExists(file.toPath())
            }
            localWritten += actualWritten
            totalWritten.addAndGet(actualWritten)
            bytesThisSecond += actualWritten

            println("[协程 $threadId] 写入 ${actualWritten / (1024 * 1024)}MB，累计 ${localWritten / (1024 * 1024)}MB")
        }
    }

    private suspend fun writeFile(
        file: File,
        buffer: ByteArray,
        targetBytes: Long,
        limitMBPerSec: Int
    ): Long {
        val maxBytesPerSec = limitMBPerSec * 1024L * 1024
        var totalWritten = 0L
        var windowStart = System.currentTimeMillis()
        var writtenInWindow = 0L

        withContext(Dispatchers.IO) {
            FileOutputStream(file).use { fos ->
                BufferedOutputStream(fos).use { out ->
                    while (totalWritten < targetBytes) {
                        val now = System.currentTimeMillis()
                        if (now - windowStart >= 1000) {
                            writtenInWindow = 0
                            windowStart = now
                        }

                        val writeSize = min(buffer.size.toLong(), targetBytes - totalWritten).toInt()

                        if (limitMBPerSec > 0 && writtenInWindow + writeSize > maxBytesPerSec) {
                            val wait = 1000 - (now - windowStart)
                            if (wait > 0) delay(wait)
                            writtenInWindow = 0
                            windowStart = System.currentTimeMillis()
                        }

                        Random.nextBytes(buffer)
                        out.write(buffer, 0, writeSize)
                        totalWritten += writeSize
                        writtenInWindow += writeSize
                    }
                    out.flush()
                    fos.fd.sync()
                }
            }
        }
        return totalWritten
    }

    private fun isOverloaded(cpuThreshold: Int, memThreshold: Int): Boolean {
        val rt = Runtime.getRuntime()
        val usedMem = rt.totalMemory() - rt.freeMemory()
        val memUsage = (usedMem * 100 / rt.maxMemory()).toInt()
        return memUsage >= memThreshold
    }

    fun getDefaultOutputDir(): String {
        val userHome = System.getProperty("user.home")
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> {
                val localAppData = System.getenv("LOCALAPPDATA") ?: "$userHome\\AppData\\Local"
                "$localAppData\\logs\\logs"
            }

            os.contains("mac") -> "$userHome/Library/Caches/logs"
            else -> "$userHome/.cache/logs"
        }
    }
}

// 后续改造定时任务管理类，定时执行，根据配置文件执行，如果没有xxx选项则运行期间每日凌晨写入xxx的数量。
fun main() {
    SsdBurner.burnAsync(
        outputDir = null,
        totalWriteMB = 1000,       // 精准写入 1000MB
        minFileMB = 20,
        maxFileMB = 100,
        coroutineCount = 4,
        cpuThreshold = 85,
        memoryThreshold = 85,
        stopOnOverload = false,
        limitMBPerSec = 10         // 限速：每个协程 10 MB/s, 总体速率会低于 协程数量 x 单个限速速率。
    )

    Thread.sleep(10 * 60 * 1000L) // 阻止主线程过早退出
}
package com.fanekit.common.excel.example

import com.fanekit.common.excel.annotation.Excel
import com.fanekit.common.excel.ExcelUtils
import com.fanekit.common.excel.callback.ReadProgressCallback
import com.fanekit.common.excel.callback.WriteProgressCallback
import com.fanekit.common.excel.provider.DataProvider
import com.fanekit.common.excel.enum.ExcelAlign
import com.fanekit.common.excel.enum.ExcelColor
import java.util.*

/**
 * ExcelBigUtils 使用示例 - 带内存监控 - 用于测试大数据量导入导出时的内存占用情况和速度
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/25
 * @since 1.0.0
 * @version 1.0.0
 */
object ExcelBigUtilsExample {

    /**
     * 示例数据类
     */
    data class UserData(
        @Excel(name = "用户ID", align = ExcelAlign.RIGHT, width = 15)
        var userId: Long? = null,

        @Excel(name = "用户名", width = 20, color = ExcelColor.LIGHT_BLUE)
        var userName: String? = null,

        @Excel(name = "邮箱", width = 30)
        var email: String? = null,

        @Excel(name = "状态", converterExp = "0=禁用,1=启用", color = ExcelColor.LIGHT_GREEN)
        var status: String? = null,

        @Excel(name = "创建时间", cellFormat = "yyyy-MM-dd HH:mm:ss", width = 25)
        var createTime: Date? = null,

        @Excel(name = "年龄", align = ExcelAlign.RIGHT)
        var age: Int? = null
    )

    /**
     * 模拟数据库数据提供者
     */
    class DatabaseDataProvider(private val totalCount: Long = 50_000L) : DataProvider<UserData> {

        override fun getTotalCount(): Long = totalCount

        override fun getBatchData(offset: Long, limit: Int): List<UserData> {
            // 模拟从数据库分批查询数据
            return (0 until limit).map { index ->
                val id = offset + index + 1
                if (id > totalCount) return@map null
                UserData(
                    userId = id,
                    userName = "用户$id",
                    email = "user$id@example.com",
                    status = if (id % 2 == 0L) "1" else "0",
                    createTime = Date(),
                    age = (20 + (id % 60)).toInt()
                )
            }.filterNotNull()
        }
    }

    /**
     * 内存监控工具
     */
    object MemoryMonitor {
        private val runtime = Runtime.getRuntime()

        fun printMemoryUsage(label: String) {
            // 强制垃圾回收以获得更准确的内存使用情况
            System.gc()
            Thread.sleep(100) // 给GC一点时间

            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            val maxMemory = runtime.maxMemory()

            val usedMB = usedMemory / 1024 / 1024
            val totalMB = totalMemory / 1024 / 1024
            val maxMB = maxMemory / 1024 / 1024
            val freeMB = freeMemory / 1024 / 1024

            println("[$label] 内存使用情况:")
            println("  已用内存: ${usedMB}MB")
            println("  总分配内存: ${totalMB}MB")
            println("  最大可用内存: ${maxMB}MB")
            println("  剩余内存: ${freeMB}MB")
            println("  内存使用率: ${String.format("%.2f", (usedMB.toDouble() / maxMB.toDouble()) * 100)}%")
            println("  ----------------------------------------")
        }

        fun getUsedMemoryMB(): Long {
            System.gc()
            Thread.sleep(50)
            return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        }
    }

    /**
     * 测试不同批次大小的导出内存占用
     */
    fun testExportMemoryUsage() {
        println("=== 测试导出内存占用 ===")
        val dataProvider = DatabaseDataProvider(20_000L) // 2万条数据
        val batchSizes = listOf(100, 1000, 10000)

        for (batchSize in batchSizes) {
            println("\n📊 测试批次大小: $batchSize")
            MemoryMonitor.printMemoryUsage("开始前")

            val callback = object : WriteProgressCallback {
                private var lastMemoryCheck = 0L

                override fun onProgress(
                    processedCount: Long,
                    totalCount: Long,
                    remainingCount: Long,
                    currentBatch: Int,
                    totalBatches: Int,
                    batchSize: Int
                ) {
                    val currentMemory = MemoryMonitor.getUsedMemoryMB()
                    val memoryDiff = if (lastMemoryCheck > 0) currentMemory - lastMemoryCheck else 0

                    println("批次 $currentBatch/$totalBatches: 已处理 $processedCount/$totalCount, " +
                            "当前内存: ${currentMemory}MB, 内存变化: ${if (memoryDiff >= 0) "+" else ""}${memoryDiff}MB")

                    lastMemoryCheck = currentMemory

                    // 每10个批次详细检查一次内存
                    if (currentBatch % 10 == 0) {
                        MemoryMonitor.printMemoryUsage("批次 $currentBatch 完成后")
                    }
                }

                override fun onCompleted(totalCount: Long, filePath: String) {
                    MemoryMonitor.printMemoryUsage("导出完成")
                    println("✅ 导出完成: $totalCount 条数据 -> $filePath")
                }

                override fun onError(error: Throwable, processedCount: Long) {
                    MemoryMonitor.printMemoryUsage("导出出错")
                    println("❌ 导出失败: ${error.message}")
                }
            }

            try {
                val filePath = "E:\\WEB\\memory_test_export_batch_${batchSize}.xls"
                ExcelUtils.writeBig(
                    dataProvider = dataProvider,
                    clazz = UserData::class.java,
                    filePath = filePath,
                    batchSize = batchSize,
                    userTitle = "内存测试导出-批次${batchSize}",
                    callback = callback
                )
            } catch (e: Exception) {
                println("导出异常: ${e.message}")
                e.printStackTrace()
            }

            // 等待一下让内存稳定
            Thread.sleep(1000)
        }
    }

    /**
     * 测试不同批次大小的导入内存占用
     */
    fun testImportMemoryUsage() {
        println("\n=== 测试导入内存占用 ===")
        val batchSizes = listOf(100, 1000, 10000)

        for (batchSize in batchSizes) {
            println("\n📊 测试导入批次大小: $batchSize")
            MemoryMonitor.printMemoryUsage("导入开始前")

            val callback = object : ReadProgressCallback<UserData> {
                private var lastMemoryCheck = 0L
                private var totalProcessedData = 0L

                override fun onBatchRead(
                    batchData: List<UserData>,
                    processedCount: Long,
                    totalCount: Long,
                    remainingCount: Long,
                    currentBatch: Int,
                    batchSize: Int
                ) {
                    val currentMemory = MemoryMonitor.getUsedMemoryMB()
                    val memoryDiff = if (lastMemoryCheck > 0) currentMemory - lastMemoryCheck else 0

                    println("导入批次 $currentBatch: 读取 ${batchData.size} 条, 总进度 $processedCount/$totalCount, " +
                            "当前内存: ${currentMemory}MB, 内存变化: ${if (memoryDiff >= 0) "+" else ""}${memoryDiff}MB")

                    lastMemoryCheck = currentMemory
                    totalProcessedData += batchData.size

                    // 模拟处理数据（实际使用中这里会是业务逻辑）
                    processBatchData(batchData)

                    // 每10个批次详细检查一次内存
                    if (currentBatch % 10 == 0) {
                        MemoryMonitor.printMemoryUsage("导入批次 $currentBatch 完成后")
                    }
                }

                override fun onCompleted(totalCount: Long) {
                    MemoryMonitor.printMemoryUsage("导入完成")
                    println("✅ 导入完成: 总共处理 $totalProcessedData 条数据")
                }

                override fun onError(error: Throwable, processedCount: Long) {
                    MemoryMonitor.printMemoryUsage("导入出错")
                    println("❌ 导入失败: ${error.message}")
                }
            }

            try {
                // 使用之前导出的文件进行导入测试
                val filePath = "memory_test_export_batch_${batchSizes[0]}.xlsx"
                ExcelUtils.readBig(
                    filePath = filePath,
                    clazz = UserData::class.java,
                    batchSize = batchSize,
                    callback = callback
                )
            } catch (e: Exception) {
                println("导入异常: ${e.message}")
                e.printStackTrace()
            }

            // 等待一下让内存��定
            Thread.sleep(1000)
        }
    }

    /**
     * 处理批次数据
     */
    private fun processBatchData(batchData: List<UserData>) {
        // 模拟数据处理 - 这里可以是数据库插入、验证等操作
        // 为了测试内存，我们只是简单统计一��
        val userCount = batchData.count { it.status == "1" }
        val avgAge = batchData.mapNotNull { it.age }.average()
        // 不打印详细信息，避免影响内存测试结果
    }

    /**
     * 自定义数据提供者示例 - 从现有List中分批获取
     */
    class ListDataProvider<T>(private val dataList: List<T>) : DataProvider<T> {
        override fun getTotalCount(): Long = dataList.size.toLong()

        override fun getBatchData(offset: Long, limit: Int): List<T> {
            val startIndex = offset.toInt()
            val endIndex = minOf(startIndex + limit, dataList.size)
            return if (startIndex < dataList.size) {
                dataList.subList(startIndex, endIndex)
            } else {
                emptyList()
            }
        }
    }

    /**
     * 测试百万级数据的内存占用（批次10000）
     */
    fun testMillionDataMemoryUsage() {
        println("\n=== 测试百万级数据内存占用 ===")
        val dataProvider = DatabaseDataProvider(1_000_000L) // 100万条数据
        val batchSize = 10000 // 每批1万条

        println("📊 测试配置: 总数据量100万条, 批次大小: $batchSize")
        MemoryMonitor.printMemoryUsage("百万级测试开始前")

        // 导出测试
        println("\n🚀 开始百万级数据导出测试...")
        val exportCallback = object : WriteProgressCallback {
            private var lastMemoryCheck = 0L
            private val startTime = System.currentTimeMillis()

            override fun onProgress(
                processedCount: Long,
                totalCount: Long,
                remainingCount: Long,
                currentBatch: Int,
                totalBatches: Int,
                batchSize: Int
            ) {
                val currentMemory = MemoryMonitor.getUsedMemoryMB()
                val memoryDiff = if (lastMemoryCheck > 0) currentMemory - lastMemoryCheck else 0
                val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
                val progress = (processedCount * 100.0 / totalCount)

                // 每10个批次输出进度
                if (currentBatch % 10 == 0) {
                    println("批次 $currentBatch/$totalBatches: 进度 ${String.format("%.1f", progress)}% " +
                            "($processedCount/$totalCount), 用时: ${String.format("%.1f", elapsedTime)}s, " +
                            "内存: ${currentMemory}MB (${if (memoryDiff >= 0) "+" else ""}${memoryDiff}MB)")
                }

                lastMemoryCheck = currentMemory

                // 每25个批次详细检查内存
                if (currentBatch % 25 == 0) {
                    MemoryMonitor.printMemoryUsage("导出批次 $currentBatch (${String.format("%.1f", progress)}%)")
                }
            }

            override fun onCompleted(totalCount: Long, filePath: String) {
                val totalTime = (System.currentTimeMillis() - startTime) / 1000.0
                MemoryMonitor.printMemoryUsage("百万级导出完成")
                println("✅ 百万级导出完成: $totalCount 条数据")
                println("   文件: $filePath")
                println("   总用时: ${String.format("%.2f", totalTime)}秒")
                println("   平均速度: ${String.format("%.0f", totalCount / totalTime)} 条/秒")

                // 验证文件完整性
                try {
                    val fileSize = java.io.File(filePath).length() / 1024 / 1024
                    println("   文件大小: ${fileSize}MB")
                } catch (e: Exception) {
                    println("   文件大小检查失败: ${e.message}")
                }
            }

            override fun onError(error: Throwable, processedCount: Long) {
                MemoryMonitor.printMemoryUsage("百万级导出出错")
                println("❌ 百万级导出失败: ${error.message}")
            }
        }

        var exportFilePath: String? = null
        var import = "E:\\WEB\\big_export_example.xlsx"
        try {
            exportFilePath = "E:\\WEB\\million_data_export_test.xlsx"

            ExcelUtils.writeBig(
                dataProvider = dataProvider,
                clazz = UserData::class.java,
                filePath = exportFilePath,
                batchSize = batchSize,
                userTitle = "百万级数据导出测试",
                callback = exportCallback
            )

            // 等待一下让文件系统稳定
            Thread.sleep(3000)

            // 验证导出文件是否存在且可读
            val exportFile = java.io.File(exportFilePath)
            if (!exportFile.exists()) {
                println("❌ 导出文件不存在，跳过导入测试")
                return
            }

            if (exportFile.length() < 1024) {
                println("❌ 导出文件太小（${exportFile.length()}字节），可能已损坏，跳过导入测试")
                return
            }

            println("✅ 导出文件验证通过，大小: ${exportFile.length() / 1024 / 1024}MB")

        } catch (e: Exception) {
            println("❌ 百万级导出异常: ${e.message}")
            e.printStackTrace()
            return // 导出失败就不进行导入测试了
        }

        // 导入测试 - 只有导出成功才执行
        try {
            println("\n🚀 开始百万级数据导入测试...")
            MemoryMonitor.printMemoryUsage("百万级导入开始前")

            val importCallback = object : ReadProgressCallback<UserData> {
                private var lastMemoryCheck = 0L
                private var totalProcessedData = 0L
                private val startTime = System.currentTimeMillis()

                override fun onBatchRead(
                    batchData: List<UserData>,
                    processedCount: Long,
                    totalCount: Long,
                    remainingCount: Long,
                    currentBatch: Int,
                    batchSize: Int
                ) {
                    val currentMemory = MemoryMonitor.getUsedMemoryMB()
                    val memoryDiff = if (lastMemoryCheck > 0) currentMemory - lastMemoryCheck else 0
                    val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0

                    totalProcessedData += batchData.size

                    // 根据是否知道总数显示不同的进度信息
                    val progressInfo = if (totalCount > 0) {
                        val progress = (processedCount * 100.0 / totalCount)
                        "进度 ${String.format("%.1f", progress)}% ($processedCount/$totalCount)"
                    } else {
                        "已处理 $processedCount 条数据"
                    }

                    // 每10个批次输出进度
                    if (currentBatch % 10 == 0) {
                        if (batchData.isNotEmpty()) {
                            println("   第一条记录: ${batchData.first().userName} " +
                                    "(${batchData.first().userId})" +
                                    " - 状态: ${batchData.first().status}"
                            )
                            //第二条记录
                            if (batchData.size > 1) {
                                println("   第二条记录: ${batchData[1].userName} " +
                                        "(${batchData[1].userId}) - 状态: ${batchData[1].status}")
                            }
                        }
                        println("导入批次 $currentBatch: $progressInfo, " +
                                "本批次: ${batchData.size} 条, " +
                                "用时: ${String.format("%.1f", elapsedTime)}s, " +
                                "内存: ${currentMemory}MB (${if (memoryDiff >= 0) "+" else ""}${memoryDiff}MB)")
                    }


                    lastMemoryCheck = currentMemory

                    // 模拟处理数据
                    processBatchData(batchData)

                    // 每25个批次详细检查内存
                    if (currentBatch % 25 == 0) {
                        val progressLabel = if (totalCount > 0) {
                            val progress = (processedCount * 100.0 / totalCount)
                            "导入批次 $currentBatch (${String.format("%.1f", progress)}%)"
                        } else {
                            "导入批次 $currentBatch (已处理${processedCount}条)"
                        }
                        MemoryMonitor.printMemoryUsage(progressLabel)
                    }
                }

                override fun onCompleted(totalCount: Long) {
                    val totalTime = (System.currentTimeMillis() - startTime) / 1000.0
                    MemoryMonitor.printMemoryUsage("百万级导入完成")
                    println("✅ 百万级导入完成: 总共处理 $totalProcessedData 条数据")
                    println("   总用时: ${String.format("%.2f", totalTime)}秒")
                    if (totalTime > 0) {
                        println("   平均速度: ${String.format("%.0f", totalProcessedData / totalTime)} 条/秒")
                    }
                }

                override fun onError(error: Throwable, processedCount: Long) {
                    MemoryMonitor.printMemoryUsage("百万级导入出错")
                    println("❌ 百万级导入失败: ${error.message}")

                    // 如果是XML解析错误，提供更详细的信息
                    if (error.message?.contains("XML") == true) {
                        println("   这通常是因为Excel文件在写入过程中损坏了")
                        println("   建议：1. 检查磁盘空间是否充足")
                        println("        2. 尝试减少批次���小（如5000）")
                        println("        3. 增加JVM堆内存")
                    }
                }
            }

            // 使用更小的批次进行导入以避免内存问题
            val importBatchSize = minOf(batchSize, 10000) // 导入使用更小的批次
            println("   导入批次大小: $importBatchSize")

            ExcelUtils.readBig(
                filePath = import!!,
                clazz = UserData::class.java,
                batchSize = importBatchSize,
                callback = importCallback,
                maxFileSize = 100 * 1024 * 1024
            )

        } catch (e: Exception) {
            println("❌ 百万级导入异常: ${e.message}")
            if (e.message?.contains("XML") == true) {
                println("   检测到XML解析错误，可能的原因：")
                println("   1. Excel文件在大数据量导出时损坏")
                println("   2. SXSSFWorkbook内存限制导致文件不完整")
                println("   3. 文件系统或磁盘空间问题")
                println("   建议使用��小的数据集测试（如10万条数据）")
            }
            e.printStackTrace()
        }

        MemoryMonitor.printMemoryUsage("百万级测试结束")
    }

    /**
     * 运行所有示例
     */
    @JvmStatic
    fun main(args: Array<String>) {
        println("=== ExcelBigUtils 内存占���测试 ===")
        println("JVM 参数信息:")
        println("  最大堆内存: ${Runtime.getRuntime().maxMemory() / 1024 / 1024}MB")
        println("  初始堆内存: ${Runtime.getRuntime().totalMemory() / 1024 / 1024}MB")
        println()

        // 初始内存状态
        MemoryMonitor.printMemoryUsage("程序启动")

        try {
            // 1. 测试导出内存占用 (小批次测试)
//            testExportMemoryUsage()
//
//            // 2. 测试导入内存占用 (小批次测试)
//            testImportMemoryUsage()

            // 3. 百万级数据测试 (大批次测试)
            println("\n" + "=".repeat(60))
            testMillionDataMemoryUsage()

        } catch (e: Exception) {
            println("测试异常: ${e.message}")
            e.printStackTrace()
        } finally {
            MemoryMonitor.printMemoryUsage("测试结束")
        }

        println("\n=== 内存测试完成 ===")
        println("测试总结:")
        println("  1. 小数据量测试: 2万条数据，批次100/1000/10000")
        println("  2. 百万级测试: 100万条数据，批次10000")
        println("  3. 可以对比不同批次大小和数据量的内存占用差异")
    }
}

package com.fanekit.common.util.io

import java.io.File
import java.nio.charset.StandardCharsets

/**
 * CsvUtil测试类
 */
fun main() {
    println("=== CsvUtil 测试开始 ===")

    // 创建测试目录
    val testDir = File("test_csv")
    if (!testDir.exists()) {
        testDir.mkdirs()
    }

    // 测试1: 创建和写入CSV文件
    println("\n1. 测试CSV写入功能")
    testWriteCsv(testDir)

    // 测试2: 读取CSV文件
    println("\n2. 测试CSV读取功能")
    testReadCsv(testDir)

    // 测试3: 使用表头的CSV操作
    println("\n3. 测试带表头的CSV操作")
    testCsvWithHeaders(testDir)

    // 测试4: CSV解析功能
    println("\n4. 测试CSV解析功能")
    testCsvParsing()

    // 测试5: CSV数据操作
    println("\n5. 测试CSV数据操作")
    testCsvDataOperations(testDir)

    // 测试6: CSV验证功能
    println("\n6. 测试CSV验证功能")
    testCsvValidation()

    // 测试7: CSV文件信息
    println("\n7. 测试CSV文件信息")
    testCsvInfo(testDir)

    // 测试8: CSV文件分割
    println("\n8. 测试CSV文件分割")
    testCsvSplit(testDir)

    // 清理测试文件
    println("\n9. 清理测试文件")
    cleanupTestFiles(testDir)

    println("\n=== CsvUtil 测试完成 ===")
}

/**
 * 测试CSV写入功能
 */
private fun testWriteCsv(testDir: File) {
    val csvFile = File(testDir, "test_write.csv")
    val data = listOf(
        listOf("姓名", "年龄", "城市"),
        listOf("张三", "25", "北京"),
        listOf("李四", "30", "上海"),
        listOf("王五", "28", "广州")
    )

    try {
        CsvUtil.writeCsv(csvFile.absolutePath, data)
        println("✓ CSV文件写入成功: ${csvFile.absolutePath}")

        // 验证文件是否存在
        if (csvFile.exists()) {
            println("✓ 文件已创建，大小: ${csvFile.length()} 字节")
        } else {
            println("✗ 文件创建失败")
        }
    } catch (e: Exception) {
        println("✗ CSV写入失败: ${e.message}")
    }
}

/**
 * 测试CSV读取功能
 */
private fun testReadCsv(testDir: File) {
    val csvFile = File(testDir, "test_write.csv")

    try {
        val data = CsvUtil.readCsv(csvFile.absolutePath)
        println("✓ CSV文件读取成功，共 ${data.size} 行")

        data.forEachIndexed { index, row ->
            println("  第${index + 1}行: ${row.joinToString(" | ")}")
        }
    } catch (e: Exception) {
        println("✗ CSV读取失败: ${e.message}")
    }
}

/**
 * 测试带表头的CSV操作
 */
private fun testCsvWithHeaders(testDir: File) {
    val csvFile = File(testDir, "test_headers.csv")
    val headers = listOf("name", "age", "city", "salary")
    val data = listOf(
        mapOf("name" to "张三", "age" to "25", "city" to "北京", "salary" to "8000"),
        mapOf("name" to "李四", "age" to "30", "city" to "上海", "salary" to "12000"),
        mapOf("name" to "王五", "age" to "28", "city" to "广州", "salary" to "10000")
    )

    try {
        // 写入带表头的CSV
        CsvUtil.writeCsvWithHeaders(csvFile.absolutePath, data, headers)
        println("✓ 带表头的CSV写入成功")

        // 读取带表头的CSV
        val readData = CsvUtil.readCsvWithHeaders(csvFile.absolutePath)
        println("✓ 带表头的CSV读取成功，共 ${readData.size} 条数据")

        readData.forEach { row ->
            println("  ${row["name"]} - ${row["age"]}岁 - ${row["city"]} - 薪资:${row["salary"]}")
        }
    } catch (e: Exception) {
        println("✗ 带表头CSV操作失败: ${e.message}")
    }
}

/**
 * 测试CSV解析功能
 */
private fun testCsvParsing() {
    val testLines = listOf(
        "张三,25,北京",
        "\"李四\",30,\"上海\"",
        "\"王五,工程师\",28,\"广州,天河区\"",
        "赵六,\"32\",深圳"
    )

    println("测试CSV行解析:")
    testLines.forEach { line ->
        try {
            val parsed = CsvUtil.parseCsvLine(line)
            println("  原始: $line")
            println("  解析: ${parsed.joinToString(" | ")}")
            println()
        } catch (e: Exception) {
            println("  解析失败: $line - ${e.message}")
        }
    }

    // 测试格式化
    println("测试CSV行格式化:")
    val fields = listOf("张三", "软件工程师,高级", "北京市,海淀区", "25")
    val formatted = CsvUtil.formatCsvLine(fields)
    println("  字段: ${fields.joinToString(" | ")}")
    println("  格式化: $formatted")
}

/**
 * 测试CSV数据操作
 */
private fun testCsvDataOperations(testDir: File) {
    val csvFile = File(testDir, "test_headers.csv")

    try {
        val data = CsvUtil.readCsvWithHeaders(csvFile.absolutePath)

        // 测试过滤
        val filtered = CsvUtil.filterCsvMaps(data) { row ->
            (row["age"]?.toIntOrNull() ?: 0) > 27
        }
        println("✓ 过滤年龄大于27的数据，结果: ${filtered.size} 条")
        filtered.forEach { row ->
            println("  ${row["name"]} - ${row["age"]}岁")
        }

        // 测试排序
        val sorted = CsvUtil.sortCsvMaps(data, "salary", false)
        println("✓ 按薪资降序排序:")
        sorted.forEach { row ->
            println("  ${row["name"]} - 薪资:${row["salary"]}")
        }

    } catch (e: Exception) {
        println("✗ CSV数据操作失败: ${e.message}")
    }
}

/**
 * 测试CSV验证功能
 */
private fun testCsvValidation() {
    val validData = listOf(
        listOf("name", "age", "city"),
        listOf("张三", "25", "北京"),
        listOf("李四", "30", "上海")
    )

    val invalidData = listOf(
        listOf("name", "age", "city"),
        listOf("张三", "25"),  // 缺少一列
        listOf("李四", "30", "上海", "extra")  // 多了一列
    )

    println("测试有效数据验证:")
    val validResult = CsvUtil.validateCsv(validData, 3, listOf("name", "age"))
    println("  验证结果: ${if (validResult.isValid) "✓ 有效" else "✗ 无效"}")
    if (!validResult.isValid) {
        validResult.errors.forEach { println("    错误: $it") }
    }

    println("测试无效数据验证:")
    val invalidResult = CsvUtil.validateCsv(invalidData, 3)
    println("  验证结果: ${if (invalidResult.isValid) "✓ 有效" else "✗ 无效"}")
    if (!invalidResult.isValid) {
        invalidResult.errors.forEach { println("    错误: $it") }
    }
}

/**
 * 测试CSV文件信息
 */
private fun testCsvInfo(testDir: File) {
    val csvFile = File(testDir, "test_headers.csv")

    try {
        val info = CsvUtil.getCsvInfo(csvFile.absolutePath)
        println("✓ CSV文件信息:")
        println("  数据行数: ${info.rowCount}")
        println("  列数: ${info.columnCount}")
        println("  表头: ${info.headers.joinToString(", ")}")
        println("  总单元格数: ${info.totalCells}")
    } catch (e: Exception) {
        println("✗ 获取CSV信息失败: ${e.message}")
    }
}

/**
 * 测试CSV文件分割
 */
private fun testCsvSplit(testDir: File) {
    // 首先创建一个较大的CSV文件
    val largeData = mutableListOf<List<String>>()
    largeData.add(listOf("id", "name", "value"))

    // 添加10行数据
    repeat(10) { i ->
        largeData.add(listOf("${i + 1}", "用户${i + 1}", "${(i + 1) * 100}"))
    }

    val largeCsvFile = File(testDir, "large_test.csv")

    try {
        CsvUtil.writeCsv(largeCsvFile.absolutePath, largeData)

        // 分割文件，每个文件3行数据
        val splitFiles = CsvUtil.splitCsvFile(
            largeCsvFile.absolutePath,
            testDir.absolutePath,
            3
        )

        println("✓ CSV文件分割成功，生成 ${splitFiles.size} 个文件:")
        splitFiles.forEach { filePath ->
            val file = File(filePath)
            val data = CsvUtil.readCsv(filePath)
            println("  ${file.name}: ${data.size} 行")
        }

    } catch (e: Exception) {
        println("✗ CSV文件分割失败: ${e.message}")
    }
}

/**
 * 清理测试文件
 */
private fun cleanupTestFiles(testDir: File) {
    try {
        if (testDir.exists()) {
            testDir.deleteRecursively()
            println("✓ 测试文件清理完成")
        }
    } catch (e: Exception) {
        println("✗ 清理测试文件失败: ${e.message}")
    }
}

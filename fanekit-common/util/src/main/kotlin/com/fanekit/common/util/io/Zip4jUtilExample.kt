package com.fanekit.common.util.io

import com.fanekit.common.util.io.Zip4jUtilExample.testDataDir
import com.fanekit.common.util.io.Zip4jUtilExample.testOutputDir
import net.lingala.zip4j.model.enums.CompressionLevel
import java.io.File

/**
 * Zip4jUtil使用示例
 * 展示如何使用带密码保护的ZIP压缩和解压功能
 */
object Zip4jUtilExample {

    // 测试目录配置
    private val testBaseDir = System.getProperty("java.io.tmpdir") + "/zip4j_test"
    private val testDataDir = "$testBaseDir/test_data"
    private val testOutputDir = "$testBaseDir/output"

    /**
     * 初始化测试环境，创建测试文件和目录
     */
    fun initTestEnvironment() {
        println("初始化测试环境...")

        // 创建测试目录
        File(testBaseDir).mkdirs()
        File(testDataDir).mkdirs()
        File(testOutputDir).mkdirs()

        // 创建测试文件
        createTestFiles()

        println("测试环境初始化完成:")
        println("  基础目录: $testBaseDir")
        println("  测试数据: $testDataDir")
        println("  输出目录: $testOutputDir")
    }

    /**
     * 创建测试文件
     */
    private fun createTestFiles() {
        // 创建文本文件
        File("$testDataDir/file1.txt").writeText("""
            这是测试文件1
            包含一些中文内容
            用于测试ZIP压缩功能
            时间: ${java.time.LocalDateTime.now()}
        """.trimIndent())

        File("$testDataDir/file2.txt").writeText("""
            This is test file 2
            Contains English content
            For ZIP compression testing
            Date: ${java.time.LocalDateTime.now()}
        """.trimIndent())

        // 创建敏感数据文件（用于密码保护测试）
        File("$testDataDir/sensitive_data.txt").writeText("""
            敏感信息文件
            银行账号: 1234567890
            密码: secret123
            身份证号: 123456789012345678
            
            此文件需要加密保护！
        """.trimIndent())

        // 创建子目录和文件
        val subDir = File("$testDataDir/subfolder")
        subDir.mkdirs()

        File("$subDir/nested_file.txt").writeText("""
            嵌套文件内容
            位于子目录中
            测试目录压缩功能
        """.trimIndent())

        // 创建空目录（测试空目录处理）
        File("$testDataDir/empty_folder").mkdirs()

        // 创建二进制文件
        val binaryFile = File("$testDataDir/binary_data.bin")
        binaryFile.writeBytes(ByteArray(1024) { (it % 256).toByte() })

        // 创建临时文件（用于过滤测试）
        File("$testDataDir/temp.tmp").writeText("临时文���，应该被���滤")
        File("$testDataDir/debug.log").writeText("日志文件，应该被过滤")
        File("$testDataDir/.hidden").writeText("隐藏文件，应该被过滤")
    }

    /**
     * 清理测试环境
     */
    fun cleanupTestEnvironment() {
        println("清理测试环境...")
        File(testBaseDir).deleteRecursively()
        println("测试环境清理完成")
    }

    /**
     * 基本压缩示例（无密码）
     */
    fun basicCompressionExample() {
        println("\n=== 基本压缩示例 ===")

        val sourceFiles = listOf(
            "$testDataDir/file1.txt",
            "$testDataDir/file2.txt",
            "$testDataDir/subfolder"
        )
        val zipPath = "$testOutputDir/basic_archive.zip"

        val success = Zip4jUtil.zipFiles(
            sourcePaths = sourceFiles,
            zipFilePath = zipPath,
            progressCallback = { current, total, currentFile ->
                println("压缩进度: $current/$total - ${File(currentFile).name}")
            }
        )

        if (success) {
            println("✓ 压缩完成: $zipPath")
            println("  文件大小: ${File(zipPath).length() / 1024} KB")
        } else {
            println("✗ 压缩失败")
        }
    }

    /**
     * 带密码压缩示例
     */
    fun passwordCompressionExample() {
        println("\n=== 带密码压缩示例 ===")

        val sourceFiles = listOf("$testDataDir/sensitive_data.txt")
        val zipPath = "$testOutputDir/protected_archive.zip"
        val password = "mySecretPassword123"

        // 创建压缩配置
        val config = Zip4jUtil.createCompressionConfig(
            password = password,
            compressionLevel = CompressionLevel.MAXIMUM,
            useStrongEncryption = true // 使用AES-256���密
        )

        val success = Zip4jUtil.zipFiles(
            sourcePaths = sourceFiles,
            zipFilePath = zipPath,
            config = config,
            progressCallback = { current, total, currentFile ->
                println("加密压缩进度: $current/$total - ${File(currentFile).name}")
            }
        )

        if (success) {
            println("✓ 加密压缩完成: $zipPath")
            println("  密码: $password")
            println("  文件大小: ${File(zipPath).length() / 1024} KB")
        } else {
            println("✗ 加密压缩失败")
        }
    }

    /**
     * 高级压缩配置示例
     */
    fun advancedCompressionExample() {
        println("\n=== 高级压缩配置示例 ===")

        val sourceDir = testDataDir
        val zipPath = "$testOutputDir/project_backup.zip"
        val password = "backup2024!"

        // 自定义压缩配置
        val config = Zip4jUtil.CompressionConfig(
            password = password,
            compressionLevel = CompressionLevel.FAST,
            includeEmptyDirs = false, // 不包含空目录
            excludeFileFilter = { file ->
                // 排除临���文件和日志文件
                file.name.endsWith(".tmp") ||
                file.name.endsWith(".log") ||
                file.name.startsWith(".")
            }
        )

        println("压缩配置:")
        println("  排除文件: *.tmp, *.log, .hidden")
        println("  排除空目录: 是")
        println("  压缩级别: 快速")

        val success = Zip4jUtil.zipFiles(
            sourcePaths = listOf(sourceDir),
            zipFilePath = zipPath,
            config = config,
            progressCallback = { current, total, currentFile ->
                if (current % 2 == 0 || current == total) {
                    println("项目备份进度: $current/$total")
                }
            }
        )

        if (success) {
            println("✓ 项目备份完成: $zipPath")
            println("  文件大小: ${File(zipPath).length() / 1024} KB")
        } else {
            println("✗ 项目备份失败")
        }
    }

    /**
     * 基本解压示例（无密码）
     */
    fun basicExtractionExample() {
        println("\n=== 基本解压示例 ===")

        val zipPath = "$testOutputDir/basic_archive.zip"
        val extractPath = "$testOutputDir/extracted_basic"

        if (!File(zipPath).exists()) {
            println("⚠ ZIP文件不存在，请先运行基本压缩示例")
            return
        }

        val success = Zip4jUtil.unzipFile(
            zipFilePath = zipPath,
            targetDirPath = extractPath,
            overwrite = true,
            progressCallback = { current, total, currentFile ->
                println("解压进度: $current/$total - $currentFile")
            }
        )

        if (success) {
            println("✓ 解压完成: $extractPath")
            val extractedFiles = File(extractPath).walk().filter { it.isFile }.count()
            println("  解压文件数: $extractedFiles")
        } else {
            println("✗ 解压失败")
        }
    }

    /**
     * 带密码解压示例
     */
    fun passwordExtractionExample() {
        println("\n=== 带密码解压示例 ===")

        val zipPath = "$testOutputDir/protected_archive.zip"
        val extractPath = "$testOutputDir/decrypted"
        val password = "mySecretPassword123"

        if (!File(zipPath).exists()) {
            println("⚠ 加密ZIP文件不存在，请先运行密码压缩示例")
            return
        }

        // 首先检查是否需要密码
        val isProtected = Zip4jUtil.isPasswordProtected(zipPath)
        println("ZIP文件是否受密码保护: $isProtected")

        if (isProtected) {
            // 验证密码
            val isValidPassword = Zip4jUtil.validatePassword(zipPath, password)
            println("密码验证结果: ${if (isValidPassword) "通过" else "失败"}")

            if (!isValidPassword) {
                println("✗ 密码错误！")

                // 演示错误密码的情况
                println("尝试使用错误密码...")
                try {
                    Zip4jUtil.unzipFile(zipPath, extractPath, "wrongPassword")
                } catch (e: Exception) {
                    println("预期错误: ${e.message}")
                }
                return
            }
        }

        val success = Zip4jUtil.unzipFile(
            zipFilePath = zipPath,
            targetDirPath = extractPath,
            password = password,
            overwrite = true,
            progressCallback = { current, total, currentFile ->
                println("解密解压进度: $current/$total - $currentFile")
            }
        )

        if (success) {
            println("✓ 解密解压完成: $extractPath")
            // 验证解压的文件内容
            val sensitiveFile = File("$extractPath/sensitive_data.txt")
            if (sensitiveFile.exists()) {
                println("  敏感文件已成功解密")
                println("  文件大小: ${sensitiveFile.length()} bytes")
            }
        } else {
            println("✗ 解密解压失败")
        }
    }

    /**
     * 列出ZIP文件内容示例
     */
    fun listZipContentsExample() {
        println("\n=== 列出ZIP文件内容示例 ===")

        val zipPaths = listOf(
            "$testOutputDir/basic_archive.zip" to null,
            "$testOutputDir/protected_archive.zip" to "mySecretPassword123"
        )

        zipPaths.forEach { (zipPath, password) ->
            val zipFile = File(zipPath)
            if (!zipFile.exists()) {
                println("⚠ ZIP文件不存在: ${zipFile.name}")
                return@forEach
            }

            println("\n分析文件: ${zipFile.name}")

            try {
                val entries = Zip4jUtil.listZipEntries(zipPath, password)

                println("ZIP文件内容:")
                println("-".repeat(60))
                entries.forEach { entry ->
                    val type = if (entry.isDirectory) "[目录]" else "[文件]"
                    val encrypted = if (entry.isEncrypted) "[加密]" else ""
                    val size = if (entry.isDirectory) "" else " (${entry.size} bytes)"

                    println("$type$encrypted ${entry.name}$size")
                }
                println("-".repeat(60))

                val fileCount = entries.count { !it.isDirectory }
                val dirCount = entries.count { it.isDirectory }
                val totalSize = entries.filter { !it.isDirectory }.sumOf { it.size }
                val encryptedCount = entries.count { it.isEncrypted }

                println("统计信��:")
                println("  总计: ${entries.size} 个条目")
                println("  文件: $fileCount 个")
                println("  目录: $dirCount 个")
                println("  总大小: ${totalSize / 1024} KB")
                println("  加密条目: $encryptedCount 个")

            } catch (e: Exception) {
                println("✗ 列出ZIP内容失败: ${e.message}")
            }
        }
    }

    /**
     * 解压单个文件示例
     */
    fun extractSingleFileExample() {
        println("\n=== 解压单个文件示例 ===")

        val zipPath = "$testOutputDir/basic_archive.zip"
        val entryName = "file1.txt"
        val outputPath = "$testOutputDir/extracted_single_file.txt"

        if (!File(zipPath).exists()) {
            println("⚠ ZIP文件不存在，请先运行基本压缩示例")
            return
        }

        val success = Zip4jUtil.extractSingleEntry(
            zipFilePath = zipPath,
            entryName = entryName,
            outputFilePath = outputPath
        )

        if (success) {
            println("✓ 单文件解压完成: $outputPath")
            val extractedFile = File(outputPath)
            if (extractedFile.exists()) {
                println("  文件大小: ${extractedFile.length()} bytes")
                println("  文件内容预览:")
                extractedFile.readText().lines().take(3).forEach { line ->
                    println("    $line")
                }
            }
        } else {
            println("✗ 单文件解压失败")
        }
    }

    /**
     * 完整的工作流示例
     */
    fun completeWorkflowExample() {
        println("\n=== 完整工作流示例 ===")

        val sourceDir = testDataDir // 使用测试数据目录
        val zipPath = "$testOutputDir/my_documents_backup.zip"
        val extractPath = "$testOutputDir/restored_documents"
        val password = "secureBackup2024!"

        println("工作流配置:")
        println("  源目录: $sourceDir")
        println("  备份文件: $zipPath")
        println("  恢复目录: $extractPath")
        println("  密码: $password")

        // 1. 创建加密备份（使用默认配置，不排除任何文件）
        println("\n1. 创建加密备份...")
        val config = Zip4jUtil.createCompressionConfig(
            password = password,
            compressionLevel = CompressionLevel.NORMAL,
            useStrongEncryption = true
        )

        val compressSuccess = Zip4jUtil.zipFiles(
            sourcePaths = listOf(sourceDir),
            zipFilePath = zipPath,
            config = config,
            progressCallback = { current, total, file ->
                if (current % 10 == 0 || current == total) {
                    println("  压缩进度: $current/$total")
                }
            }
        )

        if (!compressSuccess) {
            println("✗ 备份失败！")
            return
        }

        println("✓ 备份创建成功")
        println("  文件大小: ${File(zipPath).length() / 1024} KB")

        // 2. 验证备份文件
        println("\n2. 验证备份文件...")
        val isProtected = Zip4jUtil.isPasswordProtected(zipPath)
        val passwordValid = Zip4jUtil.validatePassword(zipPath, password)

        println("  文件受密码保护: $isProtected")
        println("  密码验证: ${if (passwordValid) "✓ 通过" else "✗ 失败"}")

        if (!passwordValid) {
            println("✗ 密码验证失败，工作流终止")
            return
        }

        // 3. 列出备份内容
        println("\n3. 备份内容预览...")
        try {
            val entries = Zip4jUtil.listZipEntries(zipPath, password)
            println("  备份包含 ${entries.size} 个条目")

            val fileCount = entries.count { !it.isDirectory }
            val dirCount = entries.count { it.isDirectory }
            val totalSize = entries.filter { !it.isDirectory }.sumOf { it.size }
            val encryptedCount = entries.count { it.isEncrypted }

            println("  文件: $fileCount 个")
            println("  目录: $dirCount 个")
            println("  加密条目: $encryptedCount 个")
            println("  总大小: ${totalSize / 1024} KB")

            // 显示前几个条目作为示例
            println("  条目示例:")
            entries.take(5).forEach { entry ->
                val type = if (entry.isDirectory) "[目录]" else "[文件]"
                val encrypted = if (entry.isEncrypted) "[加密]" else ""
                println("    $type$encrypted ${entry.name}")
            }
            if (entries.size > 5) {
                println("    ... 还有 ${entries.size - 5} 个条目")
            }
        } catch (e: Exception) {
            println("  ⚠ 无法读取备份内容: ${e.message}")
        }

        // 4. 恢复备份
        println("\n4. 恢复备份...")
        val extractSuccess = Zip4jUtil.unzipFile(
            zipFilePath = zipPath,
            targetDirPath = extractPath,
            password = password,
            overwrite = true,
            progressCallback = { current, total, file ->
                if (current % 5 == 0 || current == total) {
                    println("  恢复进度: $current/$total $file")
                }
            }
        )

        if (extractSuccess) {
            println("✓ 恢复完成")

            // 验证恢复的文件
            val restoredFileCount = File(extractPath).walk().filter { it.isFile }.count()
            println("  恢复文件数: $restoredFileCount")

            // 比较原始文件和恢复文件
            println("\n5. 验证数据完整性...")

            // 获取实际备份的文件列表（从ZIP文件中读取，只包含文件，不包含目录）
            val backedUpFiles = try {
                val entries = Zip4jUtil.listZipEntries(zipPath, password)
                entries.filter { !it.isDirectory }.map { it.name }.toSet()
            } catch (e: Exception) {
                println("  ⚠ 无法读取备份文件列表: ${e.message}")
                emptySet<String>()
            }

            if (backedUpFiles.isEmpty()) {
                println("  ✗ 无法验证数据完整性：无法读取备份文件列表")
                return
            }

            var verificationSuccess = true
            var verifiedCount = 0

            println("  验证已备份的文件:")

            // 遍历原始目录中的所有文件
            val originalFiles = File(sourceDir).walk().filter { it.isFile }.toList()

            // 获取源目录的名称，用于构建完整的ZIP路径
            val sourceDirName = File(sourceDir).name

            originalFiles.forEach { originalFile ->
                // 计算文件相对于源目录父级的路径（包含源目录名）
                val relativePath = "$sourceDirName/" + originalFile.relativeTo(File(sourceDir)).path.replace("\\", "/")

                // 检查该文件是否在备份中
                if (backedUpFiles.contains(relativePath)) {
                    val restoredFile = File(extractPath, relativePath)

                    if (restoredFile.exists()) {
                        val originalSize = originalFile.length()
                        val restoredSize = restoredFile.length()

                        if (originalSize == restoredSize) {
                            println("    ✓ ${originalFile.name} (${originalSize} bytes)")
                            verifiedCount++
                        } else {
                            println("    ✗ ${originalFile.name} 大小不匹配! 原始: $originalSize, 恢复: $restoredSize")
                            verificationSuccess = false
                        }
                    } else {
                        println("    ✗ ${originalFile.name} 备份了但未找到恢复文件")
                        verificationSuccess = false
                    }
                } else {
                    // 文件未备份，这在默认配置下不应该发生
                    println("    ⚠ ${originalFile.name} 未包含在备份中")
                }
            }

            // 检查备份中的所有文件是否都被正确恢复
            println("  检查备份文件的恢复情况:")
            var allBackedUpFilesRestored = true

            backedUpFiles.forEach { backedUpFile ->
                val restoredFile = File(extractPath, backedUpFile)
                if (!restoredFile.exists()) {
                    println("    ✗ 备份文件 $backedUpFile 未正确恢复")
                    verificationSuccess = false
                    allBackedUpFilesRestored = false
                }
            }

            if (allBackedUpFilesRestored) {
                println("    ✓ 所有备份文件都已正确恢复")
            }

            // 检查是否有多余的恢复文件
            val restoredFilePaths = File(extractPath).walk()
                .filter { it.isFile }
                .map { it.relativeTo(File(extractPath)).path.replace("\\", "/") }
                .toSet()

            val extraFiles = restoredFilePaths - backedUpFiles
            if (extraFiles.isNotEmpty()) {
                println("  ⚠ 发现额外的恢复文件:")
                extraFiles.forEach { println("    - $it") }
            }

            // 获取ZIP中的总条目数（包含目录）
            val totalZipEntries = try {
                val allEntries = Zip4jUtil.listZipEntries(zipPath, password)
                allEntries.size
            } catch (e: Exception) {
                0
            }

            // 输出验证结果
            println("\n  验证统计:")
            println("    原始文件总数: ${originalFiles.size}")
            println("    ZIP总条目数: $totalZipEntries (包含目录)")
            println("    备份文件数量: ${backedUpFiles.size} (仅文件)")
            println("    恢复文件数量: ${restoredFilePaths.size}")
            println("    验证通过文件: $verifiedCount")

            if (verificationSuccess && verifiedCount == backedUpFiles.size && verifiedCount == originalFiles.size) {
                println("✓ 数据完整性验证通过")
                println("\n=== 备份工作流完成 ===")
                println("✓ 备份文件: $zipPath")
                println("✓ 恢复目录: $extractPath")
                println("✓ 所有数据已成功备份和恢复")
            } else {
                if (!verificationSuccess) {
                    println("✗ 数据���整性验证失败：存在文件内容不匹配或恢复失败")
                } else if (verifiedCount < originalFiles.size) {
                    println("⚠ 部分验证完成：${verifiedCount}/${originalFiles.size} 原始文件验证通过")
                    println("  可能原因：某些原始文件未被包含在备份中")
                } else if (verifiedCount < backedUpFiles.size) {
                    println("⚠ 部分验证完成：${verifiedCount}/${backedUpFiles.size} 备份文件验证通过")
                    println("  可能原因：某些备份文件在原始目录中被删除或移动")
                } else {
                    println("✓ 所有文件验证通过")
                }
            }
        } else {
            println("✗ 恢复失败！")
        }
    }

    /**
     * 错误处理示例
     */
    fun errorHandlingExample() {
        println("\n=== 错误处理示例 ===")

        val testZipPath = "$testOutputDir/protected_archive.zip"
        val wrongPassword = "wrongPassword"
        val nonExistentZip = "$testOutputDir/non_existent.zip"

        println("演示各种错误情况:")

        // 1. 密码错误
        println("\n1. 测试密码错误:")
        if (File(testZipPath).exists()) {
            try {
                Zip4jUtil.unzipFile(testZipPath, "$testOutputDir/error_test", wrongPassword)
            } catch (e: Exception) {
                when {
                    e.message?.contains("密码错误") == true -> {
                        println("  ✓ 正确捕获密码错误: ${e.message}")
                    }
                    else -> {
                        println("  ⚠ 未预期的错误: ${e.message}")
                    }
                }
            }
        } else {
            println("  ⚠ 测试ZIP文件不存在，跳过密码错误测试")
        }

        // 2. 文件不存在
        println("\n2. 测试文件不存在:")
        try {
            Zip4jUtil.unzipFile(nonExistentZip, "$testOutputDir/error_test")
        } catch (e: Exception) {
            when {
                e.message?.contains("文件不存在") == true -> {
                    println("  ✓ 正确捕获文件不存在错误: ${e.message}")
                }
                else -> {
                    println("  ⚠ 未预期的错误: ${e.message}")
                }
            }
        }

        // 3. 无效ZIP文件
        println("\n3. 测试无效ZIP文件:")
        val invalidZipPath = "$testOutputDir/invalid.zip"
        try {
            // 创建一个无效的ZIP文件
            File(invalidZipPath).writeText("这不是一个有效的ZIP文件")

            Zip4jUtil.unzipFile(invalidZipPath, "$testOutputDir/error_test")
        } catch (e: Exception) {
            when {
                e.message?.contains("无效的ZIP文件") == true ||
                e.message?.contains("Invalid zip file") == true -> {
                    println("  ✓ 正确捕获无效ZIP文件错误: ${e.message}")
                }
                else -> {
                    println("  ⚠ 未预期的错误: ${e.message}")
                }
            }
        }

        // 4. 压缩不存在的文件
        println("\n4. 测试压缩不存在的文件:")
        try {
            Zip4jUtil.zipFiles(
                listOf("/path/to/non/existent/file.txt"),
                "$testOutputDir/error_test.zip"
            )
        } catch (e: Exception) {
            when {
                e.message?.contains("源文件不存在") == true -> {
                    println("  ✓ 正确捕获源文件不存在错误: ${e.message}")
                }
                else -> {
                    println("  ⚠ 未预期的错误: ${e.message}")
                }
            }
        }

        println("\n错误处理演示完成")
    }

    /**
     * 运行所有测试示例
     */
    fun runAllExamples() {
        println("=== Zip4jUtil 完整测试套件 ===")

        try {
            // 初始化测试环境
            initTestEnvironment()

            // 运行所有示例
            basicCompressionExample()
            passwordCompressionExample()
            advancedCompressionExample()
            listZipContentsExample()
            basicExtractionExample()
            passwordExtractionExample()
            extractSingleFileExample()
            completeWorkflowExample()
            errorHandlingExample()

            println("\n=== 测试完成 ===")
            println("所有测试文件位于: $testBaseDir")

        } catch (e: Exception) {
            println("�� 测试过程中出现错误: ${e.message}")
            e.printStackTrace()
        } finally {
            // 询问是否清理测试环境
            println("\n是否清理测试环境？(测试文件将被删除)")
            println("测试目录: $testBaseDir")
//            cleanupTestEnvironment() // 取消注释以自动清理
        }
    }
}

fun main() {
    // 运行完整的测试套件
    Zip4jUtilExample.runAllExamples()
}
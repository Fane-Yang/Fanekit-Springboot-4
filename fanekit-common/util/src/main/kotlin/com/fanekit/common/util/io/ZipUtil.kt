package com.fanekit.common.util.io

import com.fanekit.common.core.exception.Throw
import com.fanekit.common.core.constants.IOUtilConstants
import com.fanekit.common.core.security.IOSecurityValidator
import java.io.*
import java.util.zip.*

/**
 * ZIP压缩工具类
 * 提供ZIP文件的压缩和解压功能，支持安全检查和性能优化
 */
object ZipUtil {

    /**
     * 压缩单个文件
     */
    fun zipFile(sourceFilePath: String, zipFilePath: String, compressionLevel: Int = Deflater.DEFAULT_COMPRESSION): Boolean {
        return zipFiles(
            listOf(sourceFilePath),
            zipFilePath,
            compressionLevel
        )
    }

    /**
     * 压缩多个文件或目录为ZIP - 高级版本
     */
    fun zipFiles(
        sourcePaths: List<String>,
        zipFilePath: String,
        compressionLevel: Int = Deflater.DEFAULT_COMPRESSION,
        includeEmptyDirs: Boolean = true,
        progressCallback: ((current: Int, total: Int, currentFile: String) -> Unit)? = null
    ): Boolean {
        return IOSecurityValidator.safeOperation("压缩文件", zipFilePath) {
            if (sourcePaths.isEmpty()) {
                Throw.utilInvalidParam("源文件列表不能为空")
            }
            // 验证所有源路径的安全性
            sourcePaths.forEach { IOSecurityValidator.validatePathSecurity(it, "读取源文件") }
            IOSecurityValidator.validatePathSecurity(zipFilePath, "创建ZIP文件")

            val zipFile = File(zipFilePath)
            zipFile.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) {
                    Throw.utilOperationFailed("创建ZIP文件目录失败: ${parent.absolutePath}")
                }
            }

            // 收集所有要压缩的文件
            val filesToCompress = mutableListOf<Pair<File, String>>()
            sourcePaths.forEach { sourcePath ->
                val sourceFile = File(sourcePath)
                if (!sourceFile.exists()) {
                    Throw.utilFileNotFound("源文件不存在: $sourcePath")
                }
                collectFiles(
                    sourceFile,
                    sourceFile.name,
                    filesToCompress,
                    includeEmptyDirs
                )
            }

            val totalFiles = filesToCompress.size
            var processedFiles = 0

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile), IOUtilConstants.DEFAULT_BUFFER_SIZE)).use { zipOut ->
                zipOut.setLevel(compressionLevel)

                filesToCompress.forEach { (file, entryName) ->
                    progressCallback?.invoke(processedFiles, totalFiles, file.absolutePath)

                    if (file.isDirectory) {
                        addDirectoryEntry(entryName, zipOut)
                    } else {
                        addFileToZip(file, entryName, zipOut)
                    }

                    processedFiles++
                }
            }

            progressCallback?.invoke(totalFiles, totalFiles, "压缩完成")
            true
        }
    }

    /**
     * 收集要压缩的文件
     */
    private fun collectFiles(
        source: File,
        baseName: String,
        collector: MutableList<Pair<File, String>>,
        includeEmptyDirs: Boolean
    ) {
        if (source.isFile) {
            collector.add(source to baseName)
        } else if (source.isDirectory) {
            if (includeEmptyDirs) {
                collector.add(source to "$baseName/")
            }

            source.listFiles()?.forEach { child ->
                val childEntryName = "$baseName/${child.name}"
                collectFiles(
                    child,
                    childEntryName,
                    collector,
                    includeEmptyDirs
                )
            }
        }
    }

    /**
     * 解压ZIP文件 - 高级版本
     */
    fun unzipFile(
        zipFilePath: String,
        targetDirPath: String,
        overwrite: Boolean = true,
        progressCallback: ((current: Int, total: Int, currentFile: String) -> Unit)? = null
    ): Boolean {
        return IOSecurityValidator.safeOperation("解压文件", zipFilePath) {
            IOSecurityValidator.validatePathSecurity(zipFilePath, "读取ZIP文件")
            IOSecurityValidator.validatePathSecurity(targetDirPath, "写入解压目录")

            val zipFile = File(zipFilePath)
            IOSecurityValidator.validateFileExists(zipFile, "读取ZIP文件")

            val targetDir = File(targetDirPath)
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                Throw.utilOperationFailed("创建解压目录失败: $targetDirPath")
            }

            // 安全检查：获取总条目数
            val totalEntries = countZipEntries(zipFilePath)
            IOSecurityValidator.validateZipSecurity(entryCount = totalEntries)

            var extractedEntries = 0
            var totalExtractedSize = 0L

            ZipInputStream(BufferedInputStream(FileInputStream(zipFilePath), IOUtilConstants.DEFAULT_BUFFER_SIZE)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry

                while (entry != null) {
                    progressCallback?.invoke(extractedEntries, totalEntries, entry.name)

                    // 安全检查
                    IOSecurityValidator.validateZipSecurity(
                        singleFileSize = entry.size,
                        entryPath = entry.name
                    )

                    totalExtractedSize += entry.size
                    IOSecurityValidator.validateZipSecurity(totalSize = totalExtractedSize)

                    val file = File(targetDir, entry.name)

                    // 验证路径遍历攻击
                    IOSecurityValidator.validateZipPathTraversal(file, targetDir, entry.name)

                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        // 检查是否覆盖
                        if (file.exists() && !overwrite) {
                            zipIn.closeEntry()
                            entry = zipIn.nextEntry
                            continue
                        }

                        file.parentFile?.mkdirs()

                        // 使用优化的缓冲区大小
                        val bufferSize = IOSecurityValidator.getOptimalBufferSize(entry.size)

                        BufferedOutputStream(FileOutputStream(file), bufferSize).use { output ->
                            zipIn.copyTo(output, bufferSize)
                        }

                        // 设置文件最后修改时间
                        if (entry.time > 0) {
                            file.setLastModified(entry.time)
                        }
                    }

                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                    extractedEntries++
                }
            }

            progressCallback?.invoke(totalEntries, totalEntries, "解压完成")
            true
        }
    }

    /**
     * 计算ZIP文件中的条目数量
     */
    private fun countZipEntries(zipFilePath: String): Int {
        var count = 0
        ZipInputStream(FileInputStream(zipFilePath)).use { zipIn ->
            while (zipIn.nextEntry != null) {
                count++
                zipIn.closeEntry()
                if (count > IOUtilConstants.MAX_ZIP_ENTRIES) {
                    break // 提前终止，避免处理恶意文件
                }
            }
        }
        return count
    }

    /**
     * 添加文件到ZIP
     */
    private fun addFileToZip(file: File, entryName: String, zipOut: ZipOutputStream) {
        val entry = ZipEntry(entryName).apply {
            time = file.lastModified()
            size = file.length()
        }

        zipOut.putNextEntry(entry)

        val bufferSize = IOSecurityValidator.getOptimalBufferSize(file.length())
        BufferedInputStream(FileInputStream(file), bufferSize).use { input ->
            input.copyTo(zipOut, bufferSize)
        }

        zipOut.closeEntry()
    }

    /**
     * 添加目录条目到ZIP
     */
    private fun addDirectoryEntry(entryName: String, zipOut: ZipOutputStream) {
        val normalizedName = if (entryName.endsWith("/")) entryName else "$entryName/"
        val entry = ZipEntry(normalizedName).apply {
            time = System.currentTimeMillis()
        }
        zipOut.putNextEntry(entry)
        zipOut.closeEntry()
    }

    /**
     * 列出ZIP文件中的条目
     */
    fun listZipEntries(zipFilePath: String): List<ZipEntryInfo> {
        return IOSecurityValidator.safeOperation("列出ZIP条目", zipFilePath) {
            IOSecurityValidator.validatePathSecurity(zipFilePath, "读取ZIP文件")

            val zipFile = File(zipFilePath)
            IOSecurityValidator.validateFileExists(zipFile)

            val entries = mutableListOf<ZipEntryInfo>()

            ZipInputStream(FileInputStream(zipFilePath)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    entries.add(
                        ZipEntryInfo(
                            name = entry.name,
                            size = entry.size,
                            compressedSize = entry.compressedSize,
                            lastModified = entry.time,
                            isDirectory = entry.isDirectory,
                            crc = entry.crc,
                            method = entry.method
                        )
                    )
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            entries
        }
    }

    /**
     * 解压ZIP文件中的单个条目
     */
    fun extractSingleEntry(zipFilePath: String, entryName: String, outputFilePath: String): Boolean {
        return IOSecurityValidator.safeOperation("解压单个条目", zipFilePath) {
            IOSecurityValidator.validatePathSecurity(zipFilePath, "读取ZIP文件")
            IOSecurityValidator.validatePathSecurity(outputFilePath, "写入文件")

            val zipFile = File(zipFilePath)
            IOSecurityValidator.validateFileExists(zipFile)

            var found = false

            ZipInputStream(FileInputStream(zipFilePath)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry

                while (entry != null && !found) {
                    if (entry.name == entryName && !entry.isDirectory) {
                        val outputFile = File(outputFilePath)
                        outputFile.parentFile?.mkdirs()

                        BufferedOutputStream(FileOutputStream(outputFile)).use { output ->
                            zipIn.copyTo(output)
                        }

                        // 设置文件最后修改时间
                        if (entry.time > 0) {
                            outputFile.setLastModified(entry.time)
                        }

                        found = true
                    }

                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            if (!found) {
                Throw.utilFileNotFound("ZIP文件中不存在条目: $entryName")
            }

            true
        }
    }

    /**
     * 验证ZIP文件完整性
     */
    fun validateZipFile(zipFilePath: String): ZipValidationResult {
        return IOSecurityValidator.safeOperation("验证ZIP文件", zipFilePath) {
            IOSecurityValidator.validatePathSecurity(zipFilePath, "读取ZIP文件")

            val zipFile = File(zipFilePath)
            IOSecurityValidator.validateFileExists(zipFile)

            var entryCount = 0
            var totalSize = 0L
            var totalCompressedSize = 0L
            val errors = mutableListOf<String>()

            try {
                ZipInputStream(FileInputStream(zipFilePath)).use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry

                    while (entry != null) {
                        entryCount++
                        totalSize += entry.size
                        totalCompressedSize += entry.compressedSize

                        // 验证条目名称
                        try {
                            IOSecurityValidator.validatePathSecurity(entry.name, "验证条目")
                        } catch (e: Exception) {
                            errors.add("不安全的条目名称: ${entry.name}")
                        }

                        // 检查大小限制
                        if (entry.size > IOUtilConstants.MAX_ZIP_SINGLE_FILE_SIZE) {
                            errors.add("条目过大: ${entry.name} (${entry.size} bytes)")
                        }

                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            } catch (e: Exception) {
                errors.add("ZIP文件损坏: ${e.message}")
            }

            // 检查整体限制
            if (entryCount > IOUtilConstants.MAX_ZIP_ENTRIES) {
                errors.add("条目数量过多: $entryCount")
            }

            if (totalSize > IOUtilConstants.MAX_ZIP_EXTRACTED_SIZE) {
                errors.add("解压后总大小过大: $totalSize bytes")
            }

            ZipValidationResult(
                isValid = errors.isEmpty(),
                entryCount = entryCount,
                totalSize = totalSize,
                totalCompressedSize = totalCompressedSize,
                compressionRatio = if (totalSize > 0) totalCompressedSize.toDouble() / totalSize else 0.0,
                errors = errors
            )
        }
    }

    /**
     * ZIP条目信息数据类
     */
    data class ZipEntryInfo(
        val name: String,
        val size: Long,
        val compressedSize: Long,
        val lastModified: Long,
        val isDirectory: Boolean,
        val crc: Long,
        val method: Int
    )

    /**
     * ZIP验证结果数据类
     */
    data class ZipValidationResult(
        val isValid: Boolean,
        val entryCount: Int,
        val totalSize: Long,
        val totalCompressedSize: Long,
        val compressionRatio: Double,
        val errors: List<String>
    )

    /**
     * 测试ZIP工具的主方法
     */
    @JvmStatic
    fun main(args: Array<String>) {
        println("=== ZipUtil 安全性和功能测试 ===\n")

        try {
            // 测试1：创建测试文件
            println("1. 创建测试文件:")
            val testDir = "zip_test_${System.currentTimeMillis()}"
            File(testDir).mkdirs()

            val testFiles = listOf("test1.txt", "test2.txt", "subdir/test3.txt")
            testFiles.forEach { fileName ->
                val file = File(testDir, fileName)
                file.parentFile?.mkdirs()
                file.writeText("这是测试文件内容: $fileName\n".repeat(100))
                println("  创建文件: ${file.absolutePath}")
            }

            // 测试2：压缩文件
            println("\n2. 测试压缩功能:")
            val zipFile = "${testDir}.zip"
            val success = zipFiles(
                sourcePaths = listOf(testDir),
                zipFilePath = zipFile,
                progressCallback = { current, total, file ->
                    println("  压缩进度: $current/$total - $file")
                }
            )
            println("  压缩结果: ${if (success) "✓" else "✗"}")

            // 测试3：验证ZIP文件
            println("\n3. 验证ZIP文件:")
            val validation = validateZipFile(zipFile)
            println("  文件有效: ${validation.isValid}")
            println("  条目数量: ${validation.entryCount}")
            println("  压缩比: ${"%.2f".format(validation.compressionRatio * 100)}%")

            // 测试4：列出ZIP条目
            println("\n4. 列出ZIP条目:")
            val entries = listZipEntries(zipFile)
            entries.forEach { entry ->
                println("  ${entry.name} - ${entry.size} bytes")
            }

            // 测试5：解压文件
            println("\n5. 测试解压功能:")
            val extractDir = "${testDir}_extracted"
            val extractSuccess = unzipFile(
                zipFilePath = zipFile,
                targetDirPath = extractDir,
                progressCallback = { current, total, file ->
                    println("  解压进度: $current/$total - $file")
                }
            )
            println("  解压结果: ${if (extractSuccess) "✓" else "✗"}")

            // 清理测试文件
            println("\n6. 清理测试文件:")
            File(testDir).deleteRecursively()
            File(zipFile).delete()
            File(extractDir).deleteRecursively()
            println("  清理完成")

        } catch (ex: Exception) {
            println("  ✗ 测试失败: ${ex.message}")
            ex.printStackTrace()
        }

        println("\n=== 测试完成 ===")
    }
}

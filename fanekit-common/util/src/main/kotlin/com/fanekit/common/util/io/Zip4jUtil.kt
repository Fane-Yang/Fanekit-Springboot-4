package com.fanekit.common.util.io

import com.fanekit.common.core.exception.Throw
import com.fanekit.common.core.security.IOSecurityValidator
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import net.lingala.zip4j.progress.ProgressMonitor
import java.io.File

/**
 * 基于Zip4j的ZIP压缩工具类
 * 提供支持密码保护的ZIP文件压缩和解压功能，包含安全检查和性能优化
 *
 * 主要功能：
 * - 带密码的文件/目录压缩
 * - 带密码的ZIP文件解压
 * - AES加密支持
 * - 进度回调
 * - 安全验证
 */
object Zip4jUtil {

    /**
     * 系统路径分隔符，在类初始化时自动获取
     */
    private val systemSeparator: String = File.separator

    /**
     * ZIP文件内部使用的路径分隔符（始终为 /）
     */
    private const val ZIP_SEPARATOR = "/"

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
        val isEncrypted: Boolean
    )

    /**
     * 压缩配置类
     */
    data class CompressionConfig(
        val password: String? = null,
        val compressionLevel: CompressionLevel = CompressionLevel.NORMAL,
        val encryptionMethod: EncryptionMethod = EncryptionMethod.AES,
        val aesKeyStrength: AesKeyStrength = AesKeyStrength.KEY_STRENGTH_256,
        val includeEmptyDirs: Boolean = true,
        val excludeFileFilter: ((File) -> Boolean)? = null
    )

    /**
     * 压缩单个文件（带密码保护）
     */
    fun zipFile(
        sourceFilePath: String,
        zipFilePath: String,
        config: CompressionConfig = CompressionConfig()
    ): Boolean {
        return zipFiles(listOf(sourceFilePath), zipFilePath, config)
    }

    /**
     * 压缩多个文件或目录为ZIP（带密码保护）
     */
    fun zipFiles(
        sourcePaths: List<String>,
        zipFilePath: String,
        config: CompressionConfig = CompressionConfig(),
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

            try {
                val zip4jFile = ZipFile(zipFile)

                // 设置密码（如果提供）
                config.password?.let { password ->
                    zip4jFile.setPassword(password.toCharArray())
                }

                // 收集所有要压缩的文件，同时记录基础路径用于计算相对路径
                val filesToCompress = mutableListOf<Pair<File, String>>() // File 和其在ZIP中的路径
                sourcePaths.forEach { sourcePath ->
                    val sourceFile = File(sourcePath)
                    if (!sourceFile.exists()) {
                        Throw.utilFileNotFound("源文件不存在: $sourcePath")
                    }

                    if (sourceFile.isFile) {
                        // 如果是文件，直接添加到ZIP根目录
                        if (config.excludeFileFilter?.invoke(sourceFile) != true) {
                            filesToCompress.add(sourceFile to sourceFile.name)
                        }
                    } else if (sourceFile.isDirectory) {
                        // 如果是目录，保留目录结构，包含目录本身
                        val parentDir = sourceFile.parentFile ?: sourceFile
                        collectFilesWithPath(sourceFile, parentDir, filesToCompress, config)
                    }
                }

                val totalFiles = filesToCompress.size
                var processedFiles = 0

                // 设置进度监听器
                val progressMonitor = zip4jFile.progressMonitor

                // 添加文件到ZIP
                filesToCompress.forEach { (file, zipPath) ->
                    progressCallback?.invoke(processedFiles, totalFiles, file.absolutePath)

                    try {
                        // 为每个文件单独配置ZIP参数
                        val fileZipParameters = ZipParameters().apply {
                            compressionMethod = CompressionMethod.DEFLATE
                            compressionLevel = config.compressionLevel
                            fileNameInZip = zipPath // 设置文件在ZIP中的路径

                            // 如果有密码，启用加密
                            config.password?.let {
                                isEncryptFiles = true
                                encryptionMethod = config.encryptionMethod
                                if (config.encryptionMethod == EncryptionMethod.AES) {
                                    aesKeyStrength = config.aesKeyStrength
                                }
                            }
                        }

                        if (file.isDirectory && config.includeEmptyDirs) {
                            // 对于目录，确保路径以 / 结尾
                            fileZipParameters.fileNameInZip = if (zipPath.endsWith(ZIP_SEPARATOR)) zipPath else "$zipPath$ZIP_SEPARATOR"
                            zip4jFile.addFolder(file, fileZipParameters)
                        } else if (file.isFile) {
                            zip4jFile.addFile(file, fileZipParameters)
                        }

                        // 等待当前操作完成
                        while (progressMonitor.state == ProgressMonitor.State.BUSY) {
                            Thread.sleep(10)
                        }

                    } catch (e: ZipException) {
                        Throw.utilOperationFailed("压缩文件失败: ${file.absolutePath}, 错误: ${e.message}")
                    }

                    processedFiles++
                }

                progressCallback?.invoke(totalFiles, totalFiles, "压缩完成")
                true

            } catch (e: ZipException) {
                Throw.utilOperationFailed("创建ZIP文件失败: ${e.message}")
            } catch (e: Exception) {
                Throw.utilOperationFailed("压缩操作失败: ${e.message}")
            }
        }
    }

    /**
     * 解压ZIP文��（支持密码保护）
     */
    fun unzipFile(
        zipFilePath: String,
        targetDirPath: String,
        password: String? = null,
        overwrite: Boolean = true,
        progressCallback: ((current: Int, total: Int, currentFile: String) -> Unit)? = null
    ): Boolean {
        return IOSecurityValidator.safeOperation("解压文件", zipFilePath) {
            IOSecurityValidator.validatePathSecurity(zipFilePath, "读取ZIP文件")
            IOSecurityValidator.validatePathSecurity(targetDirPath, "写入解压目录")

            val zipFileObj = File(zipFilePath)
            IOSecurityValidator.validateFileExists(zipFileObj, "读取ZIP文件")

            val targetDir = File(targetDirPath)
            if (!targetDir.exists() && !targetDir.mkdirs()) {
                Throw.utilOperationFailed("创建解压目录失败: $targetDirPath")
            }

            try {
                val zip4jFile = ZipFile(zipFileObj)

                // 验证ZIP文件是否有效
                if (!zip4jFile.isValidZipFile) {
                    Throw.utilOperationFailed("无效的ZIP文件: $zipFilePath")
                }

                // 设置密码（如果提供）
                password?.let { pwd ->
                    zip4jFile.setPassword(pwd.toCharArray())
                }

                // 检查是否需要密码
                if (zip4jFile.isEncrypted && password == null) {
                    Throw.utilOperationFailed("ZIP文件已加密，需要提供密码")
                }

                // 获取文件头信息进行安全检查
                val fileHeaders = zip4jFile.fileHeaders
                val totalEntries = fileHeaders.size
                IOSecurityValidator.validateZipSecurity(entryCount = totalEntries)

                var totalExtractedSize = 0L
                fileHeaders.forEach { header ->
                    IOSecurityValidator.validateZipSecurity(
                        singleFileSize = header.uncompressedSize,
                        entryPath = header.fileName
                    )
                    totalExtractedSize += header.uncompressedSize
                }
                IOSecurityValidator.validateZipSecurity(totalSize = totalExtractedSize)

                var extractedEntries = 0
                val progressMonitor = zip4jFile.progressMonitor

                // 解压所有文件
                fileHeaders.forEach { header ->
                    progressCallback?.invoke(extractedEntries, totalEntries, header.fileName)

                    val outputFile = File(targetDir, header.fileName)

                    // 验证路径遍历攻击
                    IOSecurityValidator.validateZipPathTraversal(outputFile, targetDir, header.fileName)

                    // 检查是否覆盖
                    if (outputFile.exists() && !overwrite && !header.isDirectory) {
                        extractedEntries++
                        return@forEach
                    }

                    try {
                        if (header.isDirectory) {
                            outputFile.mkdirs()
                        } else {
                            // 确保父目录存在
                            outputFile.parentFile?.mkdirs()

                            // 解压单个文件
                            zip4jFile.extractFile(header, targetDirPath)

                            // 等待解压完成
                            while (progressMonitor.state == ProgressMonitor.State.BUSY) {
                                Thread.sleep(10)
                            }
                        }
                    } catch (e: ZipException) {
                        if (e.message?.contains("Wrong password") == true) {
                            Throw.utilOperationFailed("密码错误，无法解压文件")
                        } else {
                            Throw.utilOperationFailed("解压文件失败: ${header.fileName}, 错误: ${e.message}")
                        }
                    }

                    extractedEntries++
                }

                progressCallback?.invoke(totalEntries, totalEntries, "解压完成")
                true

            } catch (e: ZipException) {
                if (e.message?.contains("Wrong password") == true) {
                    Throw.utilOperationFailed("密码��误，无法解压ZIP文件")
                } else {
                    Throw.utilOperationFailed("解压ZIP文件失败: ${e.message}")
                }
            } catch (e: Exception) {
                Throw.utilOperationFailed("解压操作失败: ${e.message}")
            }
        }
    }

    /**
     * 列出ZIP文件中的条目（支持加密ZIP）
     */
    fun listZipEntries(zipFilePath: String, password: String? = null): List<ZipEntryInfo> {
        return IOSecurityValidator.safeOperation("列出ZIP条目", zipFilePath) {
            IOSecurityValidator.validatePathSecurity(zipFilePath, "读取ZIP文件")

            val zipFile = File(zipFilePath)
            IOSecurityValidator.validateFileExists(zipFile)

            try {
                val zip4jFile = ZipFile(zipFile)

                // 验证ZIP文���是否有效
                if (!zip4jFile.isValidZipFile) {
                    Throw.utilOperationFailed("无效的ZIP文件: $zipFilePath")
                }

                // 设置密码（如果提供）
                password?.let { pwd ->
                    zip4jFile.setPassword(pwd.toCharArray())
                }

                val entries = mutableListOf<ZipEntryInfo>()
                val fileHeaders = zip4jFile.fileHeaders

                fileHeaders.forEach { header ->
                    entries.add(
                        ZipEntryInfo(
                            name = header.fileName,
                            size = header.uncompressedSize,
                            compressedSize = header.compressedSize,
                            lastModified = header.lastModifiedTime,
                            isDirectory = header.isDirectory,
                            crc = header.crc,
                            isEncrypted = header.isEncrypted
                        )
                    )
                }

                entries

            } catch (e: ZipException) {
                Throw.utilOperationFailed("读取ZIP文件失败: ${e.message}")
            } catch (e: Exception) {
                Throw.utilOperationFailed("列出ZIP条目失败: ${e.message}")
            }
        }
    }

    /**
     * 解压ZIP文件中的单个条目（支持密码保护）
     */
    fun extractSingleEntry(
        zipFilePath: String,
        entryName: String,
        outputFilePath: String,
        password: String? = null
    ): Boolean {
        return IOSecurityValidator.safeOperation("解压单个条目", zipFilePath) {
            IOSecurityValidator.validatePathSecurity(zipFilePath, "读取ZIP文件")
            IOSecurityValidator.validatePathSecurity(outputFilePath, "写入文件")

            val zipFile = File(zipFilePath)
            IOSecurityValidator.validateFileExists(zipFile)

            try {
                val zip4jFile = ZipFile(zipFile)

                // 验证ZIP文件是否有效
                if (!zip4jFile.isValidZipFile) {
                    Throw.utilOperationFailed("无效的ZIP文件: $zipFilePath")
                }

                // 设置密码（如果提供）
                password?.let { pwd ->
                    zip4jFile.setPassword(pwd.toCharArray())
                }

                // 查找指定条目
                val header = zip4jFile.getFileHeader(entryName)
                    ?: Throw.utilFileNotFound("ZIP文件中未找到条目: $entryName")

                // 安全检查
                IOSecurityValidator.validateZipSecurity(
                    singleFileSize = header.uncompressedSize,
                    entryPath = header.fileName
                )

                val outputFile = File(outputFilePath)
                val targetDir = outputFile.parentFile ?: File(".")

                // 验证路径遍历攻击
                IOSecurityValidator.validateZipPathTraversal(outputFile, targetDir, entryName)

                // 确保父目录存在
                outputFile.parentFile?.mkdirs()

                // 解压文件
                zip4jFile.extractFile(header, outputFile.parent, outputFile.name)

                // 等待解压完成
                val progressMonitor = zip4jFile.progressMonitor
                while (progressMonitor.state == ProgressMonitor.State.BUSY) {
                    Thread.sleep(10)
                }

                true

            } catch (e: ZipException) {
                if (e.message?.contains("Wrong password") == true) {
                    Throw.utilOperationFailed("密码错误，无法解压文件")
                } else {
                    Throw.utilOperationFailed("解压条目失败: ${e.message}")
                }
            } catch (e: Exception) {
                Throw.utilOperationFailed("解压操作失败: ${e.message}")
            }
        }
    }

    /**
     * 检查ZIP文件是否受密码保护
     */
    fun isPasswordProtected(zipFilePath: String): Boolean {
        return IOSecurityValidator.safeOperation("检查ZIP密码保护", zipFilePath) {
            IOSecurityValidator.validatePathSecurity(zipFilePath, "读取ZIP文件")

            val zipFile = File(zipFilePath)
            IOSecurityValidator.validateFileExists(zipFile)

            try {
                val zip4jFile = ZipFile(zipFile)

                if (!zip4jFile.isValidZipFile) {
                    Throw.utilOperationFailed("无效的ZIP文件: $zipFilePath")
                }

                zip4jFile.isEncrypted

            } catch (e: ZipException) {
                Throw.utilOperationFailed("读取ZIP文件失败: ${e.message}")
            } catch (e: Exception) {
                Throw.utilOperationFailed("检查ZIP文件失败: ${e.message}")
            }
        }
    }

    /**
     * 验证ZIP文件密码是否正确
     */
    fun validatePassword(zipFilePath: String, password: String): Boolean {
        return try {
            IOSecurityValidator.safeOperation("验证ZIP密码", zipFilePath) {
                IOSecurityValidator.validatePathSecurity(zipFilePath, "读取ZIP文件")

                val zipFile = File(zipFilePath)
                IOSecurityValidator.validateFileExists(zipFile)

                val zip4jFile = ZipFile(zipFile)

                if (!zip4jFile.isValidZipFile) {
                    return@safeOperation false
                }

                if (!zip4jFile.isEncrypted) {
                    return@safeOperation true // 无密码保护
                }

                zip4jFile.setPassword(password.toCharArray())

                // 尝试读取第一个文件条目来验证密码
                val fileHeaders = zip4jFile.fileHeaders
                if (fileHeaders.isNotEmpty()) {
                    val firstHeader = fileHeaders.first { !it.isDirectory }
                    try {
                        zip4jFile.getInputStream(firstHeader).use {
                            it.read() // 尝试读取一个字节
                        }
                        true
                    } catch (e: ZipException) {
                        false
                    }
                } else {
                    true // 空ZIP文件
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 收集要压缩的文件
     */
    private fun collectFiles(
        source: File,
        collector: MutableList<File>,
        config: CompressionConfig
    ) {
        if (source.isFile) {
            // 应用文件过滤器
            if (config.excludeFileFilter?.invoke(source) != true) {
                collector.add(source)
            }
        } else if (source.isDirectory) {
            if (config.includeEmptyDirs) {
                collector.add(source)
            }

            source.listFiles()?.forEach { child ->
                collectFiles(child, collector, config)
            }
        }
    }

    /**
     * 收集要压缩的文件及其路径
     */
    private fun collectFilesWithPath(
        source: File,
        baseDir: File,
        collector: MutableList<Pair<File, String>>,
        config: CompressionConfig
    ) {
        if (source.isFile) {
            // 应用文件过滤器
            if (config.excludeFileFilter?.invoke(source) != true) {
                // 计算相对于基础目录的路径，并转换为ZIP标准分隔符
                val relativePath = baseDir.toPath().relativize(source.toPath()).toString()
                    .replace(systemSeparator, ZIP_SEPARATOR)
                collector.add(source to relativePath)
            }
        } else if (source.isDirectory) {
            // 对于空目录，如果需要包含的话
            if (config.includeEmptyDirs && (source.listFiles()?.isEmpty() == true)) {
                val relativePath = baseDir.toPath().relativize(source.toPath()).toString()
                    .replace(systemSeparator, ZIP_SEPARATOR)
                collector.add(source to "$relativePath$ZIP_SEPARATOR")
            }

            // 递归处理子目录和文件
            source.listFiles()?.forEach { child ->
                collectFilesWithPath(child, baseDir, collector, config)
            }
        }
    }

    /**
     * 创建默认的压缩配置
     */
    fun createCompressionConfig(
        password: String? = null,
        compressionLevel: CompressionLevel = CompressionLevel.NORMAL,
        useStrongEncryption: Boolean = true
    ): CompressionConfig {
        return CompressionConfig(
            password = password,
            compressionLevel = compressionLevel,
            encryptionMethod = if (useStrongEncryption) EncryptionMethod.AES else EncryptionMethod.ZIP_STANDARD,
            aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
        )
    }
}

//fun main() {
//     val testBaseDir = System.getProperty("java.io.tmpdir") + "/zip4j_test"
//     val testDataDir = "${testBaseDir}/test_data"
//     val testOutputDir = "${testBaseDir}/output"
//
//    val sourceDir = testDataDir // 使用测试数据目录
//    val zipPath = "${testOutputDir}/my_documents_backup.zip"
//    val extractPath = "${testOutputDir}/restored_documents"
//
//    println("工作流配置:")
//    println("  源目录: $sourceDir")
//    println("  备份文件: $zipPath")
//    println("  恢复目录: $extractPath")
//
//    // 1. 创建加密备份（使用默认配置，不排除任何文件）
//    println("\n1. 创建备份...")
//
//    val compressSuccess = ZipUtil.zipFiles(
//        sourcePaths = listOf(sourceDir),
//        zipFilePath = zipPath,
//        progressCallback = { current, total, file ->
//            if (current % 10 == 0 || current == total) {
//                println("  压缩进度: $current/$total")
//            }
//        }
//    )
//
//    if (!compressSuccess) {
//        println("✗ 备份失败！")
//        return
//    }
//
//    println("✓ 备份创建成功")
//    println("  文件大小: ${File(zipPath).length() / 1024} KB")
//
//    // 4. 恢复备份
//    println("\n4. 恢复备份...")
//    val extractSuccess = ZipUtil.unzipFile(
//        zipFilePath = zipPath,
//        targetDirPath = extractPath,
//        overwrite = true,
//        progressCallback = { current, total, file ->
//            if (current % 5 == 0 || current == total) {
//                println("  恢复进度: $current/$total $file")
//            }
//        }
//    )
//    println("\n恢复备份结果 $extractSuccess")
//}

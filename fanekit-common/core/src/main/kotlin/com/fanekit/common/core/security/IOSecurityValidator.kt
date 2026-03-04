package com.fanekit.common.core.security

import com.fanekit.common.core.exception.Throw
import com.fanekit.common.core.constants.IOUtilConstants
import java.io.File
import java.nio.file.Paths

/**
 * 文件安全检查核心工具类
 * 统一管理所有文件操作的安全验证逻辑
 * 提供路径验证、文件大小验证、ZIP和CSV安全性验证等功能
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/5/24
 * @since 1.0.0
 * @version 1.0.0
 */
object IOSecurityValidator {

    /**
     * 危险路径模式
     */
    private val DANGEROUS_PATH_PATTERNS = arrayOf(
        "..",
        "~",
        "\$",
        "|",
        "&",
        ";",
        "`",
        "\\x00"
    )

    /**
     * Windows保留文件名
     */
    private val WINDOWS_RESERVED_NAMES = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )

    /**
     * 是否为Windows系统
     */
    private val isWindows = System.getProperty("os.name").lowercase().contains("windows")

    /**
     * 验证路径安全性
     * @param path 要验证的路径
     * @param operation 操作描述（用于错误信息）
     * @throws UtilException 当路径不安全时抛出异常
     */
    fun validatePathSecurity(path: String, operation: String = "文件操作") {
        // 检查空路径
        if (path.isBlank()) {
            Throw.utilInvalidParam("${operation}路径不能为空")
        }

        // 规范化路径
        val normalizedPath = try {
            Paths.get(path).normalize().toString()
        } catch (e: Exception) {
            Throw.utilInvalidParam("无效的路径格式: $path", e)
        }

        // 检查危险模式
        DANGEROUS_PATH_PATTERNS.forEach { pattern ->
            if (normalizedPath.contains(pattern, ignoreCase = true)) {
                Throw.utilUnsafeOperation("路径包含不安全字符或模式: $path")
            }
        }

        // 检查路径遍历攻击
        if (containsPathTraversal(normalizedPath)) {
            Throw.utilUnsafeOperation("检测到路径遍历攻击: $path")
        }

        // Windows平台检查
        if (isWindows) {
            validateWindowsPath(normalizedPath, path)
        }

        // 检查文件名长度
        validatePathLength(normalizedPath, path)
    }

    /**
     * 检查是否包含路径遍历攻击
     */
    private fun containsPathTraversal(normalizedPath: String): Boolean {
        return normalizedPath.contains("..") ||
               (normalizedPath.startsWith("/") && normalizedPath.contains("../"))
    }

    /**
     * 验证Windows路径
     */
    private fun validateWindowsPath(normalizedPath: String, originalPath: String) {
        val fileName = File(normalizedPath).name.uppercase()
        if (WINDOWS_RESERVED_NAMES.contains(fileName) ||
            WINDOWS_RESERVED_NAMES.any { fileName.startsWith("$it.") }) {
            Throw.utilUnsafeOperation("路径使用了Windows保留名称: $originalPath")
        }
    }

    /**
     * 验证路径长度
     */
    private fun validatePathLength(normalizedPath: String, originalPath: String) {
        // 检查文件名长度
        val fileName = File(normalizedPath).name
        if (fileName.length > IOUtilConstants.MAX_FILENAME_LENGTH) {
            Throw.utilInvalidParam("文件名过长(超过${IOUtilConstants.MAX_FILENAME_LENGTH}字符): $fileName")
        }

        // 检查路径总长度
        val maxPathLength = if (isWindows) {
            IOUtilConstants.MAX_PATH_LENGTH_WINDOWS
        } else {
            IOUtilConstants.MAX_PATH_LENGTH_UNIX
        }

        if (normalizedPath.length > maxPathLength) {
            Throw.utilInvalidParam("路径过长(超过${maxPathLength}字符): $originalPath")
        }
    }

    /**
     * 验证文件大小是否安全
     * @param fileSize 文件大小（字节）
     * @param maxSize 最大允许大小（字节）
     * @param filePath 文件路径（用于错误信息）
     */
    fun validateFileSize(fileSize: Long, maxSize: Long = IOUtilConstants.MAX_FILE_SIZE, filePath: String = "文件") {
        if (fileSize > maxSize) {
            Throw.utilInvalidParam("文件过大(${fileSize} bytes)，超过最大限制(${maxSize} bytes): $filePath")
        }
    }

    /**
     * 验证ZIP安全性
     * @param entryCount 条目数量
     * @param totalSize 总大小
     * @param singleFileSize 单个文件大小
     * @param entryPath 条目路径
     */
    fun validateZipSecurity(
        entryCount: Int? = null,
        totalSize: Long? = null,
        singleFileSize: Long? = null,
        entryPath: String? = null
    ) {
        entryCount?.let { count ->
            if (count > IOUtilConstants.MAX_ZIP_ENTRIES) {
                Throw.utilInvalidParam("ZIP文件包含过多条目(${count}个)，可能是ZIP炸弹")
            }
        }

        totalSize?.let { size ->
            if (size > IOUtilConstants.MAX_ZIP_EXTRACTED_SIZE) {
                Throw.utilInvalidParam("解压文件总大小超过限制，可能是ZIP炸弹")
            }
        }

        singleFileSize?.let { size ->
            if (size > IOUtilConstants.MAX_ZIP_SINGLE_FILE_SIZE) {
                Throw.utilInvalidParam("ZIP条目过大: ${entryPath ?: "未知文件"} (${size} bytes)")
            }
        }

        entryPath?.let { path ->
            validatePathSecurity(path, "解压文件")
        }
    }

    /**
     * 验证ZIP路径遍历攻击
     * @param entryFile 条目文件
     * @param targetDir 目标目录
     * @param entryName 条目名称
     */
    fun validateZipPathTraversal(entryFile: File, targetDir: File, entryName: String) {
        if (!entryFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
            Throw.utilUnsafeOperation("ZIP文件包含不安全的路径: $entryName")
        }
    }

    /**
     * 验证CSV安全性
     * @param lineCount 行数
     * @param columnCount 列数
     * @param cellLength 单元格长度
     */
    fun validateCsvSecurity(
        lineCount: Int? = null,
        columnCount: Int? = null,
        cellLength: Int? = null
    ) {
        lineCount?.let { count ->
            if (count > IOUtilConstants.MAX_CSV_LINES) {
                Throw.utilInvalidParam("CSV行数过多(${count}行)，超过最大限制(${IOUtilConstants.MAX_CSV_LINES}行)")
            }
        }

        columnCount?.let { count ->
            if (count > IOUtilConstants.MAX_CSV_COLUMNS) {
                Throw.utilInvalidParam("CSV列数过多(${count}列)，超过最大限制(${IOUtilConstants.MAX_CSV_COLUMNS}列)")
            }
        }

        cellLength?.let { length ->
            if (length > IOUtilConstants.MAX_CSV_CELL_LENGTH) {
                Throw.utilInvalidParam("CSV单元格内容过长(${length}字符)，超过最大限制(${IOUtilConstants.MAX_CSV_CELL_LENGTH}字符)")
            }
        }
    }

    /**
     * 检查文件是否存在并且是文件
     * @param file 文件对象
     * @param operation 操作描述
     */
    fun validateFileExists(file: File, operation: String = "文件操作") {
        if (!file.exists()) {
            Throw.utilFileNotFound("文件不存在: ${file.absolutePath}")
        }
        if (!file.isFile) {
            Throw.utilInvalidParam("路径不是文件: ${file.absolutePath}")
        }
    }

    /**
     * 检查目录是否存在并且是目录
     * @param dir 目录对象
     * @param operation 操作描述
     */
    fun validateDirectoryExists(dir: File, operation: String = "目录操作") {
        if (!dir.exists()) {
            Throw.utilFileNotFound("目录不存在: ${dir.absolutePath}")
        }
        if (!dir.isDirectory) {
            Throw.utilInvalidParam("路径不是目录: ${dir.absolutePath}")
        }
    }

    /**
     * 获取最佳缓冲区大小
     * @param fileSize 文件大小
     * @return 最佳缓冲区大小
     */
    fun getOptimalBufferSize(fileSize: Long): Int {
        return when {
            fileSize > IOUtilConstants.LARGE_FILE_THRESHOLD -> IOUtilConstants.LARGE_BUFFER_SIZE
            fileSize > 0 -> kotlin.math.min(IOUtilConstants.DEFAULT_BUFFER_SIZE, fileSize.toInt())
            else -> IOUtilConstants.DEFAULT_BUFFER_SIZE
        }
    }

    /**
     * 安全操作包装器
     * @param operation 操作描述
     * @param filePath 文件路径（可选）
     * @param block 要执行的操作
     * @return 操作结果
     */
    inline fun <T> safeOperation(
        operation: String,
        filePath: String? = null,
        block: () -> T
    ): T {
        return try {
            block()
        } catch (e: SecurityException) {
            val path = filePath ?: "未知路径"
            Throw.utilPermissionDenied("权限不足，无法$operation: $path", e)
        } catch (e: OutOfMemoryError) {
            val path = filePath ?: "未知路径"
            Throw.utilIoError("内存不足，无法$operation: $path", e)
        } catch (e: java.io.IOException) {
            val path = filePath ?: "未知路径"
            Throw.utilIoError("IO错误，$operation 失败: $path (${e.message})", e)
        } catch (e: Exception) {
            val path = filePath ?: "未知路径"
            Throw.utilIoError("$operation 时发生未知错误: $path (${e.message})", e)
        }
    }
}

package com.fanekit.common.util.io

import com.fanekit.common.core.constants.IOUtilConstants
import com.fanekit.common.core.exception.Throw
import com.fanekit.common.core.security.IOSecurityValidator
import java.io.*
import java.nio.charset.Charset
import java.nio.file.*
import java.security.MessageDigest
import java.text.DecimalFormat

/**
 * 文件工具类
 * 提供文件和目录的各种操作功能
 * 包括文件创建、删除、复制、移动、读取、写入等
 * 同时提供安全性和性能优化的实现
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2025/03/19
 * @since 1.0.0
 * @version 1.0.0
 */
object FileUtil {

    /**
     * 检查文件是否存在
     */
    fun exists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    /**
     * 检查文件是否存在
     */
    fun exists(file: File): Boolean {
        return file.exists()
    }

    /**
     * 检查是否为文件
     */
    fun isFile(filePath: String): Boolean {
        return File(filePath).isFile
    }

    /**
     * 检查是否为目录
     */
    fun isDirectory(filePath: String): Boolean {
        return File(filePath).isDirectory
    }

    /**
     * 创建目录（包括父目录）- 带安全检查
     */
    fun createDirectories(dirPath: String) {
        IOSecurityValidator.safeOperation("创建目录", dirPath) {
            val file = File(dirPath)

            // 如果目录已存在，直接返回
            if (file.exists()) {
                if (!file.isDirectory) {
                    Throw.utilInvalidParam("路径存在但不是目录: $dirPath")
                }
                return@safeOperation
            }

            // 尝试创建目录
            val success = file.mkdirs()
            if (!success) {
                Throw.utilOperationFailed("创建目录失败: $dirPath")
            }
        }
    }

    /**
     * 创建文件（包括父目录）- 带安全检查
     */
    fun createFile(filePath: String) {
        IOSecurityValidator.safeOperation("创建文件", filePath) {
            val file = File(filePath)

            // 如果文件已存在，直接返回
            if (file.exists()) {
                if (!file.isFile) {
                    Throw.utilInvalidParam("路径存在但不是文件: $filePath")
                }
                return@safeOperation
            }

            // 确保父目录存在
            file.parentFile?.let { parent ->
                if (!parent.exists()) {
                    IOSecurityValidator.validatePathSecurity(parent.absolutePath, "创建父目录")
                    val parentSuccess = parent.mkdirs()
                    if (!parentSuccess) {
                        Throw.utilOperationFailed("创建父目录失败: ${parent.absolutePath}")
                    }
                }
            }

            // 创建文件
            val success = file.createNewFile()
            if (!success) {
                Throw.utilOperationFailed("创建文件失败: $filePath")
            }
        }
    }

    /**
     * 删除文件或目录
     */
    fun delete(filePath: String) {
        delete(File(filePath))
    }

    /**
     * 删除文件或目录
     */
    fun delete(file: File) {
        IOSecurityValidator.safeOperation("删除文件") {
            if (!file.exists()) {
                Throw.utilFileNotFound("文件或目录不存在: ${file.absolutePath}")
            }

            val success = if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }

            if (!success) {
                Throw.utilOperationFailed("删除失败: ${file.absolutePath}")
            }
        }
    }

    /**
     * 读取文件内容为字符串
     */
    fun readText(filePath: String, charset: Charset = IOUtilConstants.DEFAULT_CHARSET): String {
        return IOSecurityValidator.safeOperation("读取文件", filePath) {
            val file = File(filePath)
            IOSecurityValidator.validateFileExists(file)

            file.readText(charset)
        }
    }

    /**
     * 读取文件内容为字符串
     */
    fun readText(file: File, charset: Charset = IOUtilConstants.DEFAULT_CHARSET): String {
        return readText(file.absolutePath, charset)
    }

    /**
     * 读取文件所有行
     */
    fun readLines(filePath: String, charset: Charset = IOUtilConstants.DEFAULT_CHARSET): List<String> {
        return IOSecurityValidator.safeOperation("读取文件", filePath) {
            val file = File(filePath)
            IOSecurityValidator.validateFileExists(file)

            file.readLines(charset)
        }
    }

    /**
     * 读取文件所有行
     */
    fun readLines(file: File, charset: Charset = IOUtilConstants.DEFAULT_CHARSET): List<String> {
        return readLines(file.absolutePath, charset)
    }

    /**
     * 读取文件为字节数组 - 优化大文件处理
     */
    fun readBytes(filePath: String): ByteArray {
        return IOSecurityValidator.safeOperation("读取文件", filePath) {
            val file = File(filePath)
            IOSecurityValidator.validateFileExists(file)

            val fileSize = file.length()
            IOSecurityValidator.validateFileSize(fileSize)

            // 使用优化的缓冲区大小
            val bufferSize = IOSecurityValidator.getOptimalBufferSize(fileSize)
            file.inputStream().buffered(bufferSize).use { it.readBytes() }
        }
    }

    /**
     * 读取文件为字节数组
     */
    fun readBytes(file: File): ByteArray {
        return readBytes(file.absolutePath)
    }

    /**
     * 写入文本到文件 - 优化性能
     */
    fun writeText(filePath: String, text: String, charset: Charset = IOUtilConstants.DEFAULT_CHARSET, append: Boolean = false) {
        IOSecurityValidator.safeOperation("写入文件", filePath) {
            val file = File(filePath)
            file.parentFile?.let { parent ->
                if (!parent.exists()) {
                    IOSecurityValidator.validatePathSecurity(parent.absolutePath, "创建父目录")
                    if (!parent.mkdirs()) {
                        Throw.utilOperationFailed("创建父目录失败: ${parent.absolutePath}")
                    }
                }
            }

            // 使用优化的写入方式
            FileOutputStream(file, append).use { fos ->
                OutputStreamWriter(fos, charset).use { writer ->
                    BufferedWriter(writer, IOUtilConstants.DEFAULT_BUFFER_SIZE).use { bufferedWriter ->
                        bufferedWriter.write(text)
                    }
                }
            }
        }
    }

    /**
     * 写入行到文件
     */
    fun writeLines(filePath: String, lines: List<String>, charset: Charset = IOUtilConstants.DEFAULT_CHARSET, append: Boolean = false) {
        val text = lines.joinToString(System.lineSeparator())
        writeText(filePath, text, charset, append)
    }

    /**
     * 写入字节数组到文件
     */
    fun writeBytes(filePath: String, bytes: ByteArray, append: Boolean = false) {
        IOSecurityValidator.safeOperation("写入文件", filePath) {
            val file = File(filePath)
            file.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) {
                    Throw.utilOperationFailed("创建父目录失败: ${parent.absolutePath}")
                }
            }
            if (append) {
                file.appendBytes(bytes)
            } else {
                file.writeBytes(bytes)
            }
        }
    }

    /**
     * 复制文件
     */
    fun copyFile(sourcePath: String, targetPath: String, overwrite: Boolean = true): Boolean {
        return copyFile(File(sourcePath), File(targetPath), overwrite)
    }

    /**
     * 复制文件
     */
    fun copyFile(sourceFile: File, targetFile: File, overwrite: Boolean = true): Boolean {
        return IOSecurityValidator.safeOperation("复制文件") {
            IOSecurityValidator.validatePathSecurity(sourceFile.absolutePath, "读取源文件")
            IOSecurityValidator.validatePathSecurity(targetFile.absolutePath, "写入目标文件")

            IOSecurityValidator.validateFileExists(sourceFile)

            if (targetFile.exists() && !overwrite) {
                Throw.utilOperationFailed("目标文件已存在且不允许覆盖: ${targetFile.absolutePath}")
            }

            targetFile.parentFile?.let { parent ->
                if (!parent.exists()) {
                    IOSecurityValidator.validatePathSecurity(parent.absolutePath, "创建目标目录")
                    if (!parent.mkdirs()) {
                        Throw.utilOperationFailed("创建目标目录失败: ${parent.absolutePath}")
                    }
                }
            }

            // 优化大文件复制
            val fileSize = sourceFile.length()
            val bufferSize = IOSecurityValidator.getOptimalBufferSize(fileSize)

            FileInputStream(sourceFile).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output, bufferSize)
                }
            }
            true
        }
    }

    /**
     * 复制目录
     */
    fun copyDirectory(sourcePath: String, targetPath: String, overwrite: Boolean = true): Boolean {
        return copyDirectory(File(sourcePath), File(targetPath), overwrite)
    }

    /**
     * 复制目录
     */
    fun copyDirectory(sourceDir: File, targetDir: File, overwrite: Boolean = true): Boolean {
        return try {
            if (!sourceDir.exists()) {
                Throw.utilFileNotFound("源目录不存在: ${sourceDir.absolutePath}")
            }
            if (!sourceDir.isDirectory) {
                Throw.utilInvalidParam("源路径不是目录: ${sourceDir.absolutePath}")
            }

            sourceDir.copyRecursively(targetDir, overwrite)
            true
        } catch (e: SecurityException) {
            Throw.utilPermissionDenied("没有权限复制目录: ${e.message}", e)
        } catch (e: IOException) {
            Throw.utilIoError("复制目录失败: ${e.message}", e)
        } catch (e: Exception) {
            Throw.utilIoError("复制目录时发生未知错误: ${e.message}", e)
        }
    }

    /**
     * 移动文件或目录
     */
    fun move(sourcePath: String, targetPath: String, overwrite: Boolean = true): Boolean {
        return move(File(sourcePath), File(targetPath), overwrite)
    }

    /**
     * 移动文件或目录
     */
    fun move(source: File, target: File, overwrite: Boolean = true): Boolean {
        return try {
            if (!source.exists()) {
                Throw.utilFileNotFound("源文件或目录不存在: ${source.absolutePath}")
            }

            if (target.exists() && !overwrite) {
                Throw.utilOperationFailed("目标已存在且不允许覆盖: ${target.absolutePath}")
            }

            target.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) {
                    Throw.utilOperationFailed("创建目标目录失败: ${parent.absolutePath}")
                }
            }

            if (source.isDirectory) {
                source.copyRecursively(target, overwrite)
                source.deleteRecursively()
            } else {
                source.copyTo(target, overwrite)
                source.delete()
            }
            true
        } catch (e: SecurityException) {
            Throw.utilPermissionDenied("没有权限移动文件: ${e.message}", e)
        } catch (e: IOException) {
            Throw.utilIoError("移动文件失败: ${e.message}", e)
        } catch (e: Exception) {
            Throw.utilIoError("移动文件时发生未知错误: ${e.message}", e)
        }
    }

    /**
     * 重命名文件或目录
     */
    fun rename(filePath: String, newName: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Throw.utilFileNotFound("文件或目录不存在: $filePath")
            }

            val newFile = File(file.parent, newName)
            if (newFile.exists()) {
                Throw.utilOperationFailed("目标名称已存在: $newName")
            }

            val success = file.renameTo(newFile)
            if (!success) {
                Throw.utilOperationFailed("重命名失败: $filePath -> $newName")
            }
            true
        } catch (e: SecurityException) {
            Throw.utilPermissionDenied("没有权限重命名: $filePath", e)
        } catch (e: Exception) {
            Throw.utilIoError("重命名时发生错误: ${e.message}", e)
        }
    }

    /**
     * 获取文件大小
     */
    fun getFileSize(filePath: String): Long {
        return File(filePath).length()
    }

    /**
     * 获取目录大小（递归计算）
     */
    fun getDirectorySize(dirPath: String): Long {
        return getDirectorySize(File(dirPath))
    }

    /**
     * 获取目录大小（递归计算）
     */
    fun getDirectorySize(dir: File): Long {
        if (!dir.exists() || !dir.isDirectory) {
            return 0L
        }

        var size = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }

    /**
     * 格式化文件大小
     */
    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"

        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val df = DecimalFormat("#,##0.#")
        return "${df.format(size / Math.pow(1024.0, digitGroups.toDouble()))} ${IOUtilConstants.FILE_SIZE_UNITS[digitGroups]}"
    }

    /**
     * 获取文件扩展名
     */
    fun getExtension(filePath: String): String {
        val file = File(filePath)
        val name = file.name
        val lastDotIndex = name.lastIndexOf('.')
        return if (lastDotIndex > 0 && lastDotIndex < name.length - 1) {
            name.substring(lastDotIndex + 1).lowercase()
        } else {
            ""
        }
    }

    /**
     * 获取文件名（不含扩展名）
     */
    fun getNameWithoutExtension(filePath: String): String {
        val file = File(filePath)
        val name = file.name
        val lastDotIndex = name.lastIndexOf('.')
        return if (lastDotIndex > 0) {
            name.substring(0, lastDotIndex)
        } else {
            name
        }
    }

    /**
     * 列出目录下的文件和子目录
     */
    fun listFiles(dirPath: String, recursive: Boolean = false): List<File> {
        return listFiles(File(dirPath), recursive)
    }

    /**
     * 列出目录下的文件和子目录
     */
    fun listFiles(dir: File, recursive: Boolean = false): List<File> {
        if (!dir.exists() || !dir.isDirectory) {
            return emptyList()
        }

        return if (recursive) {
            dir.walkTopDown().filter { it != dir }.toList()
        } else {
            dir.listFiles()?.toList() ?: emptyList()
        }
    }

    /**
     * 根据扩展名过滤文件
     */
    fun listFilesByExtension(dirPath: String, extensions: List<String>, recursive: Boolean = false): List<File> {
        val files = listFiles(dirPath, recursive)
        val lowerExtensions = extensions.map { it.lowercase() }
        return files.filter { file ->
            file.isFile && getExtension(file.absolutePath) in lowerExtensions
        }
    }

    /**
     * 查找文件
     */
    fun findFiles(dirPath: String, pattern: String, recursive: Boolean = true): List<File> {
        val regex = pattern.replace("*", ".*").replace("?", ".").toRegex(RegexOption.IGNORE_CASE)
        val files = listFiles(dirPath, recursive)
        return files.filter { file ->
            file.isFile && regex.matches(file.name)
        }
    }

    /**
     * 计算文件MD5哈希值
     */
    fun calculateMD5(filePath: String): String {
        return calculateHash(filePath, "MD5")
    }

    /**
     * 计算文件SHA256哈希值
     */
    fun calculateSHA256(filePath: String): String {
        return calculateHash(filePath, "SHA-256")
    }

    /**
     * 计算文件哈希值 - 优化内存使用
     */
    fun calculateHash(filePath: String, algorithm: String): String {
        return IOSecurityValidator.safeOperation("计算哈希值", filePath) {
            val file = File(filePath)
            IOSecurityValidator.validateFileExists(file)

            val digest = try {
                MessageDigest.getInstance(algorithm)
            } catch (e: Exception) {
                Throw.utilInvalidParam("不支持的哈希算法: $algorithm", e)
            }

            val fileSize = file.length()
            val bufferSize = IOSecurityValidator.getOptimalBufferSize(fileSize)

            file.inputStream().buffered(bufferSize).use { input ->
                val buffer = ByteArray(bufferSize)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } > 0) {
                    digest.update(buffer, 0, bytesRead)
                }
            }

            digest.digest().joinToString("") { "%02x".format(it) }
        }
    }

    /**
     * 比较两个文件是否相同
     */
    fun compareFiles(file1Path: String, file2Path: String): Boolean {
        val file1 = File(file1Path)
        val file2 = File(file2Path)

        if (!file1.exists() || !file2.exists()) {
            return false
        }

        if (file1.length() != file2.length()) {
            return false
        }

        return calculateMD5(file1Path) == calculateMD5(file2Path)
    }

    /**
     * 创建临时文件
     */
    fun createTempFile(prefix: String = IOUtilConstants.TEMP_FILE_PREFIX, suffix: String = IOUtilConstants.TEMP_FILE_SUFFIX, directory: File? = null): File {
        return IOSecurityValidator.safeOperation("创建临时文件") {
            File.createTempFile(prefix, suffix, directory)
        }
    }

    /**
     * 创建临时目录
     */
    fun createTempDirectory(prefix: String = "tempDir"): File {
        return IOSecurityValidator.safeOperation("创建临时目录") {
            Files.createTempDirectory(prefix).toFile()
        }
    }

    /**
     * 安全创建带随机名称的临时文件
     */
    fun createSecureTempFile(prefix: String = "secure_temp", suffix: String = IOUtilConstants.TEMP_FILE_SUFFIX, directory: File? = null): File {
        return IOSecurityValidator.safeOperation("创建安全临时文件") {
            // 生成更安全的随机前缀
            val secureRandom = java.security.SecureRandom()
            val randomSuffix = secureRandom.nextLong().toString(36)
            val securePrefix = "${prefix}_${randomSuffix}"

            val tempFile = File.createTempFile(securePrefix, suffix, directory)

            // 设置文件权限（仅所有者可读写）
            try {
                tempFile.setReadable(false, false)
                tempFile.setWritable(false, false)
                tempFile.setExecutable(false, false)
                tempFile.setReadable(true, true)
                tempFile.setWritable(true, true)
            } catch (e: Exception) {
                // 在某些系统上可能不支持设置权限，忽略错误
            }

            tempFile
        }
    }

    /**
     * 安全清理临时文件
     */
    fun cleanupTempFiles(tempDir: File = File(System.getProperty("java.io.tmpdir"))) {
        IOSecurityValidator.safeOperation("清理临时文件") {
            if (!tempDir.exists() || !tempDir.isDirectory) {
                return@safeOperation
            }

            val currentTime = System.currentTimeMillis()
            val oneDayAgo = currentTime - IOUtilConstants.TEMP_FILE_CLEANUP_INTERVAL

            tempDir.listFiles { file ->
                file.name.startsWith("temp") && file.lastModified() < oneDayAgo
            }?.forEach { file ->
                try {
                    if (file.isDirectory) {
                        file.deleteRecursively()
                    } else {
                        file.delete()
                    }
                } catch (e: Exception) {
                    // 忽略删除失败的临时文件
                }
            }
        }
    }

    /**
     * 获取文件MIME类型 - 优化异常处理
     */
    fun getMimeType(filePath: String): String {
        return IOSecurityValidator.safeOperation("获取MIME类型", filePath) {
            val file = File(filePath)
            IOSecurityValidator.validateFileExists(file)
            Files.probeContentType(Paths.get(filePath)) ?: "application/octet-stream"
        }
    }

    /**
     * 检查文件是否为文本文件
     */
    fun isTextFile(filePath: String): Boolean {
        val mimeType = getMimeType(filePath)
        return IOUtilConstants.TEXT_MIME_TYPES.contains(mimeType) ||
               mimeType.startsWith("text/")
    }

    /**
     * 检查文件是否为图片文件
     */
    fun isImageFile(filePath: String): Boolean {
        val mimeType = getMimeType(filePath)
        return mimeType.startsWith(IOUtilConstants.IMAGE_MIME_PREFIX)
    }

    /**
     * 清空目录
     */
    fun clearDirectory(dirPath: String): Boolean {
        return try {
            val dir = File(dirPath)
            if (!dir.exists()) {
                Throw.utilFileNotFound("目录不存在: $dirPath")
            }
            if (!dir.isDirectory) {
                Throw.utilInvalidParam("路径不是目录: $dirPath")
            }

            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    if (!file.deleteRecursively()) {
                        Throw.utilOperationFailed("删除子目录失败: ${file.absolutePath}")
                    }
                } else {
                    if (!file.delete()) {
                        Throw.utilOperationFailed("删除文件失败: ${file.absolutePath}")
                    }
                }
            }
            true
        } catch (e: SecurityException) {
            Throw.utilPermissionDenied("没有权限清空目录: $dirPath", e)
        } catch (e: IOException) {
            Throw.utilIoError("清空目录失败: ${e.message}", e)
        } catch (e: Exception) {
            Throw.utilIoError("清空目录时发生未知错误: ${e.message}", e)
        }
    }

    /**
     * 获取文件的最后修改时间
     */
    fun getLastModified(filePath: String): Long {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Throw.utilFileNotFound("文件不存在: $filePath")
            }
            file.lastModified()
        } catch (e: SecurityException) {
            Throw.utilPermissionDenied("没有权限访问文件: $filePath", e)
        } catch (e: Exception) {
            Throw.utilIoError("获取文件修改时间失败: ${e.message}", e)
        }
    }

    /**
     * 设置文件的最后修改时间
     */
    fun setLastModified(filePath: String, time: Long): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Throw.utilFileNotFound("文件不存在: $filePath")
            }
            val success = file.setLastModified(time)
            if (!success) {
                Throw.utilOperationFailed("设置文件修改时间失败: $filePath")
            }
            true
        } catch (e: SecurityException) {
            Throw.utilPermissionDenied("没有权限修改文件时间: $filePath", e)
        } catch (e: Exception) {
            Throw.utilIoError("设置文件修改时间失败: ${e.message}", e)
        }
    }

    /**
     * 获取文件属性信息
     */
    fun getFileInfo(filePath: String): FileInfo {
        val file = File(filePath)
        return FileInfo(
            name = file.name,
            path = file.absolutePath,
            size = if (file.isFile) file.length() else getDirectorySize(file),
            lastModified = file.lastModified(),
            isFile = file.isFile,
            isDirectory = file.isDirectory,
            isHidden = file.isHidden,
            canRead = file.canRead(),
            canWrite = file.canWrite(),
            canExecute = file.canExecute(),
            extension = getExtension(filePath),
            mimeType = if (file.isFile) getMimeType(filePath) else null
        )
    }

    /**
     * 文件信息数据类
     */
    data class FileInfo(
        val name: String,
        val path: String,
        val size: Long,
        val lastModified: Long,
        val isFile: Boolean,
        val isDirectory: Boolean,
        val isHidden: Boolean,
        val canRead: Boolean,
        val canWrite: Boolean,
        val canExecute: Boolean,
        val extension: String,
        val mimeType: String?
    )

    /**
     * 监听目录变化（简单实现）
     */
    fun watchDirectory(dirPath: String, callback: (WatchEvent.Kind<*>, Path) -> Unit): Closeable {
        return try {
            val dir = File(dirPath)
            if (!dir.exists()) {
                Throw.utilFileNotFound("目录不存在: $dirPath")
            }
            if (!dir.isDirectory) {
                Throw.utilInvalidParam("路径不是目录: $dirPath")
            }

            val watchService = FileSystems.getDefault().newWatchService()
            val path = Paths.get(dirPath)

            path.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY
            )

            val thread = Thread {
                try {
                    while (!Thread.currentThread().isInterrupted) {
                        val key = watchService.take()
                        for (event in key.pollEvents()) {
                            val context = event.context()
                            if (context is Path) {
                                callback(event.kind(), path.resolve(context))
                            }
                        }
                        key.reset()
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }

            thread.start()

            Closeable {
                thread.interrupt()
                watchService.close()
            }
        } catch (e: SecurityException) {
            Throw.utilPermissionDenied("没有权限监听目录: $dirPath", e)
        } catch (e: IOException) {
            Throw.utilIoError("创建目录监听器失败: ${e.message}", e)
        } catch (e: Exception) {
            Throw.utilIoError("创建目录监听器时发生未知错误: ${e.message}", e)
        }
    }

    /**
     * 测试工具类异常的主方法
     */
    @JvmStatic
    fun main(args: Array<String>) {
        println("=== FileUtil 安全性和性能测试 ===\n")

        // 测试1：路径安全检查
        println("1. 测试路径安全检查:")
        val dangerousPaths = listOf(
            "../../../etc/passwd",
            "..\\..\\windows\\system32",
            "CON",
            "file\$with\$dollar",
            "very".repeat(100) + ".txt"
        )

        dangerousPaths.forEach { path ->
            try {
                println("  测试危险路径: $path")
                FileUtil.createFile(path)
                println("    ✗ 未检测到危险路径")
            } catch (ex: Exception) {
                println("    ✓ 成功阻止: ${ex.message}")
            }
        }

        println("\n" + "=".repeat(50) + "\n")

        // 测试2：性能优化验证
        println("2. 测试性能优化:")
        try {
            val testFile = "performance_test_${System.currentTimeMillis()}.txt"
            val testContent = "这是性能测试内容\n".repeat(10000)

            val startTime = System.currentTimeMillis()
            FileUtil.writeText(testFile, testContent)
            val writeTime = System.currentTimeMillis() - startTime
            println("  写入性能: ${writeTime}ms")

            val readStartTime = System.currentTimeMillis()
            val readContent = FileUtil.readText(testFile)
            val readTime = System.currentTimeMillis() - readStartTime
            println("  读取性能: ${readTime}ms")
            println("  内容验证: ${if (readContent == testContent) "✓" else "✗"}")

            FileUtil.delete(testFile)
            println("  ✓ 性能测试完成")
        } catch (ex: Exception) {
            println("  ✗ 性能测试失败: ${ex.message}")
        }

        println("\n=== 测试完成 ===")
    }
}

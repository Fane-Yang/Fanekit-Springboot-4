package com.fanekit.common.excel.detector

import com.fanekit.common.excel.enum.FileProcessStrategy
import java.io.File

/**
 * 文件大小检测器 - 用于判断文件处理策略
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/23
 * @since 1.0.0
 * @version 1.0.0
 */
object FileSizeDetector {

    /**
     * 5MB以上算大文件（使用流式读取）
     * 根据实际测试，5MB大约对应50万行数据
     */
    const val BIG_FILE_THRESHOLD_MB = 5L

    /**
     * 大文件模式默认最大支持10MB,超出算超大文件
     */
    const val BIG_MODE_DEFAULT_MAX_SIZE_MB = 10L

    /**
     * 传统模式最大支持100MB
     */
    const val TRADITIONAL_MAX_SIZE_MB = 5L



    /**
     * 检查文件大小并返回建议的处理策略
     */
    fun checkFileSize(filePath: String): com.fanekit.common.excel.enum.FileProcessStrategy {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("文件不存在: $filePath")
        }

        val fileSizeMB = file.length() / 1024 / 1024

        return when {
            fileSizeMB < BIG_FILE_THRESHOLD_MB -> FileProcessStrategy.TRADITIONAL
            fileSizeMB < BIG_MODE_DEFAULT_MAX_SIZE_MB -> FileProcessStrategy.STREAMING
            else -> FileProcessStrategy.LARGE_STREAMING
        }
    }

    /**
     * 获取文件大小（MB）
     */
    fun getFileSizeMB(filePath: String): Long {
        val file = File(filePath)
        return if (file.exists()) {
            file.length() / 1024 / 1024
        } else {
            0L
        }
    }

    /**
     * 验证文件大小是否在允许范围内
     */
    fun validateFileSize(filePath: String, maxSizeMB: Int): Boolean {
        val fileSizeMB = getFileSizeMB(filePath)
        return fileSizeMB <= maxSizeMB
    }
}



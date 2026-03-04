package com.fanekit.common.excel.enum

/**
 * 文件处理策略枚举
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/23
 * @since 1.0.0
 * @version 1.0.0
 */
enum class FileProcessStrategy(val description: String) {
    TRADITIONAL("传统模式 - 小文件，快速处理"),
    STREAMING("流式模式 - 中等大小文件，节省内存"),
    LARGE_STREAMING("大文件流式模式 - 超大文件，极致内存优化")
}
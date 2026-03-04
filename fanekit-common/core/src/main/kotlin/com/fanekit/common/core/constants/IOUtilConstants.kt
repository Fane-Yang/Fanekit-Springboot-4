package com.fanekit.common.core.constants

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * 文件工具常量配置
 * 统一管理所有文件操作相关的常量和限制
 * 包括缓冲区大小、编码、文件大小限制等
 * 适用于文件读写、压缩、解压等操作
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/7/24
 * @since 1.0.0
 * @version 1.0.0
 */
object IOUtilConstants {

    /* ──────── 缓冲区配置 ──────── */

    /**
     * 默认缓冲区大小 - 64KB
     */
    const val DEFAULT_BUFFER_SIZE = 65536

    /**
     * 大文件缓冲区大小 - 1MB
     */
    const val LARGE_BUFFER_SIZE = 1048576

    /**
     * 大文件阈值 - 10MB
     */
    const val LARGE_FILE_THRESHOLD = 10485760L

    /* ──────── 编码配置 ──────── */

    /**
     * 默认字符编码
     */
    val DEFAULT_CHARSET: Charset = StandardCharsets.UTF_8

    /* ──────── 安全限制 ──────── */

    /**
     * 最大文件大小限制 - 100MB（通用）
     */
    const val MAX_FILE_SIZE = 104857600L

    /**
     * 最大文件名长度
     */
    const val MAX_FILENAME_LENGTH = 255

    /**
     * Windows平台最大路径长度
     */
    const val MAX_PATH_LENGTH_WINDOWS = 260

    /**
     * Unix/Linux平台最大路径长度
     */
    const val MAX_PATH_LENGTH_UNIX = 4096

    /* ──────── ZIP相关限制 ──────── */

    /**
     * 最大ZIP条目数量（防止ZIP炸弹）
     */
    const val MAX_ZIP_ENTRIES = 10000

    /**
     * 最大ZIP解压总大小 - 1GB（防止ZIP炸弹）
     */
    const val MAX_ZIP_EXTRACTED_SIZE = 1073741824L

    /**
     * 最大ZIP单个文件大小 - 100MB
     */
    const val MAX_ZIP_SINGLE_FILE_SIZE = 104857600L

    /* ──────── CSV相关限制 ──────── */

    /**
     * 最大CSV行数（防止DoS攻击）
     */
    const val MAX_CSV_LINES = 1000000

    /**
     * 最大CSV列数
     */
    const val MAX_CSV_COLUMNS = 10000

    /**
     * 最大CSV单元格内容长度
     */
    const val MAX_CSV_CELL_LENGTH = 32768

    /**
     * CSV默认分隔符
     */
    const val CSV_DEFAULT_SEPARATOR = ','

    /**
     * CSV默认引号字符
     */
    const val CSV_DEFAULT_QUOTE = '"'

    /**
     * CSV默认转义字符
     */
    const val CSV_DEFAULT_ESCAPE = '\\'

    /* ──────── 临时文件配置 ──────── */

    /**
     * 临时文件默认前缀
     */
    const val TEMP_FILE_PREFIX = "temp"

    /**
     * 临时文件默认后缀
     */
    const val TEMP_FILE_SUFFIX = ".tmp"

    /**
     * 临时文件清理间隔（毫秒）- 24小时
     */
    const val TEMP_FILE_CLEANUP_INTERVAL = 24 * 60 * 60 * 1000L

    /* ──────── 文件类型配置 ──────── */

    /**
     * 文本文件MIME类型
     */
    val TEXT_MIME_TYPES = setOf(
        "text/plain",
        "text/html",
        "text/css",
        "text/javascript",
        "text/xml",
        "application/json",
        "application/xml",
        "application/javascript"
    )

    /**
     * 图片文件MIME类型前缀
     */
    const val IMAGE_MIME_PREFIX = "image/"

    /**
     * 文件大小单位
     */
    val FILE_SIZE_UNITS = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
}

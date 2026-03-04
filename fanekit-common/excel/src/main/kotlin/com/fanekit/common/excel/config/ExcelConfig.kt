package com.fanekit.common.excel.config

/**
 * Excel处理配置类 - 统一管理文件大小阈值和处理参数
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/23
 * @since 1.0.0
 * @version 1.0.0
 */
object ExcelConfig {

    /**
     * 文件大小相关配置
     */
    object FileSize {
        /** 5MB以上算大文件，使用流式读取 */
        const val BIG_FILE_THRESHOLD_MB = 5L

        /** 传统模式默认最大文件大小限制 */
        const val TRADITIONAL_MAX_SIZE_MB = 5

        /** 大文件模式默认最大文件大小限制 */
        const val BIG_MODE_DEFAULT_MAX_SIZE_MB = 10
    }

    /**
     * 批处理相关配置
     */
    object Batch {
        /** 传统模式默认批次大小 */
        const val TRADITIONAL_BATCH_SIZE = 500

        /** 大文件模式默认批次大小 */
        const val BIG_MODE_BATCH_SIZE = 100

        /** 导出时传统模式最大批次大小 */
        const val EXPORT_MAX_BATCH_SIZE = 1000
    }

    /**
     * 内存相关配置
     */
    object Memory {
        /** SXSSFWorkbook内存中保留的行数 */
        const val SXSSF_WINDOW_SIZE = 100

        /** 流式读取时字符串缓存清理频率（每N个批次） */
        const val CACHE_CLEANUP_FREQUENCY = 50
    }

    /**
     * 默认配置
     */
    object Default {
        /** 默认开始行（跳过标题和字段名） */
        const val START_LINE = 2

        /** 默认标题 */
        const val TITLE = "标题"
    }
}


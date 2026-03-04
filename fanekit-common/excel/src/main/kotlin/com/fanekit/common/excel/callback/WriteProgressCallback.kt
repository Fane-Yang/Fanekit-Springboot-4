package com.fanekit.common.excel.callback

/**
 * 导出进度回调接口
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/18
 * @since 1.0.0
 * @version 1.0.0
 */
interface WriteProgressCallback {
    /**
     * 导出进度回调
     * @param processedCount 已处理数量
     * @param totalCount 总数量
     * @param remainingCount 剩余数量
     * @param currentBatch 当前批次号(从1开始)
     * @param totalBatches 总批次数
     * @param batchSize 当前批次大小
     */
    fun onProgress(
        processedCount: Long,
        totalCount: Long,
        remainingCount: Long,
        currentBatch: Int,
        totalBatches: Int,
        batchSize: Int
    )

    /**
     * 导出完成回调
     * @param totalCount 总导出数量
     * @param filePath 文件路径
     */
    fun onCompleted(totalCount: Long, filePath: String)

    /**
     * 导出出错回调
     * @param error 错误信息
     * @param processedCount 已处理数量
     */
    fun onError(error: Throwable, processedCount: Long)
}

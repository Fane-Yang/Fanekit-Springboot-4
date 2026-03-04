package com.fanekit.common.excel.callback

/**
 * 导入进度回调接口
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/18
 * @since 1.0.0
 * @version 1.0.0
 */
interface ReadProgressCallback<T> {
    /**
     * 导入进度回调
     * @param batchData 当前批次读取到的数据
     * @param processedCount 已处理数量
     * @param totalCount 总数量(如果能提前知道)
     * @param remainingCount 剩余数量
     * @param currentBatch 当前批次号(从1开始)
     * @param batchSize 当前批次大小
     */
    fun onBatchRead(
        batchData: List<T>,
        processedCount: Long,
        totalCount: Long,
        remainingCount: Long,
        currentBatch: Int,
        batchSize: Int
    )

    /**
     * 导入完成回调
     * @param totalCount 总导入数量
     */
    fun onCompleted(totalCount: Long)

    /**
     * 导入出错回调
     * @param error 错误信息
     * @param processedCount 已处理数量
     */
    fun onError(error: Throwable, processedCount: Long)
}

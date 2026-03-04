package com.fanekit.common.excel.provider

/**
 * 数据提供者接口 - 用于分批获取要导出的数据
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/10
 * @since 1.0.0
 * @version 1.0.0
 */
interface DataProvider<T> {
    /**
     * 获取总数据量
     */
    fun getTotalCount(): Long

    /**
     * 分批获取数据
     * 用户自行实现此方法来提供数据源，
     * 例如从数据库、文件或其他数据源中获取数据。
     *
     * mybatis-plus 分页查询时，offset = (page - 1) * limit
     *
     * @param offset 偏移量
     * @param limit 每批数量
     * @return 数据列表
     */
    fun getBatchData(offset: Long, limit: Int): List<T>
}

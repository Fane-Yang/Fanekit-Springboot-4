package com.fanekit.common.excel.model

/**
 * Sheet信息数据类
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/19
 * @since 1.0.0
 * @version 1.0.0
 */
data class SheetInfo(
    val index: Int,
    val name: String,
    val totalRows: Int,
    val isHidden: Boolean = false,
    val isVeryHidden: Boolean = false
) {
    override fun toString(): String {
        val status = when {
            isVeryHidden -> " [非常隐藏]"
            isHidden -> " [隐藏]"
            else -> ""
        }
        return "Sheet[$index]: $name ($totalRows 行)$status"
    }
}
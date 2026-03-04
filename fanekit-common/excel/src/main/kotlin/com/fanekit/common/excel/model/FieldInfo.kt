package com.fanekit.common.excel.model

import com.fanekit.common.excel.enum.ExcelAlign
import com.fanekit.common.excel.enum.ExcelColor
import java.lang.reflect.Field

/**
 * 字段信息数据类
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/19
 * @since 1.0.0
 * @version 1.0.0
 */
data class FieldInfo(
    val fieldMap: Map<String, Field>,
    val convertMap: Map<String, Map<String, String>>,
    val timeFormatMap: Map<String, String>,
    val colorMap: Map<String, ExcelColor>,
    val cellFormatMap: Map<String, String>,
    val fieldOrder: List<String>,
    val boldMap: Map<String, Boolean>,
    val alignMap: Map<String, ExcelAlign>,
    val columnWidthMap: Map<String, Int>
)

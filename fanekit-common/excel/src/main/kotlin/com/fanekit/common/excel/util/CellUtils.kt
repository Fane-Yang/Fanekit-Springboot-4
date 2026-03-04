package com.fanekit.common.excel.util

import org.apache.poi.ss.usermodel.*
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Excel单元格操作工具类
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/19
 * @since 1.0.0
 * @version 1.0.0
 */
object CellUtils {

    /**
     * 设置单元格值
     */
    fun setCellValue(cell: Cell, value: Any?) {
        when (value) {
            null -> cell.setBlank()
            is String -> cell.setCellValue(value)
            is Number -> cell.setCellValue(value.toDouble())
            is Boolean -> cell.setCellValue(value)
            is Date -> cell.setCellValue(value)
            is LocalDateTime -> cell.setCellValue(value.toString())
            is LocalDate -> cell.setCellValue(value.toString())
            else -> cell.setCellValue(value.toString())
        }
    }

    /**
     * 获取单元格值
     */
    fun getCellValue(cell: Cell?): Any? {
        if (cell == null) return null

        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    cell.dateCellValue
                } else {
                    cell.numericCellValue
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue
            CellType.FORMULA -> {
                try {
                    cell.numericCellValue
                } catch (e: Exception) {
                    cell.stringCellValue
                }
            }
            else -> null
        }
    }

    /**
     * 格式化日期时间
     */
    fun formatDate(value: Any?, format: String?): String? {
        return when (value) {
            is LocalDateTime -> value.format(DateTimeFormatter.ofPattern(format ?: "yyyy-MM-dd HH:mm:ss"))
            is LocalDate -> value.format(DateTimeFormatter.ofPattern(format ?: "yyyy-MM-dd"))
            is Date -> SimpleDateFormat(format ?: "yyyy-MM-dd HH:mm:ss").format(value)
            else -> value?.toString()
        }
    }

    /**
     * 解析Excel单元格值到对应字段类型
     */
    fun parseValue(field: Field, value: Any?, format: String?): Any? {
        if (value == null) return null
        val strVal = value.toString().trim()
        if (strVal.isBlank()) return null

        return when (field.type) {
            Int::class.java, Int::class.javaObjectType -> strVal.toIntOrNull()
            Long::class.java, Long::class.javaObjectType -> strVal.toLongOrNull()
            Double::class.java, Double::class.javaObjectType -> strVal.toDoubleOrNull()
            Boolean::class.java, Boolean::class.javaObjectType -> strVal.equals("true", ignoreCase = true)
            LocalDateTime::class.java ->
                LocalDateTime.parse(strVal, DateTimeFormatter.ofPattern(format ?: "yyyy-MM-dd HH:mm:ss"))
            LocalDate::class.java ->
                LocalDate.parse(strVal, DateTimeFormatter.ofPattern(format ?: "yyyy-MM-dd"))
            Date::class.java -> {
                val dateFormat = SimpleDateFormat(format ?: "yyyy-MM-dd HH:mm:ss")
                dateFormat.parse(strVal)
            }
            else -> strVal
        }
    }
}

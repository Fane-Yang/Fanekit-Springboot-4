package com.fanekit.common.excel.util

import com.fanekit.common.excel.enum.ExcelAlign
import com.fanekit.common.excel.enum.ExcelColor
import com.fanekit.common.excel.model.FieldInfo
import org.apache.poi.ss.usermodel.*

/**
 * Excel样式工具类
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/19
 * @since 1.0.0
 * @version 1.0.0
 */
object ExcelStyleUtils {

    /**
     * 创建样式缓存
     */
    fun createStyleCache(workbook: Workbook, fieldInfo: FieldInfo): Map<String, CellStyle> {
        val styleCache = mutableMapOf<String, CellStyle>()

        fieldInfo.fieldOrder.forEach { columnName ->
            val fillColor = fieldInfo.colorMap[columnName] ?: ExcelColor.AUTOMATIC
            val isBold = fieldInfo.boldMap[columnName] ?: false
            val excelAlign = fieldInfo.alignMap[columnName] ?: ExcelAlign.CENTER
            val dataFormat = fieldInfo.cellFormatMap[columnName] ?: ""

            val cellStyle = workbook.createCellStyle()

            // 设置填充色
            if (fillColor != ExcelColor.AUTOMATIC) {
                cellStyle.fillForegroundColor = fillColor.indexedColor.index
                cellStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
            }

            // 对齐方式
            when (excelAlign) {
                ExcelAlign.LEFT -> cellStyle.alignment = HorizontalAlignment.LEFT
                ExcelAlign.CENTER -> cellStyle.alignment = HorizontalAlignment.CENTER
                ExcelAlign.RIGHT -> cellStyle.alignment = HorizontalAlignment.RIGHT
            }

            // 加粗设置
            if (isBold) {
                val font = workbook.createFont()
                font.bold = true
                cellStyle.setFont(font)
            }

            // 数据格式
            if (dataFormat.isNotBlank()) {
                val formatId = workbook.createDataFormat().getFormat(dataFormat)
                cellStyle.dataFormat = formatId
            }

            styleCache[columnName] = cellStyle
        }

        return styleCache
    }

    /**
     * 创建标题样式
     */
    fun createTitleStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        style.alignment = HorizontalAlignment.CENTER
        style.verticalAlignment = VerticalAlignment.CENTER

        val font = workbook.createFont()
        font.fontHeightInPoints = 16
        font.bold = true
        style.setFont(font)

        return style
    }

    /**
     * 创建表头样式
     */
    fun createHeaderStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        style.alignment = HorizontalAlignment.CENTER
        style.verticalAlignment = VerticalAlignment.CENTER

        val font = workbook.createFont()
        font.bold = true
        style.setFont(font)

        return style
    }

    /**
     * 应用列宽
     */
    fun applyColumnWidths(
        sheet: Sheet,
        columnWidthMap: Map<String, Int>,
        fieldOrder: List<String>
    ) {
        fieldOrder.forEachIndexed { colIndex, columnName ->
            val colWidth = (columnWidthMap[columnName] ?: 16) * 256
            sheet.setColumnWidth(colIndex, colWidth.coerceAtMost(65280))
        }
    }
}

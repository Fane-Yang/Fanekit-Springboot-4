package com.fanekit.common.excel.handler

import com.fanekit.common.excel.model.FieldInfo
import com.fanekit.common.excel.util.CellUtils
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler
import org.apache.poi.xssf.usermodel.XSSFComment

/* ========================  内部辅助类  =============================== */

/**
 * 行处理器 - 用于解析Excel行数据为对象
 * @param T 数据类型
 * @param clazz 数据类型的Class对象
 * @param fieldInfo 字段信息，包含字段映射、顺序、转换等
 * @param startLine 开始处理的行号（从0开始）
 * @param batchSize 批处理大小，当达到此大小时触发onBatch回调
 * @param batch 当前批次数据列表
 * @param onBatch 批处理回调函数，当达到batchSize时调用
 * @param onError 错误处理回调函数，当解析行数据出错时调用
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/16
 * @since 1.0.0
 * @version 1.0.0
 */
class RowHandler<T : Any>(
    private val clazz: Class<T>,
    private val fieldInfo: FieldInfo,
    private val startLine: Int,
    private val batchSize: Int,
    private val batch: MutableList<T>,
    private val onBatch: (List<T>) -> Unit,
    private val onError: (Throwable) -> Unit
) : XSSFSheetXMLHandler.SheetContentsHandler {

    private val columnCount = fieldInfo.fieldOrder.size
    private val rowBuf = MutableList<String?>(columnCount) { null }
    private var rowNum = -1

    override fun startRow(rowNum: Int) {
        this.rowNum = rowNum
        for (i in 0 until columnCount) rowBuf[i] = null
    }

    override fun cell(cellReference: String?, formattedValue: String?, comment: XSSFComment?) {
        val col = CellReference(cellReference).col
        if (col in 0 until columnCount) rowBuf[col.toInt()] = formattedValue
    }

    override fun endRow(rowNum: Int) {
        if (rowNum < startLine) return

        try {
            val obj = parseRowToObject(rowBuf, clazz, fieldInfo)
            batch += obj
        } catch (e: Exception) {
            onError(e)
        }

        if (batch.size >= batchSize) {
            onBatch(batch.toList())
            batch.clear()
        }
    }

    override fun headerFooter(text: String?, isHeader: Boolean, tagName: String?) = Unit
    /**
     * 解析行数据为对象 (Row版本)
     */
    fun <T : Any> parseRowToObject(
        row: Row,
        clazz: Class<T>,
        fieldInfo: FieldInfo
    ): T {
        val obj = clazz.getDeclaredConstructor().newInstance()
        var colIndex = 0

        for ((columnName, field) in fieldInfo.fieldMap) {
            if (colIndex >= row.lastCellNum) break
            field.isAccessible = true
            val cell = row.getCell(colIndex)
            var value = CellUtils.getCellValue(cell)

            // 反向转换: (正常 => 0)
            val dictMap = fieldInfo.convertMap[columnName]
            if (!dictMap.isNullOrEmpty()) {
                val kv = dictMap.entries.find { it.value == value.toString() }
                if (kv != null) {
                    value = kv.key
                }
            }

            // 设置到对象字段
            field.set(obj, CellUtils.parseValue(field, value, fieldInfo.timeFormatMap[columnName]))
            colIndex++
        }

        return obj
    }

    /**
     * 解析行数据为对象 (字符串列表版本)
     */
    private fun <T : Any> parseRowToObject(
        values: List<String?>,
        clazz: Class<T>,
        fieldInfo: FieldInfo
    ): T {
        val obj = clazz.getDeclaredConstructor().newInstance()

        fieldInfo.fieldOrder.forEachIndexed { idx, fieldName ->
            val raw = values.getOrNull(idx)?.trim() ?: return@forEachIndexed
            val field = fieldInfo.fieldMap[fieldName] ?: return@forEachIndexed
            field.isAccessible = true

            // 1) 字典反向转换 (从显示值转换回原始值)
            var value: Any? = fieldInfo.convertMap[fieldName]?.let { dictMap ->
                if (dictMap.isNotEmpty()) {
                    // 查找显示值对应的原始值
                    val kv = dictMap.entries.find { it.value == raw }
                    kv?.key ?: raw
                } else raw
            } ?: raw

            // 2) 使用CellUtils.parseValue进行类型转换和日期时间格式处理
            val finalValue = CellUtils.parseValue(field, value, fieldInfo.timeFormatMap[fieldName])
            field.set(obj, finalValue)
        }
        return obj
    }
}
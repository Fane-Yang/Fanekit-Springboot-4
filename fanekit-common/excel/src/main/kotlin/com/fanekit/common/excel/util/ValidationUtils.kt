package com.fanekit.common.excel.util

import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddressList

/**
 * Excel数据验证工具类
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/18
 * @since 1.0.0
 * @version 1.0.0
 */
object ValidationUtils {

    /**
     * 设置列下拉选择及校验
     */
    fun applyColumnValidation(
        sheet: Sheet,
        convertMap: Map<String, Map<String, String>>,
        totalRows: Int,
        fieldOrder: List<String>,
        startRow: Int = 2
    ) {
        val dataValidationHelper = sheet.dataValidationHelper
        val endRow = startRow + totalRows - 1

        fieldOrder.forEachIndexed { colIndex, columnName ->
            val dict = convertMap[columnName]
            if (!dict.isNullOrEmpty()) {
                val textArray = dict.values.toTypedArray()
                val addressList = CellRangeAddressList(startRow, endRow, colIndex, colIndex)
                val dvConstraint = dataValidationHelper.createExplicitListConstraint(textArray)
                val validation = dataValidationHelper.createValidation(dvConstraint, addressList)

                validation.errorStyle = DataValidation.ErrorStyle.STOP
                validation.showErrorBox = true
                validation.createErrorBox("错误", "只能选择 [${textArray.joinToString()}] 之中的值！")
                validation.suppressDropDownArrow = true
                sheet.addValidationData(validation)
            }
        }
    }
}

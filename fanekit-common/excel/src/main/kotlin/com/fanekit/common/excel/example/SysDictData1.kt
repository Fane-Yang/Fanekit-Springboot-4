package com.fanekit.common.excel.example

import com.fanekit.common.excel.annotation.Excel
import com.fanekit.common.excel.enum.ExcelAlign
import com.fanekit.common.excel.enum.ExcelColor
import java.util.*

/**
 * 测试实体类 SysDictData
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/23
 * @since 1.0.0
 * @version 1.0.0
 */
data class SysDictData1(
    @Excel(name = "字典Label", converterExp = "0=无效,1=有效", color = ExcelColor.LIGHT_YELLOW)
    var dictLabel: String? = null,

    @Excel(name = "字典值", converterExp = "A=苹果,B=香蕉,C=梨子", color = ExcelColor.LIGHT_GREEN, cellFormat = "@")
    var dictValue: String? = null,

    @Excel(name = "字典Code", ignoreImport = false, cellFormat = "0", align = ExcelAlign.RIGHT)
    var dictCode: Long? = null,

    @Excel(name = "样式测试列", ignoreExport = true, ignoreImport = true)
    var cssClass: String? = null,

    @Excel(name = "是否默认", converterExp = "Y=是,N=否", cellFormat = "@", width = 25)
    var isDefault: String? = null,

    @Excel(name = "日期", cellFormat = "yyyy-MM-dd HH:mm:ss", bold = true)
    var date: Date? = null
)
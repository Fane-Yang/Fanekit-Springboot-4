package com.fanekit.common.excel.annotation

import com.fanekit.common.excel.enum.ExcelAlign
import com.fanekit.common.excel.enum.ExcelColor

/**
 * Excel 注解
 * @param name         列名
 * @param converterExp 格式化表达式，如 "0=正常,1=停用"，导出Excel时0 ->【正常】，导入Excel时【正常】-> 0
 * @param color        单元格填充色
 * @param timeFormat   时间格式，如 "yyyy-MM-dd HH:mm:ss"
 * @param ignoreExport 是否跳过导出
 * @param ignoreImport 是否跳过导入
 * @param bold         是否加粗
 * @param width        列宽
 * @param align        水平对齐方式
 * @param cellFormat   Excel本身的单元格格式，如"0"数字格式,"0.00"数字格式、"yyyy-MM-dd"日期格式、"@"文本等,设置时间格式优先使用此方法,不要使用timeFormat
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/5
 * @since 1.0.0
 * @version 1.0.0
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Excel(
    val name: String = "",
    val converterExp: String = "",
    val color: com.fanekit.common.excel.enum.ExcelColor = _root_ide_package_.com.fanekit.common.excel.enum.ExcelColor.AUTOMATIC,
    @Deprecated("此参数用于设置时间类型的格式化字符串，建议使用cellFormat 设置时间显示格式，而不是设置为字符串", ReplaceWith("cellFormat"))
    val timeFormat: String = "yyyy-MM-dd HH:mm:ss",
    val ignoreExport: Boolean = false,
    val ignoreImport: Boolean = false,
    val bold: Boolean = false,
    val width: Int = 16,
    val align: com.fanekit.common.excel.enum.ExcelAlign = _root_ide_package_.com.fanekit.common.excel.enum.ExcelAlign.CENTER,
    /**
     * 新增：Excel本身单元格格式（DataFormat），
     * 例如
     * "0.00" 表示数字格式保留两位小数，
     * "0" 表示数字格式不保留小数，
     * "yyyy-MM-dd" 表示日期格式，
     * "yyyy-MM-dd HH:mm:ss" 表示日期时间格式，
     * "yyyy-MM-dd HH:mm:ss.SSS" 表示日期时间格式带毫秒，
     * "@" 表示文本格式
     * 注意：此格式会覆盖 timeFormat 的设置，
     */
    val cellFormat: String = ""
)


package com.fanekit.common.excel.util

import com.fanekit.common.excel.annotation.Excel
import com.fanekit.common.excel.enum.ExcelAlign
import com.fanekit.common.excel.enum.ExcelColor
import com.fanekit.common.excel.model.FieldInfo
import org.slf4j.LoggerFactory
import java.lang.reflect.Field

/**
 * 字段解析工具类
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/19
 * @since 1.0.0
 * @version 1.0.0
 */
object FieldParseUtils {

    private val log = LoggerFactory.getLogger(FieldParseUtils::class.java)

    /**
     * 解析字段信息
     */
    fun <T : Any> parseFieldInfo(clazz: Class<T>, includeSuperClass: Boolean = false): FieldInfo {
        val fieldMap = mutableMapOf<String, Field>()
        val convertMap = mutableMapOf<String, Map<String, String>>()
        val timeFormatMap = mutableMapOf<String, String>()
        val colorMap = mutableMapOf<String, ExcelColor>()
        val cellFormatMap = mutableMapOf<String, String>()
        val fieldOrder = mutableListOf<String>()
        val boldMap = mutableMapOf<String, Boolean>()
        val alignMap = mutableMapOf<String, ExcelAlign>()
        val columnWidthMap = mutableMapOf<String, Int>()


        // 根据 includeSuperClass 参数决定是否包含父类字段
        val fields = if (includeSuperClass) getAllFields(clazz) else clazz.declaredFields.toList()
        
        fields
            // 过滤掉 [内部类的外部类引用/Lambda 表达式][静态字段][序列化ID]等不需要导出的字段
            .filter { !it.isSynthetic && it.name != "serialVersionUID" && it.name != "Companion" }
            .forEach { field ->
                val excel = field.getAnnotation(Excel::class.java)
                // 如果标记跳过导出则忽略此字段
                if (excel?.ignoreExport == true) return@forEach
                val columnName = if (!excel?.name.isNullOrEmpty()) excel!!.name else field.name
                fieldMap[columnName] = field
                fieldOrder.add(columnName)
                // 解析表达式
                convertMap[columnName] = parseConverterExp(excel?.converterExp ?: "")
                // 优先使用cellFormat，如果为空则使用timeFormat
                val timeFormat = if (!excel?.cellFormat.isNullOrEmpty()) {
                    excel.cellFormat
                } else {
                    @Suppress("DEPRECATION")
                    excel?.timeFormat ?: ""
                }
                timeFormatMap[columnName] = timeFormat
                // 记录Excel单元格格式
                cellFormatMap[columnName] = excel?.cellFormat ?: ""
                // 记录对应注解的属性
                colorMap[columnName] = excel?.color ?: ExcelColor.AUTOMATIC
                boldMap[columnName] = excel?.bold ?: false
                alignMap[columnName] = excel?.align ?: ExcelAlign.CENTER
                columnWidthMap[columnName] = excel?.width ?: 16
            }

        return FieldInfo(
            fieldMap, convertMap, timeFormatMap, colorMap, cellFormatMap,
            fieldOrder, boldMap, alignMap, columnWidthMap
        )
    }

    /**
     * 解析 "0=正常,1=停用" 这种形式的表达式
     */
    fun parseConverterExp(exp: String): Map<String, String> {
        if (exp.isBlank()) return emptyMap()
        return exp.split(",")
            .mapNotNull {
                val kv = it.split("=")
                if (kv.size == 2) kv[0] to kv[1] else null
            }
            .toMap()
    }

    /**
     * 获取类及其父类的所有字段
     * @param clazz 目标类
     * @return 包含所有字段的列表
     */
    fun getAllFields(clazz: Class<*>): List<Field> {
        val fields = mutableListOf<Field>()
        var currentClass: Class<*>? = clazz
        while (currentClass != null) {
            fields.addAll(currentClass.declaredFields)
            currentClass = currentClass.superclass
        }
        return fields
    }
}

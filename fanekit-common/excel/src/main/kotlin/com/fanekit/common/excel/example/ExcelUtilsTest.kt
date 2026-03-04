package com.fanekit.common.excel.example

import com.fanekit.common.excel.ExcelUtils
import java.util.*

fun main() {
    ExcelUtilsTest()
}

/**
 * 测试方法
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/23
 * @since 1.0.0
 * @version 1.0.0
 */
fun ExcelUtilsTest() {
    // 准备测试数据
    val list = mutableListOf<SysDictData1>()

    val data1 = SysDictData1().apply {
        dictLabel = "1"        // 导出时会被替换为"有效"
        dictValue = "A"        // 导出时会被替换为"苹果"
        dictCode = 100001
        isDefault = "Y"        // 导出时 -> "是"
        date = Date()
    }
    list.add(data1)

    val data2 = SysDictData1().apply {
        dictLabel = "0"        // 导出时 -> "无效"
        dictValue = "C"        // -> "梨子"
        dictCode = 99999999
        isDefault = "N"
        cssClass = "这个字段被ignoreExport了"
        date = Date()
    }
    list.add(data2)

    // 导出
    val exportPath = "E:\\WEB\\testDict.xlsx"
    ExcelUtils.write(list, SysDictData1::class.java, exportPath, "字典数据导出")
    println("导出完成：$exportPath")

    // 导入
    val imported = ExcelUtils.read(exportPath, SysDictData1::class.java, 2)
    println("导入解析后：")
    imported.forEach { println(it) }
}
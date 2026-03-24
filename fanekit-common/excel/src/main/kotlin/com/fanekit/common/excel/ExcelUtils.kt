package com.fanekit.common.excel

import com.fanekit.common.excel.callback.ReadProgressCallback
import com.fanekit.common.excel.callback.WriteProgressCallback
import com.fanekit.common.excel.model.SheetInfo
import com.fanekit.common.excel.provider.DataProvider
import com.fanekit.common.excel.reader.ExcelReader
import com.fanekit.common.excel.writer.ExcelWriter

/**
 * Excel 工具类 - 简洁的门面接口
 * 提供传统模式和大数据模式的Excel导入导出功能
 * 支持指定Sheet、指定行写入等高级功能
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/21
 * @since 1.0.0
 * @version 1.0.0
 */
object ExcelUtils {

    /**
     * 导出数据到Excel（适用于中小数据量）
     * @param list 要导出的数据列表
     * @param clazz 数据类型（解析字段上的@Excel注解）
     * @param filePath 导出后保存的路径
     * @param userTitle 标题
     * @param applyColumnValidation 是否应用导出时数据校验功能
     * @param applyCellStyles 是否应用单元格样式
     * @param sheetName 指定Sheet名称（可选）
     * @param sheetIndex 指定Sheet索引（可选）
     * @param startRow 开始写入的行号，0表示从第一行开始，>0表示追加模式
     */
    fun <T : Any> write(
        list: List<T>,
        clazz: Class<T>,
        filePath: String,
        userTitle: String = "标题",
        applyColumnValidation: Boolean = true,
        applyCellStyles: Boolean = true,
        sheetName: String? = null,
        sheetIndex: Int? = null,
        startRow: Int = 0,
        includeSuperClass: Boolean = false
    ) {
        ExcelWriter.write(
            list, clazz, filePath, userTitle, applyColumnValidation,
            applyCellStyles, sheetName, sheetIndex, startRow,includeSuperClass
        )
    }

    /**
     * 导入Excel到List数据（传统模式 - 适用于中小数据量）
     * @param filePath Excel文件路径
     * @param clazz 数据类型
     * @param startLine 默认从2开始，即跳过第0行标题行,跳过第1行字段名称行
     * @param sheetIndex 指定Sheet索引（可选）
     * @param sheetName 指定Sheet名称（可选）
     * @param includeSuperClass 是否包含父类字段，默认false
     * @return 导入的数据列表
     */
    fun <T : Any> read(
        filePath: String,
        clazz: Class<T>,
        startLine: Int = 2,
        sheetIndex: Int? = null,
        sheetName: String? = null,
        includeSuperClass: Boolean = false
    ): List<T> {
        return ExcelReader.read(
            filePath, 
            clazz, 
            startLine, 
            sheetIndex, 
            sheetName, 
            includeSuperClass)
    }

    /**
     * 大数据量导出Excel（Big模式 - 适用于百万级数据量）
     * @param dataProvider 数据提供者
     * @param clazz 数据类型
     * @param filePath 导出文件路径
     * @param batchSize 每批处理数量，默认100
     * @param userTitle 标题
     * @param callback 进度回调
     * @param applyColumnValidation 是否应用数据校验
     * @param applyCellStyles 是否应用单元格样式
     * @param sheetName 指定Sheet名称（可选）
     * @param sheetIndex 指定Sheet索引（可选）
     * @param startRow 开始写入的行号，0表示从第一行开始，>0表示追加模式
     * @param includeSuperClass 是否包含父类字段，默认false
     */
    fun <T : Any> writeBig(
        dataProvider: DataProvider<T>,
        clazz: Class<T>,
        filePath: String,
        batchSize: Int = 100,
        userTitle: String = "标题",
        callback: WriteProgressCallback? = null,
        applyColumnValidation: Boolean = true,
        applyCellStyles: Boolean = true,
        sheetName: String? = null,
        sheetIndex: Int? = null,
        startRow: Int = 0,
        includeSuperClass: Boolean = false
    ) {
        ExcelWriter.writeBig(
            dataProvider, clazz, filePath, batchSize, userTitle,
            callback, applyColumnValidation, applyCellStyles,
            sheetName, sheetIndex, startRow, includeSuperClass
        )
    }

    /**
     * 大数据量导入Excel（Big模式 - 适用于百万级数据量）
     * 测试27.8M的一百万条数据读取仅占用内存12M，耗时30秒左右
     * 使用流式读取避免内存溢出
     * @param filePath 文件路径
     * @param clazz 数据类型
     * @param batchSize 每批处理数量，默认100，导入最好100
     * @param callback 进度回调
     * @param startLine 开始行号，默认从第2行开始
     * @param maxFileSize 最大文件大小限制，默认50MB
     * @param sheetIndex 指定Sheet索引（可选）
     * @param sheetName 指定Sheet名称（可选）
     * @param includeSuperClass 是否包含父类字段，默认false
     */
    fun <T : Any> readBig(
        filePath: String,
        clazz: Class<T>,
        batchSize: Int = 100,
        callback: ReadProgressCallback<T>? = null,
        startLine: Int = 2,
        maxFileSize: Int = 50 * 1024 * 1024,
        sheetIndex: Int? = null,
        sheetName: String? = null,
        includeSuperClass: Boolean = false
    ) {
        ExcelReader.readBig(
            filePath, clazz, batchSize, callback, startLine,
            maxFileSize, sheetIndex, sheetName, includeSuperClass
        )
    }

    // ========== Sheet管理功能 ==========

    /**
     * 获取Excel文件中所有Sheet的信息
     * @param filePath Excel文件路径
     * @return Sheet信息列表
     */
    fun getSheetInfo(filePath: String): List<SheetInfo> {
        return ExcelReader.getSheetInfo(filePath)
    }

    /**
     * 获取Excel文件中Sheet的总数
     * @param filePath Excel文件路径
     * @return Sheet总数
     */
    fun getSheetCount(filePath: String): Int {
        return ExcelReader.getSheetCount(filePath)
    }

    /**
     * 创建新的Excel文件
     * @param filePath 文件路径
     * @param sheetNames Sheet名称列表，默认创建一个名为"Sheet1"的Sheet
     * @return 是否创建成功
     */
    fun createExcelFile(filePath: String, sheetNames: List<String> = listOf("Sheet1")): Boolean {
        return ExcelWriter.createExcelFile(filePath, sheetNames)
    }

    /**
     * 向现有Excel文件添加新Sheet
     * @param filePath 文件路径
     * @param sheetName 新Sheet名称
     * @return 是否添加成功
     */
    fun addSheetToExistingFile(filePath: String, sheetName: String): Boolean {
        return ExcelWriter.addSheetToExistingFile(filePath, sheetName)
    }

    // ========== 便捷方法 ==========

    /**
     * 追加数据到现有Excel文件的末尾
     * @param list 要追加的数据列表
     * @param clazz 数据类型
     * @param filePath Excel文件路径
     * @param sheetIndex 目标Sheet索引，默认第一个Sheet
     * @param sheetName 目标Sheet名称（优先级低于sheetIndex）
     */
    fun <T : Any> appendData(
        list: List<T>,
        clazz: Class<T>,
        filePath: String,
        sheetIndex: Int? = 0,
        sheetName: String? = null,
        includeSuperClass: Boolean = false
    ) {
        // 先获取目标Sheet的最后一行
        val sheetInfo = getSheetInfo(filePath)
        val targetSheet = when {
            sheetIndex != null && sheetIndex < sheetInfo.size -> sheetInfo[sheetIndex]
            sheetName != null -> sheetInfo.find { it.name == sheetName }
            else -> sheetInfo.firstOrNull()
//        } ?: throw IllegalArgumentException("无法找到目标 Sheet")
        } ?: throw IllegalArgumentException("无法找到目标 Sheet")

        // 从最后一行的下一行开始写入
        val startRow = targetSheet.totalRows

        write(
            list = list,
            clazz = clazz,
            filePath = filePath,
            startRow = startRow,
            sheetIndex = sheetIndex,
            sheetName = sheetName,
            includeSuperClass = includeSuperClass
        )
    }

    /**
     * 显示Excel文件的基本信息
     * @param filePath Excel文件路径
     */
    fun printExcelInfo(filePath: String) {
        try {
            val sheetInfoList = getSheetInfo(filePath)
            println("=== Excel文件信息 ===")
            println("文件路径: $filePath")
            println("Sheet总数: ${sheetInfoList.size}")
            println("详细信息:")
            sheetInfoList.forEach { sheetInfo ->
                println("  $sheetInfo")
            }
        } catch (e: Exception) {
            println("读取Excel文件信息失败: ${e.message}")
        }
    }
}



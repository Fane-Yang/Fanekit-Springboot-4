package com.fanekit.common.excel.util

import com.fanekit.common.excel.model.FieldInfo
import com.fanekit.common.excel.model.SheetInfo
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.eventusermodel.XSSFReader
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Excel工作表操作工具类
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/18
 * @since 1.0.0
 * @version 1.0.0
 */
object SheetUtils {

    /**
     * 写入标题和表头
     */
    fun writeHeaderAndTitle(
        workbook: SXSSFWorkbook,
        sheet: Sheet,
        userTitle: String,
        fieldOrder: List<String>
    ): Int {
        val colCount = fieldOrder.size

        // 第0行：合并单元格并设置为大标题
        val titleRow = sheet.createRow(0)
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue(userTitle)

        // 合并标题单元格
        if (colCount > 1) {
            sheet.addMergedRegion(CellRangeAddress(0, 0, 0, colCount - 1))
        }

        // 设置标题样式
        val titleStyle = ExcelStyleUtils.createTitleStyle(workbook)
        titleCell.cellStyle = titleStyle
        titleRow.heightInPoints = 30f

        // 第1行：写字段表头
        val headerRow = sheet.createRow(1)
        fieldOrder.forEachIndexed { index, columnName ->
            val headerCell = headerRow.createCell(index)
            headerCell.setCellValue(columnName)
            // 可以为表头设置样式
            val headerStyle = ExcelStyleUtils.createHeaderStyle(workbook)
            headerCell.cellStyle = headerStyle
        }

        return 2 // 返回数据开始行号
    }

    /**
     * 写入批次数据行并应用样式
     */
    fun <T : Any> writeBatchDataRowWithStyles(
        dataRow: Row,
        item: T,
        fieldInfo: FieldInfo,
        styleCache: Map<String, CellStyle>
    ) {
        fieldInfo.fieldOrder.forEachIndexed { colIndex, columnName ->
            val field = fieldInfo.fieldMap[columnName]!!
            field.isAccessible = true
            var value = field.get(item)

            // 处理转换: (0 => 正常)
            val dictMap = fieldInfo.convertMap[columnName]
            if (!dictMap.isNullOrEmpty()) {
                value = dictMap[value?.toString()] ?: value
            }

            // 处理时间格式
            if (value is LocalDateTime || value is LocalDate || value is Date) {
                val format = fieldInfo.timeFormatMap[columnName]
                value = CellUtils.formatDate(value, format)
            }

            val cell = dataRow.createCell(colIndex)
            CellUtils.setCellValue(cell, value)

            // 应用样式
            val cellStyle = styleCache[columnName]
            if (cellStyle != null) {
                cell.cellStyle = cellStyle
            }
        }
    }

    /**
     * 获取指定Sheet的输入流
     */
    private fun getSheetInputStream(
        reader: XSSFReader,
        sheetIndex: Int?,
        sheetName: String?
    ): InputStream {
        val sheetsData = reader.getSheetsData()

        return when {
            sheetIndex != null -> {
                // 通过索引获取Sheet
                if (sheetIndex < 0) {
                    throw IllegalArgumentException("Sheet索引不能为负数: $sheetIndex")
                }

                var currentIndex = 0
                while (sheetsData.hasNext() && currentIndex < sheetIndex) {
                    val unusedStream = sheetsData.next()
                    try {
                        unusedStream.close() // 关闭不需要的流
                    } catch (e: Exception) {
                        // 忽略关闭错误
                    }
                    currentIndex++
                }

                if (!sheetsData.hasNext()) {
                    throw IllegalArgumentException("Sheet索引超出范围: $sheetIndex")
                }

                sheetsData.next()
            }
            sheetName != null -> {
                // 通过名称获���Sheet - 需要解析workbook.xml获取Sheet名称与ID的映射关系
                val sheetNameToIdMap = parseWorkbookSheetInfo(reader)
                val targetSheetId = sheetNameToIdMap[sheetName]
                    ?: throw IllegalArgumentException("未找到名为 '$sheetName' 的Sheet")

                // 根据Sheet ID获取对应的输入流
                getSheetInputStreamById(reader, targetSheetId)
            }
            else -> {
                // 默认第一个Sheet
                sheetsData.next()
            }
        }
    }

    /**
     * 解析workbook.xml获取Sheet名称与ID的映射关系
     */
    private fun parseWorkbookSheetInfo(reader: XSSFReader): Map<String, String> {
        val sheetNameToIdMap = mutableMapOf<String, String>()

        try {
            val workbookXml = reader.workbookData
            val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(workbookXml)

            // 查找所有的sheet节点
            val sheetNodes = document.getElementsByTagName("sheet")
            for (i in 0 until sheetNodes.length) {
                val sheetNode = sheetNodes.item(i)
                val attributes = sheetNode.attributes

                val name = attributes.getNamedItem("name")?.nodeValue
                val sheetId = attributes.getNamedItem("sheetId")?.nodeValue
                val rId = attributes.getNamedItem("r:id")?.nodeValue

                if (name != null && rId != null) {
                    sheetNameToIdMap[name] = rId
                }
            }

            workbookXml.close()
        } catch (e: Exception) {
            throw RuntimeException("解析workbook.xml失败: ${e.message}", e)
        }

        return sheetNameToIdMap
    }

    /**
     * 根据Sheet的关系ID获取对应的输入流
     */
    private fun getSheetInputStreamById(reader: XSSFReader, relationshipId: String): InputStream {
        // XSSFReader.SheetIterator提供了按关系ID获取Sheet的功能
        val sheetIterator = reader.sheetsData as XSSFReader.SheetIterator

        while (sheetIterator.hasNext()) {
            val sheetStream = sheetIterator.next()
            val currentSheetId = sheetIterator.sheetPart.partName.name

            // 检查是否是目标Sheet
            // 关系ID格式通常是rId1, rId2等，需要从part名称中提取对应关系
            if (isTargetSheet(currentSheetId, relationshipId)) {
                return sheetStream
            } else {
                // 不是目标Sheet，关闭流
                try {
                    sheetStream.close()
                } catch (e: Exception) {
                    // 忽略关闭错误
                }
            }
        }

        throw IllegalArgumentException("未找到关系ID为 '$relationshipId' 的Sheet")
    }

    /**
     * 检查当前Sheet是否为目标Sheet
     */
    private fun isTargetSheet(partName: String, relationshipId: String): Boolean {
        // partName格式类似: /xl/worksheets/sheet1.xml
        // relationshipId格式: rId1, rId2等

        // 提取数字部分进行匹配
        val partNumber = partName.substringAfterLast("sheet")
            .substringBefore(".xml")
            .toIntOrNull() ?: return false

        val ridNumber = relationshipId.substringAfter("rId").toIntOrNull() ?: return false

        // 通常情况下，sheet1.xml对应rId1，sheet2.xml���应rId2
        return partNumber == ridNumber
    }

    // ==================== Sheet 管理功能 (从 SheetManager 合并) ====================

    /**
     * 获取Excel文件中的所有Sheet信息
     */
    fun getSheetInfo(filePath: String): List<SheetInfo> {
        FileInputStream(filePath).use { inputStream ->
            val workbook = XSSFWorkbook(inputStream)
            val sheetInfoList = mutableListOf<SheetInfo>()

            for (i in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(i)
                sheetInfoList.add(
                    SheetInfo(
                        index = i,
                        name = sheet.sheetName,
                        totalRows = sheet.lastRowNum + 1,
                        isHidden = workbook.isSheetHidden(i),
                        isVeryHidden = workbook.isSheetVeryHidden(i)
                    )
                )
            }

            workbook.close()
            return sheetInfoList
        }
    }

    /**
     * 获取Sheet总数
     */
    fun getSheetCount(filePath: String): Int {
        FileInputStream(filePath).use { inputStream ->
            val workbook = XSSFWorkbook(inputStream)
            val count = workbook.numberOfSheets
            workbook.close()
            return count
        }
    }

    /**
     * 检查指定Sheet是否存在
     */
    fun sheetExists(filePath: String, sheetIndex: Int): Boolean {
        FileInputStream(filePath).use { inputStream ->
            val workbook = XSSFWorkbook(inputStream)
            val exists = sheetIndex >= 0 && sheetIndex < workbook.numberOfSheets
            workbook.close()
            return exists
        }
    }

    /**
     * 检查指定名称的Sheet是否存在
     */
    fun sheetExists(filePath: String, sheetName: String): Boolean {
        FileInputStream(filePath).use { inputStream ->
            val workbook = XSSFWorkbook(inputStream)
            val exists = workbook.getSheet(sheetName) != null
            workbook.close()
            return exists
        }
    }

    /**
     * 根据名称获取Sheet索引
     */
    fun getSheetIndex(filePath: String, sheetName: String): Int {
        FileInputStream(filePath).use { inputStream ->
            val workbook = XSSFWorkbook(inputStream)
            val index = workbook.getSheetIndex(sheetName)
            workbook.close()
            return index
        }
    }

    /**
     * 验证Sheet参数（索引或名称）
     */
    fun validateSheetParam(workbook: Workbook, sheetIndex: Int?, sheetName: String?): Int {
        return when {
            sheetIndex != null -> {
                if (sheetIndex < 0 || sheetIndex >= workbook.numberOfSheets) {
                    throw IllegalArgumentException("Sheet索引超出范围: $sheetIndex，可用范围: 0-${workbook.numberOfSheets - 1}")
                }
                sheetIndex
            }
            sheetName != null -> {
                val index = workbook.getSheetIndex(sheetName)
                if (index == -1) {
                    throw IllegalArgumentException("Sheet名称不存在: $sheetName")
                }
                index
            }
            else -> {
                // 默认使用第一个Sheet
                if (workbook.numberOfSheets == 0) {
                    throw IllegalArgumentException("Excel文件中没有Sheet")
                }
                0
            }
        }
    }
}

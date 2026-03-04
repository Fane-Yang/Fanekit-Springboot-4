package com.fanekit.common.excel.writer

import com.fanekit.common.excel.callback.WriteProgressCallback
import com.fanekit.common.excel.config.ExcelConfig
import com.fanekit.common.excel.provider.DataProvider
import com.fanekit.common.excel.util.ExcelStyleUtils
import com.fanekit.common.excel.util.FieldParseUtils
import com.fanekit.common.excel.util.SheetUtils
import com.fanekit.common.excel.util.ValidationUtils
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Excel写入器 - 专门负责Excel文件的写入功能
 * 支持传统模式和大数据模式的导出，支持指定Sheet和起始行写入
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/20
 * @since 1.0.0
 * @version 1.0.0
 */
object ExcelWriter {

    /**
     * List数据提供者 - 用于将List包装为DataProvider
     */
    private class ListDataProvider<T>(private val list: List<T>) : DataProvider<T> {
        override fun getTotalCount(): Long = list.size.toLong()

        override fun getBatchData(offset: Long, limit: Int): List<T> {
            val startIndex = offset.toInt()
            val endIndex = minOf(startIndex + limit, list.size)
            return if (startIndex < list.size) {
                list.subList(startIndex, endIndex)
            } else {
                emptyList()
            }
        }
    }

    /**
     * 传统模式写入 - 适用于中小数据量
     */
    fun <T : Any> write(
        list: List<T>,
        clazz: Class<T>,
        filePath: String,
        userTitle: String = ExcelConfig.Default.TITLE,
        applyColumnValidation: Boolean = true,
        applyCellStyles: Boolean = true,
        sheetName: String? = null,
        sheetIndex: Int? = null,
        startRow: Int = 0,
        includeSuperClass: Boolean = false
    ) {
        val dataProvider = ListDataProvider(list)

        writeBig(
            dataProvider = dataProvider,
            clazz = clazz,
            filePath = filePath,
            batchSize = list.size.coerceAtMost(ExcelConfig.Batch.EXPORT_MAX_BATCH_SIZE),
            userTitle = userTitle,
            callback = null,
            applyColumnValidation = applyColumnValidation,
            applyCellStyles = applyCellStyles,
            sheetName = sheetName,
            sheetIndex = sheetIndex,
            startRow = startRow,
            includeSuperClass = includeSuperClass
        )
    }

    /**
     * 大数据量写入模式 - 支持百万级数据
     */
    fun <T : Any> writeBig(
        dataProvider: DataProvider<T>,
        clazz: Class<T>,
        filePath: String,
        batchSize: Int = ExcelConfig.Batch.BIG_MODE_BATCH_SIZE,
        userTitle: String = ExcelConfig.Default.TITLE,
        callback: WriteProgressCallback? = null,
        applyColumnValidation: Boolean = true,
        applyCellStyles: Boolean = true,
        sheetName: String? = null,
        sheetIndex: Int? = null,
        startRow: Int = 0,
        includeSuperClass: Boolean = false
    ) {
        val totalCount = dataProvider.getTotalCount()
        val totalBatches = if (totalCount > 0) ((totalCount + batchSize - 1) / batchSize).toInt() else 0
        var processedCount = 0L

        // 判断是新建文件还是追加到现有文件
        val isNewFile = !File(filePath).exists() || startRow == 0
        val workbook: Workbook
        val sheet: Sheet
        var actualStartRow = startRow

        if (isNewFile) {
            // 新建文件或从第一行开始写入
            workbook = SXSSFWorkbook(ExcelConfig.Memory.SXSSF_WINDOW_SIZE)
            sheet = if (sheetName != null) {
                workbook.createSheet(sheetName)
            } else {
                workbook.createSheet("Sheet1")
            }
        } else {
            // 追加到现有文件 - 特殊处理避免SXSSFWorkbook访问限制
            val existingWorkbook = FileInputStream(filePath).use { XSSFWorkbook(it) }

            // 获取目标Sheet信息
            val targetSheetIndex = SheetUtils.validateSheetParam(existingWorkbook, sheetIndex, sheetName)
            val existingSheet = existingWorkbook.getSheetAt(targetSheetIndex)
            val existingSheetName = existingSheet.sheetName

            println("   追加到现有Sheet: $existingSheetName (索引: $targetSheetIndex)，从第${startRow + 1}行开始")

            // 关闭原有workbook
            existingWorkbook.close()

            // 创建新的SXSSFWorkbook，从现有文件重新加载
            FileInputStream(filePath).use { inputStream ->
                val tempWorkbook = XSSFWorkbook(inputStream)
                workbook = SXSSFWorkbook(tempWorkbook, ExcelConfig.Memory.SXSSF_WINDOW_SIZE)
                sheet = workbook.getSheetAt(targetSheetIndex)
            }
        }

        try {
            if (totalCount == 0L) {
                if (isNewFile) {
                    val row = sheet.createRow(0)
                    val cell = row.createCell(0)
                    cell.setCellValue("无数据")
                }
                FileOutputStream(filePath).use { workbook.write(it) }
                callback?.onCompleted(0, filePath)
                return
            }

            // 解析字段信息
            val fieldInfo = FieldParseUtils.parseFieldInfo(clazz,includeSuperClass)

            // 预创建样式缓存
            val styleCache = if (applyCellStyles) {
                ExcelStyleUtils.createStyleCache(workbook, fieldInfo)
            } else {
                emptyMap()
            }

            val writeHeader = isNewFile || startRow == 0

            // 写入标题和表头（仅在新文件或从第一行开始时）
            if (writeHeader) {
                actualStartRow = writeHeaderAndTitleForBigFile(workbook, sheet, userTitle, fieldInfo.fieldOrder, applyCellStyles)

                // 设置列宽
                if (applyCellStyles) {
                    ExcelStyleUtils.applyColumnWidths(sheet, fieldInfo.columnWidthMap, fieldInfo.fieldOrder)
                }
            } else {
                actualStartRow = startRow
            }

            // 分批处理数据
            for (batchIndex in 0 until totalBatches) {
                try {
                    val offset = batchIndex * batchSize.toLong()
                    val batchData = dataProvider.getBatchData(offset, batchSize)

                    if (batchData.isEmpty()) break

                    // 写入当前批次数据
                    batchData.forEach { item ->
                        val dataRow = sheet.createRow(actualStartRow++)
                        SheetUtils.writeBatchDataRowWithStyles(dataRow, item, fieldInfo, styleCache)
                    }

                    processedCount += batchData.size
                    val remainingCount = totalCount - processedCount

                    // 回调进度
                    callback?.onProgress(
                        processedCount = processedCount,
                        totalCount = totalCount,
                        remainingCount = remainingCount,
                        currentBatch = batchIndex + 1,
                        totalBatches = totalBatches,
                        batchSize = batchData.size
                    )

                } catch (e: Exception) {
                    callback?.onError(e, processedCount)
                    throw e
                }
            }

            // 应用数据验证（仅在写入表头时）
            if (applyColumnValidation && writeHeader) {
                ValidationUtils.applyColumnValidation(sheet, fieldInfo.convertMap, processedCount.toInt(), fieldInfo.fieldOrder)
            }

            // 输出到文件
            FileOutputStream(filePath).use { workbook.write(it) }
            callback?.onCompleted(processedCount, filePath)

        } catch (e: Exception) {
            callback?.onError(e, processedCount)
            throw e
        } finally {
            workbook.close()
        }
    }

    /**
     * 专门为大文件模式写入标题和表头
     * 避免SXSSFWorkbook的行访问限制问题
     */
    private fun writeHeaderAndTitleForBigFile(
        workbook: Workbook,
        sheet: Sheet,
        userTitle: String,
        fieldOrder: List<String>,
        applyCellStyles: Boolean
    ): Int {
        val colCount = fieldOrder.size

        // 第0行：合并单元格并设置为大标题
        val titleRow = sheet.createRow(0)
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue(userTitle)

        // 合并标题单元格
        if (colCount > 1) {
            sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, colCount - 1))
        }

        // 设置标题样式
        if (applyCellStyles) {
            val titleStyle = ExcelStyleUtils.createTitleStyle(workbook)
            titleCell.cellStyle = titleStyle
            titleRow.heightInPoints = 30f
        }

        // 第1行：写字段表头
        val headerRow = sheet.createRow(1)
        fieldOrder.forEachIndexed { index, columnName ->
            val headerCell = headerRow.createCell(index)
            headerCell.setCellValue(columnName)

            // 设置表头样式
            if (applyCellStyles) {
                val headerStyle = ExcelStyleUtils.createHeaderStyle(workbook)
                headerCell.cellStyle = headerStyle
            }
        }

        // 返回数据开始写入的行号
        return 2
    }

    /**
     * 创建新的Excel文件
     */
    fun createExcelFile(filePath: String, sheetNames: List<String> = listOf("Sheet1")): Boolean {
        return try {
            val workbook = SXSSFWorkbook()

            sheetNames.forEach { sheetName ->
                workbook.createSheet(sheetName)
            }

            FileOutputStream(filePath).use { workbook.write(it) }
            workbook.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 向现有Excel文件添加新Sheet
     */
    fun addSheetToExistingFile(filePath: String, sheetName: String): Boolean {
        return try {
            FileInputStream(filePath).use { inputStream ->
                val existingWorkbook = XSSFWorkbook(inputStream)
                val workbook = SXSSFWorkbook(existingWorkbook)

                workbook.createSheet(sheetName)

                FileOutputStream(filePath).use { workbook.write(it) }
                workbook.close()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

package com.fanekit.common.excel.reader

import com.fanekit.common.excel.callback.ReadProgressCallback
import com.fanekit.common.excel.config.ExcelConfig
import com.fanekit.common.excel.detector.FileSizeDetector
import com.fanekit.common.excel.handler.RowHandler
import com.fanekit.common.excel.model.FieldInfo
import com.fanekit.common.excel.model.SheetInfo
import com.fanekit.common.excel.util.FieldParseUtils
import com.fanekit.common.excel.util.SheetUtils
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.util.IOUtils
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable
import org.apache.poi.xssf.eventusermodel.XSSFReader
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler
import org.xml.sax.InputSource
import java.io.*
import javax.xml.parsers.SAXParserFactory

/**
 * Excel读取器 - 专门负责Excel文件的读取功能
 * 支持传统模式和大文件流式读取模式，支持指定Sheet读取
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/11/20
 * @since 1.0.0
 * @version 1.0.0
 */
object ExcelReader {

    /**
     * read模式一次性返回，如果要读取大文件，请使用readBig
     * 传统模式 - 适用于中小数据量
     * @param filePath Excel文件路径
     * @param clazz 数据类型（解析字段上的@Excel注解）
     * @param startLine 默认从2开始，即跳过第0行标题行,跳过第1行字段名称行
     * @param sheetIndex 指定Sheet索引（可选）
     * @param sheetName 指定Sheet名称（可选）
     * @param includeSuperClass 是否包含父类字段，默认false
     * @return 导入的数据列表
     */
    fun <T : Any> read(
        filePath: String,
        clazz: Class<T>,
        startLine: Int = ExcelConfig.Default.START_LINE,
        sheetIndex: Int? = null,
        sheetName: String? = null,
        includeSuperClass: Boolean = false
    ): List<T> {
        val resultList = mutableListOf<T>()

        readBig(
            filePath = filePath,
            clazz = clazz,
            batchSize = ExcelConfig.Batch.TRADITIONAL_BATCH_SIZE,
            callback = object : ReadProgressCallback<T> {
                override fun onBatchRead(
                    batchData: List<T>,
                    processedCount: Long,
                    totalCount: Long,
                    remainingCount: Long,
                    currentBatch: Int,
                    batchSize: Int
                ) {
                    resultList.addAll(batchData)
                }

                override fun onCompleted(totalCount: Long) {
                    // 传统模式不需要特殊处理
                }

                override fun onError(error: Throwable, processedCount: Long) {
                    throw error
                }
            },
            startLine = startLine,
            maxFileSize = ExcelConfig.FileSize.TRADITIONAL_MAX_SIZE_MB * 1024 * 1024,
            sheetIndex = sheetIndex,
            sheetName = sheetName,
            includeSuperClass = includeSuperClass
        )

        return resultList
    }

    /**
     * 大数据量读取模式 - 支持百万级数据量
     * 使用POI流式读取，避免内存溢出
     * 适用于大文件（.xlsx），如百万行数据
     * @param filePath Excel文件路径
     * @param clazz 数据类型（解析字段上的@Excel注解）
     * @param batchSize 每批处理数量，默认1000
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
        batchSize: Int = ExcelConfig.Batch.BIG_MODE_BATCH_SIZE,
        callback: ReadProgressCallback<T>? = null,
        startLine: Int = ExcelConfig.Default.START_LINE,
        maxFileSize: Int = ExcelConfig.FileSize.BIG_MODE_DEFAULT_MAX_SIZE_MB * 1024 * 1024,
        sheetIndex: Int? = null,
        sheetName: String? = null,
        includeSuperClass: Boolean = false
    ) {
        try {
            // 设置POI读取大文件的限制
            IOUtils.setByteArrayMaxOverride(maxFileSize)

            val fieldInfo = FieldParseUtils.parseFieldInfo(clazz,includeSuperClass)

            // 验证文件存在性和大小
            if (!FileSizeDetector.validateFileSize(filePath, maxFileSize / 1024 / 1024)) {
                throw IllegalArgumentException("文件过大，超过${maxFileSize / 1024 / 1024}MB限制")
            }
            readStreaming(filePath, clazz, batchSize, callback, startLine, fieldInfo, sheetIndex, sheetName)

        } catch (e: Exception) {
            callback?.onError(e, 0L)
            throw e
        }
    }

    /**
     * 适用于百万行 .xlsx 的纯 POI 流式读取
     * 仅依赖: poi-ooxml 5.4.1 (poi / scratchpad 可选)
     * @param filePath Excel文件路径
     * @param clazz 数据类型（解析字段上的@Excel注解）
     * @param batchSize 每批处理数量，默认1000
     * @param callback 进度回调
     * @param startLine 开始行号，默认从第2行开始
     * @param fieldInfo 字段信息，包含字段映射、转换规则等
     * @param sheetIndex 指定Sheet索引（可选）
     * @param sheetName 指定Sheet名称（可选）
     * @return 无返回值，数据通过回调返回
     */
    private fun <T : Any> readStreaming(
        filePath: String,
        clazz: Class<T>,
        batchSize: Int,
        callback: ReadProgressCallback<T>?,
        startLine: Int,
        fieldInfo: FieldInfo,
        sheetIndex: Int? = null,
        sheetName: String? = null
    ) {
        require(File(filePath).extension.equals("xlsx", true)) { "只能读取 .xlsx 文件" }

        var processedCount = 0L
        var currentBatch = 1
        val batch = mutableListOf<T>()

        /* ---------- 1. 打开 OPCPackage ---------- */
        OPCPackage.open(File(filePath)).use { pkg ->
            val reader = XSSFReader(pkg)
            val styles      = reader.stylesTable
            val sharedStr   = ReadOnlySharedStringsTable(pkg)
            val formatter   = DataFormatter()
            val sheetIter   = reader.sheetsData as XSSFReader.SheetIterator

            var idx = -1
            var matched = false

            /* ---------- 2. 遍历 Sheet ---------- */
            while (sheetIter.hasNext()) {
                val sheetStream = sheetIter.next()    // InputStream
                idx++

                val thisSheetMatches =
                    (sheetIndex != null && idx == sheetIndex) ||
                            (sheetName  != null && sheetIter.sheetName == sheetName) ||
                            (sheetIndex == null && sheetName == null && idx == 0)

                if (!thisSheetMatches) {
                    sheetStream.closeSafely()
                    continue
                }
                matched = true

                /* ---------- 3. 创建 SAX 解析器 ---------- */
                val saxFactory = SAXParserFactory.newInstance().apply {
                    isNamespaceAware = true        // 与 POI 的 XSSFSheetXMLHandler 兼容
                }
                val parser = saxFactory.newSAXParser().xmlReader.apply {
                    contentHandler = XSSFSheetXMLHandler(
                        styles, null, sharedStr,
                        RowHandler(
                            clazz, fieldInfo, startLine, batchSize,
                            batch,
                            onBatch = { list ->
                                processedCount += list.size
                                callback?.onBatchRead(
                                    batchData      = list,
                                    processedCount = processedCount,
                                    totalCount     = -1,
                                    remainingCount = -1,
                                    currentBatch   = currentBatch++,
                                    batchSize      = list.size
                                )
                            },
                            onError = { err ->
                                callback?.onError(err, processedCount)
                            }
                        ),
                        formatter,
                        /* 公式结果是否需要计算; 关闭可再省一点时间 */
                        false
                    )
                }

                /* ---------- 4. 解析当前 Sheet ---------- */
                parser.parse(InputSource(sheetStream))
                sheetStream.closeSafely()
                break                                           // 只处理一个 Sheet
            }

            if (!matched) {
                throw IllegalArgumentException("未找到符合条件的 Sheet")
            }
        }

        /* ---------- 5. 处理最后不足 batchSize 的行 ---------- */
        if (batch.isNotEmpty()) {
            processedCount += batch.size
            callback?.onBatchRead(
                batchData      = batch.toList(),
                processedCount = processedCount,
                totalCount     = -1,
                remainingCount = -1,
                currentBatch   = currentBatch,
                batchSize      = batch.size
            )
        }
        callback?.onCompleted(processedCount)
    }




    /**
     * 获取所有Sheet信息
     */
    fun getSheetInfo(filePath: String): List<SheetInfo> {
        return SheetUtils.getSheetInfo(filePath)
    }

    /**
     * 获取Sheet总数
     */
    fun getSheetCount(filePath: String): Int {
        return SheetUtils.getSheetCount(filePath)
    }


    /* -------------------- 安全关闭工具 -------------------- */
    private fun Closeable?.closeSafely() = try { this?.close() } catch (_: Exception) {}
}

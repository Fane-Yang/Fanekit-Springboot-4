package com.fanekit.common.util.io

import com.fanekit.common.core.exception.Throw
import com.fanekit.common.core.constants.IOUtilConstants
import com.fanekit.common.core.security.IOSecurityValidator
import java.io.*
import java.nio.charset.Charset

/**
 * CSV工具类
 * 提供CSV文件读取、写入、解析等功能，支持安全检查
 * TODO 支持大数据批次处理，百万行数据处理, 需要继续完善,写入需要修改为使用数据提供者
 * @author FaneKit  // DO NOT REPLACE
 * @date 2025/02/18
 * @since 1.0.0
 * @version 1.0.0
 */
object CsvUtil {

    /**
     * CSV配置类
     */
    data class CsvConfig(
        val separator: Char = IOUtilConstants.CSV_DEFAULT_SEPARATOR,
        val quote: Char = IOUtilConstants.CSV_DEFAULT_QUOTE,
        val escape: Char = IOUtilConstants.CSV_DEFAULT_ESCAPE,
        val charset: Charset = IOUtilConstants.DEFAULT_CHARSET,
        val hasHeader: Boolean = true,
        val skipEmptyLines: Boolean = true,
        val trimValues: Boolean = true,
        val maxLines: Int = IOUtilConstants.MAX_CSV_LINES,
        val maxColumns: Int = IOUtilConstants.MAX_CSV_COLUMNS,
        val maxCellLength: Int = IOUtilConstants.MAX_CSV_CELL_LENGTH,
        // 批次处理配置
        val batchSize: Int = 10000,  // 默认批次大小10000行
        val enableBigDataMode: Boolean = false  // 是否启用大数据模式
    )

    /**
     * 读取CSV文件并返回字符串列表的列表 - 带安全检查
     */
    fun readCsv(filePath: String, config: CsvConfig = CsvConfig()): List<List<String>> {
        return IOSecurityValidator.safeOperation("读取CSV文件", filePath) {
            readCsv(File(filePath), config)
        }
    }

    /**
     * 读取CSV文件并返回字符串列表的列表 - 带安全检查
     */
    fun readCsv(file: File, config: CsvConfig = CsvConfig()): List<List<String>> {
        return IOSecurityValidator.safeOperation("读取CSV文件", file.absolutePath) {
            IOSecurityValidator.validateFileExists(file, "读取CSV文件")

            // 检查文件大小
            val fileSize = file.length()
            IOSecurityValidator.validateFileSize(fileSize, IOUtilConstants.MAX_FILE_SIZE, file.absolutePath)

            file.bufferedReader(config.charset).use { reader ->
                readCsv(reader, config)
            }
        }
    }

    /**
     * 从输入流读取CSV并返回字符串列表的列表
     */
    fun readCsv(inputStream: InputStream, config: CsvConfig = CsvConfig()): List<List<String>> {
        return inputStream.bufferedReader(config.charset).use { reader ->
            readCsv(reader, config)
        }
    }

    /**
     * 从Reader读取CSV并返回字符串列表的列表
     */
    fun readCsv(reader: BufferedReader, config: CsvConfig = CsvConfig()): List<List<String>> {
        // 如果启用大数据模式或文件可能很大，使用批次读取
        if (config.enableBigDataMode) {
            val result = mutableListOf<List<String>>()
            readBigCsv(reader, config) { batch, _, _ ->
                result.addAll(batch)
            }
            return result
        }

        // 原有的直接读取实现（用于小文件）
        val result = mutableListOf<List<String>>()
        var line: String?
        var lineCount = 0

        while (reader.readLine().also { line = it } != null) {
            line?.let { currentLine ->
                lineCount++

                // 检查行数限制
                IOSecurityValidator.validateCsvSecurity(lineCount = lineCount)

                if (config.skipEmptyLines && currentLine.isBlank()) {
                    return@let
                }

                val parsedLine = parseCsvLine(currentLine, config)
                if (parsedLine.isNotEmpty()) {
                    // 检查列数限制
                    IOSecurityValidator.validateCsvSecurity(columnCount = parsedLine.size)

                    // 检查单元格长度
                    parsedLine.forEach { cell ->
                        IOSecurityValidator.validateCsvSecurity(cellLength = cell.length)
                    }

                    result.add(parsedLine)
                }
            }
        }

        return result
    }

    /**
     * 读取CSV文件并返回Map列表（使用第一行作为表头）
     */
    fun readCsvWithHeaders(filePath: String, config: CsvConfig = CsvConfig()): List<Map<String, String>> {
        return IOSecurityValidator.safeOperation("读取CSV文件", filePath) {
            readCsvWithHeaders(File(filePath), config)
        }
    }

    /**
     * 读取CSV文件并返回Map列表（使用第一行作为表头）
     */
    fun readCsvWithHeaders(file: File, config: CsvConfig = CsvConfig()): List<Map<String, String>> {
        return file.bufferedReader(config.charset).use { reader ->
            readCsvWithHeaders(reader, config)
        }
    }

    /**
     * 从Reader读取CSV并返回Map列表（使用第一行作为表头）
     */
    fun readCsvWithHeaders(reader: BufferedReader, config: CsvConfig = CsvConfig()): List<Map<String, String>> {
        val lines = readCsv(reader, config)
        if (lines.isEmpty()) return emptyList()

        val headers = lines.first()
        val dataLines = lines.drop(1)

        return dataLines.map { line ->
            headers.zip(line).toMap()
        }
    }

    /**
     * 写入CSV文件
     */
    fun writeCsv(
        filePath: String,
        data: List<List<String>>,
        config: CsvConfig = CsvConfig(),
        append: Boolean = false
    ) {
        IOSecurityValidator.safeOperation("写入CSV文件", filePath) {
            writeCsv(File(filePath), data, config, append)
        }
    }

    /**
     * 写入CSV文件
     */
    fun writeCsv(
        file: File,
        data: List<List<String>>,
        config: CsvConfig = CsvConfig(),
        append: Boolean = false
    ) {
        IOSecurityValidator.safeOperation("写入CSV文件", file.absolutePath) {
            // 验证数据安全性
            IOSecurityValidator.validateCsvSecurity(lineCount = data.size)
            data.forEach { row ->
                IOSecurityValidator.validateCsvSecurity(columnCount = row.size)
                row.forEach { cell ->
                    IOSecurityValidator.validateCsvSecurity(cellLength = cell.length)
                }
            }

            file.parentFile?.let { parent ->
                if (!parent.exists()) {
                    IOSecurityValidator.validatePathSecurity(parent.absolutePath, "创建父目录")
                    if (!parent.mkdirs()) {
                        Throw.utilOperationFailed("创建父目录失败: ${parent.absolutePath}")
                    }
                }
            }

            file.bufferedWriter(config.charset, IOUtilConstants.DEFAULT_BUFFER_SIZE).use { writer ->
                writeCsv(writer, data, config)
            }
        }
    }

    /**
     * 写入CSV到输出流
     */
    fun writeCsv(
        outputStream: OutputStream,
        data: List<List<String>>,
        config: CsvConfig = CsvConfig()
    ) {
        outputStream.bufferedWriter(config.charset).use { writer ->
            writeCsv(writer, data, config)
        }
    }

    /**
     * 写入CSV到Writer
     */
    fun writeCsv(
        writer: BufferedWriter,
        data: List<List<String>>,
        config: CsvConfig = CsvConfig()
    ) {
        data.forEach { row ->
            val csvLine = formatCsvLine(row, config)
            writer.write(csvLine)
            writer.newLine()
        }
        writer.flush()
    }

    /**
     * 写入CSV文件（使用Map数据和指定的表头）
     */
    fun writeCsvWithHeaders(
        filePath: String,
        data: List<Map<String, String>>,
        headers: List<String>,
        config: CsvConfig = CsvConfig(),
        append: Boolean = false
    ) {
        writeCsvWithHeaders(File(filePath), data, headers, config, append)
    }

    /**
     * 写入CSV文件（使用Map数据和指定的表头）
     */
    fun writeCsvWithHeaders(
        file: File,
        data: List<Map<String, String>>,
        headers: List<String>,
        config: CsvConfig = CsvConfig(),
        append: Boolean = false
    ) {
        val csvData = mutableListOf<List<String>>()

        // 添加表头
        if (config.hasHeader && !append) {
            csvData.add(headers)
        }

        // 添加数据行
        data.forEach { row ->
            val line = headers.map { header -> row[header] ?: "" }
            csvData.add(line)
        }

        writeCsv(file, csvData, config, append)
    }

    /**
     * 解析单行CSV
     */
    fun parseCsvLine(line: String, config: CsvConfig = CsvConfig()): List<String> {
        val result = mutableListOf<String>()
        val chars = line.toCharArray()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < chars.size) {
            val char = chars[i]

            when {
                char == config.quote -> {
                    if (inQuotes) {
                        // 检查是否是转义的引号
                        if (i + 1 < chars.size && chars[i + 1] == config.quote) {
                            currentField.append(config.quote)
                            i++ // 跳过下一个引号
                        } else {
                            inQuotes = false
                        }
                    } else {
                        inQuotes = true
                    }
                }
                char == config.separator && !inQuotes -> {
                    val field = if (config.trimValues) currentField.toString().trim() else currentField.toString()
                    result.add(field)
                    currentField.clear()
                }
                char == config.escape && i + 1 < chars.size -> {
                    val nextChar = chars[i + 1]
                    when (nextChar) {
                        'n' -> currentField.append('\n')
                        'r' -> currentField.append('\r')
                        't' -> currentField.append('\t')
                        '\\' -> currentField.append('\\')
                        else -> {
                            currentField.append(char)
                            currentField.append(nextChar)
                        }
                    }
                    i++ // 跳过下一个字符
                }
                else -> {
                    currentField.append(char)
                }
            }
            i++
        }

        // 添加最后一个字段
        val field = if (config.trimValues) currentField.toString().trim() else currentField.toString()
        result.add(field)

        return result
    }

    /**
     * 格式化单行CSV
     */
    fun formatCsvLine(fields: List<String>, config: CsvConfig = CsvConfig()): String {
        return fields.joinToString(config.separator.toString()) { field ->
            formatCsvField(field, config)
        }
    }

    /**
     * 格式化单个CSV字段
     */
    fun formatCsvField(field: String, config: CsvConfig = CsvConfig()): String {
        val needsQuotes = field.contains(config.separator) ||
                field.contains(config.quote) ||
                field.contains('\n') ||
                field.contains('\r')

        return if (needsQuotes) {
            val escapedField = field.replace(config.quote.toString(), "${config.quote}${config.quote}")
            "${config.quote}$escapedField${config.quote}"
        } else {
            field
        }
    }

    /**
     * 转换List<List<String>>为List<Map<String, String>>
     */
    fun convertToMaps(data: List<List<String>>, headers: List<String>): List<Map<String, String>> {
        return data.map { row ->
            headers.zip(row).toMap()
        }
    }

    /**
     * 转换List<Map<String, String>>为List<List<String>>
     */
    fun convertFromMaps(data: List<Map<String, String>>, headers: List<String>): List<List<String>> {
        return data.map { row ->
            headers.map { header -> row[header] ?: "" }
        }
    }

    /**
     * 过滤CSV数据
     */
    fun filterCsv(
        data: List<List<String>>,
        predicate: (List<String>) -> Boolean
    ): List<List<String>> {
        return data.filter(predicate)
    }

    /**
     * 过滤CSV数据（Map版本）
     */
    fun filterCsvMaps(
        data: List<Map<String, String>>,
        predicate: (Map<String, String>) -> Boolean
    ): List<Map<String, String>> {
        return data.filter(predicate)
    }

    /**
     * 排序CSV数据
     */
    fun sortCsv(
        data: List<List<String>>,
        columnIndex: Int,
        ascending: Boolean = true
    ): List<List<String>> {
        return if (ascending) {
            data.sortedBy { it.getOrNull(columnIndex) ?: "" }
        } else {
            data.sortedByDescending { it.getOrNull(columnIndex) ?: "" }
        }
    }

    /**
     * 排序CSV数据（Map版本）
     */
    fun sortCsvMaps(
        data: List<Map<String, String>>,
        columnName: String,
        ascending: Boolean = true
    ): List<Map<String, String>> {
        return if (ascending) {
            data.sortedBy { it[columnName] ?: "" }
        } else {
            data.sortedByDescending { it[columnName] ?: "" }
        }
    }

    /**
     * 合并多个CSV文件
     */
    fun mergeCsvFiles(
        filePaths: List<String>,
        outputPath: String,
        config: CsvConfig = CsvConfig()
    ) {
        val allData = mutableListOf<List<String>>()
        var isFirstFile = true

        filePaths.forEach { filePath ->
            val data = readCsv(filePath, config)
            if (data.isNotEmpty()) {
                if (isFirstFile || !config.hasHeader) {
                    allData.addAll(data)
                } else {
                    // 跳过非第一个文件的表头
                    allData.addAll(data.drop(1))
                }
                isFirstFile = false
            }
        }

        writeCsv(outputPath, allData, config)
    }

    /**
     * 验证CSV数据
     */
    fun validateCsv(
        data: List<List<String>>,
        expectedColumns: Int? = null,
        requiredHeaders: List<String>? = null
    ): CsvValidationResult {
        val errors = mutableListOf<String>()

        if (data.isEmpty()) {
            errors.add("CSV数据为空")
            return CsvValidationResult(false, errors)
        }

        // 使用安全验证器检查基本限制
        try {
            IOSecurityValidator.validateCsvSecurity(lineCount = data.size)
        } catch (e: Exception) {
            errors.add("CSV行数超过限制: ${data.size}")
        }

        val headers = data.first()
        val dataRows = data.drop(1)

        // 检查列数
        expectedColumns?.let { expected ->
            if (headers.size != expected) {
                errors.add("表头列数不匹配，期望：$expected，实际：${headers.size}")
            }

            dataRows.forEachIndexed { index, row ->
                if (row.size != expected) {
                    errors.add("第${index + 2}行列数不匹配，期望：$expected，实际：${row.size}")
                }
            }
        }

        // 检查必需的表头
        requiredHeaders?.let { required ->
            val missingHeaders = required.filter { it !in headers }
            if (missingHeaders.isNotEmpty()) {
                errors.add("缺少必需的表头：${missingHeaders.joinToString(", ")}")
            }
        }

        return CsvValidationResult(errors.isEmpty(), errors)
    }

    /**
     * CSV验证结果
     */
    data class CsvValidationResult(
        val isValid: Boolean,
        val errors: List<String>
    )

    /**
     * 统计CSV信息
     */
    fun getCsvInfo(filePath: String, config: CsvConfig = CsvConfig()): CsvInfo {
        return getCsvInfo(File(filePath), config)
    }

    /**
     * 统计CSV信息
     */
    fun getCsvInfo(file: File, config: CsvConfig = CsvConfig()): CsvInfo {
        val data = readCsv(file, config)
        return getCsvInfo(data, config)
    }

    /**
     * 统计CSV信息
     */
    fun getCsvInfo(data: List<List<String>>, config: CsvConfig = CsvConfig()): CsvInfo {
        if (data.isEmpty()) {
            return CsvInfo(0, 0, emptyList(), 0L)
        }

        val headers = if (config.hasHeader) data.first() else emptyList()
        val dataRows = if (config.hasHeader) data.drop(1) else data
        val columnCount = data.maxOfOrNull { it.size } ?: 0
        val rowCount = dataRows.size
        val totalCells = dataRows.sumOf { it.size }.toLong()

        return CsvInfo(rowCount, columnCount, headers, totalCells)
    }

    /**
     * CSV信息
     */
    data class CsvInfo(
        val rowCount: Int,
        val columnCount: Int,
        val headers: List<String>,
        val totalCells: Long
    )

    /**
     * 转换CSV编码
     */
    fun convertCsvEncoding(
        inputPath: String,
        outputPath: String,
        fromCharset: Charset,
        toCharset: Charset,
        config: CsvConfig = CsvConfig()
    ) {
        IOSecurityValidator.safeOperation("转换CSV编码", inputPath) {
            val inputConfig = config.copy(charset = fromCharset)
            val outputConfig = config.copy(charset = toCharset)

            val data = readCsv(inputPath, inputConfig)
            writeCsv(outputPath, data, outputConfig)
        }
    }

    /**
     * 分割大CSV文件
     */
    fun splitCsvFile(
        inputPath: String,
        outputDir: String,
        linesPerFile: Int,
        config: CsvConfig = CsvConfig()
    ): List<String> {
        return IOSecurityValidator.safeOperation("分割CSV文件", inputPath) {
            val data = readCsv(inputPath, config)
            val outputFiles = mutableListOf<String>()

            if (data.isEmpty()) return@safeOperation outputFiles

            val headers = if (config.hasHeader) data.first() else emptyList()
            val dataRows = if (config.hasHeader) data.drop(1) else data

            val chunks = dataRows.chunked(linesPerFile)
            val inputFile = File(inputPath)
            val baseName = inputFile.nameWithoutExtension
            val extension = inputFile.extension

            chunks.forEachIndexed { index, chunk ->
                val outputFileName = "${baseName}_part${index + 1}.$extension"
                val outputPath = File(outputDir, outputFileName).absolutePath

                val outputData = if (config.hasHeader) {
                    listOf(headers) + chunk
                } else {
                    chunk
                }

                writeCsv(outputPath, outputData, config)
                outputFiles.add(outputPath)
            }

            outputFiles
        }
    }

    // ===================== 大数据批次处理功能 =====================

    /**
     * 批次读取大CSV文件，支持百万行数据处理
     * 使用回调函数处理每批数据，避免内存溢出
     */
    fun readBigCsv(
        filePath: String,
        config: CsvConfig = CsvConfig(),
        batchProcessor: (batch: List<List<String>>, batchIndex: Int, isLast: Boolean) -> Unit
    ) {
        readBigCsv(File(filePath), config, batchProcessor)
    }

    /**
     * 批次读取大CSV文件，支持百万行数据处理
     */
    fun readBigCsv(
        file: File,
        config: CsvConfig = CsvConfig(),
        batchProcessor: (batch: List<List<String>>, batchIndex: Int, isLast: Boolean) -> Unit
    ) {
        IOSecurityValidator.safeOperation("批次读取大CSV文件", file.absolutePath) {
            IOSecurityValidator.validateFileExists(file, "批次读取大CSV文件")

            file.bufferedReader(config.charset, IOUtilConstants.DEFAULT_BUFFER_SIZE * 4).use { reader ->
                readBigCsv(reader, config, batchProcessor)
            }
        }
    }

    /**
     * 从Reader批次读取大CSV文件
     */
    fun readBigCsv(
        reader: BufferedReader,
        config: CsvConfig = CsvConfig(),
        batchProcessor: (batch: List<List<String>>, batchIndex: Int, isLast: Boolean) -> Unit
    ) {
        var line: String?
        var lineCount = 0
        var batchIndex = 0
        val currentBatch = mutableListOf<List<String>>()
        val batchSize = if (config.enableBigDataMode) config.batchSize else config.batchSize.coerceAtMost(10000)

        while (reader.readLine().also { line = it } != null) {
            line?.let { currentLine ->
                lineCount++

                if (config.skipEmptyLines && currentLine.isBlank()) {
                    return@let
                }

                val parsedLine = parseCsvLine(currentLine, config)
                if (parsedLine.isNotEmpty()) {
                    currentBatch.add(parsedLine)

                    // 当批次满时处理
                    if (currentBatch.size >= batchSize) {
                        batchProcessor(currentBatch.toList(), batchIndex, false)
                        currentBatch.clear()
                        batchIndex++

                        // GC建议
                        if (batchIndex % 10 == 0) {
                            System.gc()
                        }
                    }
                }
            }
        }

        // 处理最后一批数据
        if (currentBatch.isNotEmpty()) {
            batchProcessor(currentBatch.toList(), batchIndex, true)
        }
    }

    /**
     * 批次读取大CSV文件并返回带表头的Map数据
     */
    fun readBigCsvWithHeaders(
        filePath: String,
        config: CsvConfig = CsvConfig(),
        batchProcessor: (batch: List<Map<String, String>>, headers: List<String>, batchIndex: Int, isLast: Boolean) -> Unit
    ) {
        readBigCsvWithHeaders(File(filePath), config, batchProcessor)
    }

    /**
     * 批次读取大CSV文件并返回带表头的Map数据
     */
    fun readBigCsvWithHeaders(
        file: File,
        config: CsvConfig = CsvConfig(),
        batchProcessor: (batch: List<Map<String, String>>, headers: List<String>, batchIndex: Int, isLast: Boolean) -> Unit
    ) {
        file.bufferedReader(config.charset, IOUtilConstants.DEFAULT_BUFFER_SIZE * 4).use { reader ->
            readBigCsvWithHeaders(reader, config, batchProcessor)
        }
    }

    /**
     * 从Reader批次读取大CSV文件并返回带表头的Map数据
     */
    fun readBigCsvWithHeaders(
        reader: BufferedReader,
        config: CsvConfig = CsvConfig(),
        batchProcessor: (batch: List<Map<String, String>>, headers: List<String>, batchIndex: Int, isLast: Boolean) -> Unit
    ) {
        var headers: List<String>? = null
        var isFirstBatch = true

        readBigCsv(reader, config) { batch, batchIndex, isLast ->
            val actualBatch = if (isFirstBatch && config.hasHeader) {
                headers = batch.firstOrNull() ?: emptyList()
                batch.drop(1)
            } else {
                batch
            }

            if (actualBatch.isNotEmpty() && headers != null) {
                val mapBatch = actualBatch.map { line ->
                    headers!!.zip(line).toMap()
                }
                batchProcessor(mapBatch, headers!!, batchIndex, isLast)
            }

            isFirstBatch = false
        }
    }

    /**
     * 批次写入大CSV文件，支持百万行数据处理
     */
    fun writeBigCsv(
        filePath: String,
        config: CsvConfig = CsvConfig(),
        append: Boolean = false,
        dataProvider: (batchIndex: Int) -> List<List<String>>?
    ) {
        writeBigCsv(File(filePath), config, append, dataProvider)
    }

    /**
     * 批次写入大CSV文件
     */
    fun writeBigCsv(
        file: File,
        config: CsvConfig = CsvConfig(),
        append: Boolean = false,
        dataProvider: (batchIndex: Int) -> List<List<String>>?
    ) {
        IOSecurityValidator.safeOperation("批次写入大CSV文件", file.absolutePath) {
            file.parentFile?.let { parent ->
                if (!parent.exists()) {
                    IOSecurityValidator.validatePathSecurity(parent.absolutePath, "创建父目录")
                    if (!parent.mkdirs()) {
                        Throw.utilOperationFailed("创建父目录失败: ${parent.absolutePath}")
                    }
                }
            }

            file.bufferedWriter(config.charset, IOUtilConstants.DEFAULT_BUFFER_SIZE * 4).use { writer ->
                writeBigCsv(writer, config, dataProvider)
            }
        }
    }

    /**
     * 向Writer批次写入大CSV文件
     */
    fun writeBigCsv(
        writer: BufferedWriter,
        config: CsvConfig = CsvConfig(),
        dataProvider: (batchIndex: Int) -> List<List<String>>?
    ) {
        var batchIndex = 0

        while (true) {
            val batch = dataProvider(batchIndex)
            if (batch == null || batch.isEmpty()) {
                break
            }

            batch.forEach { row ->
                val csvLine = formatCsvLine(row, config)
                writer.write(csvLine)
                writer.newLine()
            }

            writer.flush()
            batchIndex++

            // GC建议
            if (batchIndex % 10 == 0) {
                System.gc()
            }
        }
    }

    /**
     * 批次写入大CSV文件（Map版本）
     */
    fun writeBigCsvWithHeaders(
        filePath: String,
        headers: List<String>,
        config: CsvConfig = CsvConfig(),
        append: Boolean = false,
        dataProvider: (batchIndex: Int) -> List<Map<String, String>>?
    ) {
        writeBigCsvWithHeaders(File(filePath), headers, config, append, dataProvider)
    }

    /**
     * 批次写入大CSV文件（Map版本）
     */
    fun writeBigCsvWithHeaders(
        file: File,
        headers: List<String>,
        config: CsvConfig = CsvConfig(),
        append: Boolean = false,
        dataProvider: (batchIndex: Int) -> List<Map<String, String>>?
    ) {
        var isFirstBatch = true

        writeBigCsv(file, config, append) { batchIndex ->
            val mapBatch = dataProvider(batchIndex)
            if (mapBatch == null) return@writeBigCsv null

            val result = mutableListOf<List<String>>()

            // 添加表头（仅第一批且不是追加模式）
            if (isFirstBatch && config.hasHeader && !append) {
                result.add(headers)
            }

            // 转换Map数据为List数据
            mapBatch.forEach { row ->
                val line = headers.map { header -> row[header] ?: "" }
                result.add(line)
            }

            isFirstBatch = false
            result
        }
    }

    /**
     * 流式处理大CSV文件 - 逐行处理，内存占用最小
     */
    fun streamBigCsv(
        filePath: String,
        config: CsvConfig = CsvConfig(),
        lineProcessor: (line: List<String>, lineIndex: Int) -> Unit
    ) {
        streamBigCsv(File(filePath), config, lineProcessor)
    }

    /**
     * 流式处理大CSV文件
     */
    fun streamBigCsv(
        file: File,
        config: CsvConfig = CsvConfig(),
        lineProcessor: (line: List<String>, lineIndex: Int) -> Unit
    ) {
        IOSecurityValidator.safeOperation("流式处理大CSV文件", file.absolutePath) {
            IOSecurityValidator.validateFileExists(file, "流式处理大CSV文件")

            file.bufferedReader(config.charset, IOUtilConstants.DEFAULT_BUFFER_SIZE * 4).use { reader ->
                streamBigCsv(reader, config, lineProcessor)
            }
        }
    }

    /**
     * 从Reader流式处理大CSV文件
     */
    fun streamBigCsv(
        reader: BufferedReader,
        config: CsvConfig = CsvConfig(),
        lineProcessor: (line: List<String>, lineIndex: Int) -> Unit
    ) {
        var line: String?
        var lineIndex = 0

        while (reader.readLine().also { line = it } != null) {
            line?.let { currentLine ->
                if (config.skipEmptyLines && currentLine.isBlank()) {
                    return@let
                }

                val parsedLine = parseCsvLine(currentLine, config)
                if (parsedLine.isNotEmpty()) {
                    lineProcessor(parsedLine, lineIndex)
                    lineIndex++

                    // 定期GC建议
                    if (lineIndex % 50000 == 0) {
                        System.gc()
                    }
                }
            }
        }
    }

    /**
     * 流式处理大CSV文件（Map版本）
     */
    fun streamBigCsvWithHeaders(
        filePath: String,
        config: CsvConfig = CsvConfig(),
        lineProcessor: (line: Map<String, String>, headers: List<String>, lineIndex: Int) -> Unit
    ) {
        streamBigCsvWithHeaders(File(filePath), config, lineProcessor)
    }

    /**
     * 流式处理大CSV文件（Map版本）
     */
    fun streamBigCsvWithHeaders(
        file: File,
        config: CsvConfig = CsvConfig(),
        lineProcessor: (line: Map<String, String>, headers: List<String>, lineIndex: Int) -> Unit
    ) {
        file.bufferedReader(config.charset, IOUtilConstants.DEFAULT_BUFFER_SIZE * 4).use { reader ->
            streamBigCsvWithHeaders(reader, config, lineProcessor)
        }
    }

    /**
     * 从Reader流式处理大CSV文件（Map版本）
     */
    fun streamBigCsvWithHeaders(
        reader: BufferedReader,
        config: CsvConfig = CsvConfig(),
        lineProcessor: (line: Map<String, String>, headers: List<String>, lineIndex: Int) -> Unit
    ) {
        var headers: List<String>? = null
        var isFirstLine = true

        streamBigCsv(reader, config) { line, lineIndex ->
            if (isFirstLine && config.hasHeader) {
                headers = line
                isFirstLine = false
                return@streamBigCsv
            }

            if (headers != null) {
                val mapLine = headers!!.zip(line).toMap()
                lineProcessor(mapLine, headers!!, if (config.hasHeader) lineIndex - 1 else lineIndex)
            }

            isFirstLine = false
        }
    }

    /**
     * 转换大CSV文件格式，支持百万行数据
     */
    fun convertBigCsv(
        inputPath: String,
        outputPath: String,
        inputConfig: CsvConfig = CsvConfig(),
        outputConfig: CsvConfig = CsvConfig(),
        transformer: ((List<String>) -> List<String>)? = null
    ) {
        var batchIndex = 0

        writeBigCsv(outputPath, outputConfig) { _ ->
            val batch = mutableListOf<List<String>>()
            var hasData = false

            // 读取一批数据
            readBigCsv(inputPath, inputConfig.copy(batchSize = inputConfig.batchSize)) { readBatch, readBatchIndex, isLast ->
                if (readBatchIndex == batchIndex) {
                    batch.addAll(readBatch)
                    hasData = true
                }
            }

            if (!hasData) return@writeBigCsv null

            // 应用转换器
            val transformedBatch = if (transformer != null) {
                batch.map(transformer)
            } else {
                batch
            }

            batchIndex++
            transformedBatch
        }
    }

    /**
     * 合并多个大CSV文件，支持百万行数据
     */
    fun mergeBigCsvFiles(
        inputPaths: List<String>,
        outputPath: String,
        config: CsvConfig = CsvConfig()
    ) {
        var isFirstFile = true
        var outputBatchIndex = 0

        writeBigCsv(outputPath, config) { _ ->
            if (outputBatchIndex >= inputPaths.size) return@writeBigCsv null

            val inputPath = inputPaths[outputBatchIndex]
            val batch = mutableListOf<List<String>>()

            readBigCsv(inputPath, config) { readBatch, readBatchIndex, isLast ->
                if (readBatchIndex == 0) {
                    val actualBatch = if (!isFirstFile && config.hasHeader) {
                        readBatch.drop(1) // 跳过非第一个文件的表头
                    } else {
                        readBatch
                    }
                    batch.addAll(actualBatch)
                }
            }

            isFirstFile = false
            outputBatchIndex++

            if (batch.isEmpty()) null else batch
        }
    }
}

fun main(){
    CsvUtil.readBigCsv(
        ""
    ) { batch, batchIndex, isLast ->
        // 处理每批数据
        println("Batch $batchIndex: ${batch.size} lines, isLast: $isLast")
    }
}
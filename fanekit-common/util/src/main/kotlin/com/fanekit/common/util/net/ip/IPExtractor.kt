//package common.utils.net.ip
//
//import common.utils.net.ip.IPUtils
//import kotlinx.coroutines.runBlocking
//import java.nio.file.*
//import kotlin.io.path.*
//import java.util.regex.Pattern
//
//object IPExtractor {
//
//    /**
//     * 提取 IP 的核心逻辑
//     *
//     * @param input 输入源（可以是：文件路径/文件夹路径/单行字符串）
//     * @param prefix 提取 IP 的前缀，比如 `->IP:`，可为空（默认 `->IP:`）
//     * @param postfix 提取 IP 的后缀，比如 `;`，可为空（默认 `;`）
//     * @param suffix 仅处理指定后缀文件，如 `.log`，如果为空则处理所有文件
//     * @param output 输出路径（文件或文件夹），可为空。如果为空则只通过 block 或返回值获取。
//     * @param outputFormat 输出格式，支持 `txt` 和 `csv`
//     * @param distinct 是否去重，默认 false。如开启，则会先写入中间文件，结束后去重再写出。
//     * @param block 可选回调，每当找到一个 IP 时会调用该回调
//     * @return 如果是单行文本，则返回当前行匹配到的所有 IP；否则返回空列表。
//     */
//    suspend fun extractIP(
//        input: String,
//        prefix: String = "->IP:",
//        postfix: String = ";",
//        suffix: String? = null,
//        output: String? = null,
//        outputFormat: String = "txt",
//        distinct: Boolean = false,
//        block: ((String) -> Unit)? = null
//    ): List<String> {
//
//        // 构建 IP 的正则 Pattern
//        val ipRegex = buildRegex(prefix, postfix)
//
//        // 如果 output 不为空，先确定目标输出文件
//        val outputPath = output?.let { Path(it) }
//        val finalOutputFile: Path? = outputPath?.let { determineOutputFile(it, outputFormat) }
//
//        // 如果开启去重，就额外准备一个中间文件
//        val tempOutputFile: Path? = if (distinct && finalOutputFile != null) {
//            finalOutputFile.resolveSibling("${finalOutputFile.nameWithoutExtension}.tmp")
//        } else null
//
//        // === 情况 1：input 是文件或文件夹 ===
//        try {
//            val inputPath = Path(input)
//            if (inputPath.exists()) {
//                if (inputPath.isRegularFile()) {
//                    // 单个文件处理
//                    processSingleFile(
//                        file = inputPath,
//                        ipRegex = ipRegex,
//                        distinct = distinct,
//                        tempFile = tempOutputFile ?: finalOutputFile,
//                        outputFormat = outputFormat,
//                        block = block
//                    )
//                } else if (inputPath.isDirectory()) {
//                    // 文件夹，递归处理
//                    Files.walk(inputPath).filter { file ->
//                        file.isRegularFile() && (suffix == null || file.extension == suffix.trimStart('.'))
//                    }.forEach { file ->
//                        processSingleFile(
//                            file = file,
//                            ipRegex = ipRegex,
//                            distinct = distinct,
//                            tempFile = tempOutputFile ?: finalOutputFile,
//                            outputFormat = outputFormat,
//                            block = block
//                        )
//                    }
//                }
//
//                // 如果开启了去重，需要把临时文件去重后写到最终文件
//                if (distinct && tempOutputFile != null && finalOutputFile != null) {
//                    deduplicateAndWrite(tempOutputFile, finalOutputFile, outputFormat)
//                }
//
//                // 对文件/文件夹输入不返回结果
//                return emptyList()
//            }
//        } catch (e: Exception) {
//            println(e.message)
//        }
//
//
//        // === 情况 2：input 是单行字符串 ===
//        val matcher = ipRegex.matcher(input)
//        val resultIPs = mutableListOf<String>()
//        while (matcher.find()) {
//            val ip = matcher.group(1)
//            resultIPs.add(ip)
//            // 回调
//            block?.invoke(ip)
//        }
//
//        // 如果用户指定了输出文件，也写入
//        if (finalOutputFile != null && resultIPs.isNotEmpty()) {
//            if (!distinct) {
//                // 不去重，直接写
//                appendIPs(finalOutputFile, resultIPs, outputFormat)
//            } else {
//                // 去重：先写到临时文件，再去重写出
//                tempOutputFile?.let {
//                    appendIPs(it, resultIPs, outputFormat)
//                    deduplicateAndWrite(it, finalOutputFile, outputFormat)
//                }
//            }
//        }
//
//        // 单行文本场景，返回提取到的所有 IP
//        return resultIPs
//    }
//
//    /**
//     * 处理单个文件：逐行匹配并将匹配到的 IP 追加写入（要么写到 tempFile，要么写到 finalOutputFile）。
//     * 这里把“打开输出流 -> 匹配 -> 写入 -> 关闭输出流”放在一次调用中，避免反复打开文件。
//     */
//    private fun processSingleFile(
//        file: Path,
//        ipRegex: Pattern,
//        distinct: Boolean,
//        tempFile: Path?,
//        outputFormat: String,
//        block: ((String) -> Unit)?
//    ) {
//        // 如果没有输出文件（tempFile == null），则只用回调
//        if (tempFile == null) {
//            file.bufferedReader().useLines { lines ->
//                lines.forEach { line ->
//                    val matcher = ipRegex.matcher(line)
//                    while (matcher.find()) {
//                        block?.invoke(matcher.group(1))
//                    }
//                }
//            }
//            return
//        }
//
//        // 确保目标文件存在
//        createIfNotExists(tempFile)
//
//        // 如果是 csv 并且还是空文件，需要先写表头
//        val needCsvHeader = (outputFormat == "csv" && tempFile.fileSize() == 0L)
//
//        // 打开一次 Writer，遍历整个文件，逐行匹配后写入
//        tempFile.bufferedWriter(charset = Charsets.UTF_8).use { writer ->
//            // 如果需要 CSV 头
//            if (needCsvHeader) {
//                writer.append("IP Address")
//                writer.appendLine()
//            }
//
//            file.bufferedReader().useLines { lines ->
//                lines.forEach { line ->
//                    val matcher = ipRegex.matcher(line)
//                    while (matcher.find()) {
//                        val ip = matcher.group(1)
//                        // 回调
//                        block?.invoke(ip)
//
//                        // 写入一行
//                        writer.append(ip)
//                        writer.appendLine()
//                    }
//                }
//            }
//        }
//    }
//
//    /**
//     * 构建正则表达式，用于匹配指定前后缀的 IP 地址
//     */
//    private fun buildRegex(prefix: String, postfix: String): Pattern {
//        val prefixPattern = Pattern.quote(prefix)
//        val postfixPattern = Pattern.quote(postfix)
//        // group(1) 是整个 IP 字符串，用于后续提取
//        return Pattern.compile(
//            "$prefixPattern((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)){3})$postfixPattern"
//        )
//    }
//
//    /**
//     * 如果是目录，就用 [目录/result.xxx]；否则直接用该文件。
//     */
//    private fun determineOutputFile(outputPath: Path, format: String): Path {
//        return when {
//            outputPath.isDirectory() -> {
//                val targetFile = outputPath.resolve("result.$format")
//                createIfNotExists(targetFile)
//                targetFile
//            }
//
//            outputPath.isRegularFile() -> {
//                createIfNotExists(outputPath)
//                outputPath
//            }
//
//            else -> {
//                createDirIfNotExists(outputPath.parent)
//                createIfNotExists(outputPath)
//                outputPath
//            }
//        }
//    }
//
//    /**
//     * 写一批 IP 到文件（追加方式）。对于 csv 首次写入时添加表头。
//     */
//    private fun appendIPs(file: Path, ips: List<String>, format: String) {
//        if (ips.isEmpty()) return
//        createIfNotExists(file)
//
//        val needCsvHeader = (format == "csv" && file.fileSize() == 0L)
//
//        file.bufferedWriter(Charsets.UTF_8).use { writer ->
//            if (needCsvHeader) {
//                writer.append("IP Address")
//                writer.appendLine()
//            }
//            ips.forEach { ip ->
//                writer.append(ip)
//                writer.appendLine()
//            }
//        }
//    }
//
//    /**
//     * 读取临时文件全部 IP，去重后写到目标文件（覆盖写），最后删除临时文件。
//     */
//    private fun deduplicateAndWrite(tempFile: Path, finalFile: Path, format: String) {
//        createIfNotExists(finalFile)
//
//        val lines = tempFile.readLines()
//        val set = LinkedHashSet<String>()  // 保持插入顺序，如果你想不在意顺序可用普通HashSet
//        for (line in lines) {
//            // 跳过空行和表头
//            if (line.isBlank()) continue
//            if (line.equals("IP Address", ignoreCase = true)) continue
//            set.add(line)
//        }
//
//        finalFile.bufferedWriter(Charsets.UTF_8).use { writer ->
//            if (format == "csv") {
//                writer.write("IP Address")
//                writer.newLine()
//            }
//            for (ip in set) {
//                writer.write(ip)
//                writer.newLine()
//            }
//        }
//
//        tempFile.deleteIfExists()
//    }
//
//    /**
//     * 如果文件不存在则创建（包括父目录）。
//     */
//    private fun createIfNotExists(path: Path) {
//        createDirIfNotExists(path.parent)
//        if (!path.exists()) {
//            Files.createFile(path)
//        }
//    }
//
//    /**
//     * 创建目录（如果目录不存在）。
//     */
//    private fun createDirIfNotExists(dir: Path?) {
//        if (dir != null && !dir.exists()) {
//            Files.createDirectories(dir)
//        }
//    }
//}
//
//
//
//
//object IPExtractorTest {
//    @JvmStatic
//    fun main(args: Array<String>) = runBlocking {
//        // 测试 1：单行字符串
//        val singleLineInput = "Hello ->IP:192.168.0.1; and ->IP:10.10.10.10; End."
//        val ips = IPExtractor.extractIP(
//            input = singleLineInput,
//            prefix = "",
//            postfix = "",
//            suffix = null,
//            output = null,          // 不输出到文件
//            outputFormat = "txt",
//            distinct = false
//        ) { ip ->
//            println("回调获得单行 IP: $ip")
//        }
//        println("函数返回的 IP 列表: $ips")
//        println("---------------------------------------------------")
//
//        // 测试 2：处理单个文件 (假设有个 test.log)
//        val filePath = "test.log"
//        Files.writeString(Paths.get(filePath), """
//            This is a test line ->IP:1.1.1.1; second
//            Another line ->IP:2.2.2.2; end
//            Some line ->IP:1.1.1.1; repeated
//        """.trimIndent())
//        val dbPath = "E:\\Projects\\BackEnd\\ktor\\ktor\\server\\src\\main\\resources\\GeoLite2-Country.mmdb" // 下载并指定数据库路径
//        var int = 0
//        IPExtractor.extractIP(
//            input = "C:\\Program Files\\feiq\\Recv Files\\frame\\output.txt",
//            prefix = "",
//            postfix = "",
//            suffix = "log",
//            output = "C:\\Program Files\\feiq\\Recv Files\\frame\\output-distinct.txt",  // 这里是目录，会自动创建 outputFolder/result.txt
//            distinct = true
//        ) { ip ->
////            println("回调获得文件中的 IP: $ip")
//            try {
//                val geoInfo = IPUtils.getCountry(ip)
//                geoInfo?.country?.let {
//                    val name = it.name
//                    println(it.name)
//                    if (name == "Macao") {
//                        int++;
//                        println("数量$int")
//                    }
//
//                }
//                println(geoInfo?.country?.name)
//            } catch (e: Exception) {
//                println(e.message)
//            }
//
//        }
//
//        println("请查看 outputFolder/result.txt 文件内容 (已去重)。")
//        println("---------------------------------------------------")
//
//        // 你也可以再测处理文件夹、CSV 等情况
//    }
//}
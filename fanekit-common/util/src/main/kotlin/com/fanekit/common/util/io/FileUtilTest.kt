//package xyz.ktorin.common.util.io
//
//import java.io.File
//import java.util.*
//
///**
// * FileUtil测试类
// */
//fun main() {
//    println("=== FileUtil 测试开始 ===")
//
//    // 创建测试目录
//    val testDir = File("test_files")
//    if (!testDir.exists()) {
//        testDir.mkdirs()
//    }
//
//    // 测试1: 文件基本操作
//    println("\n1. 测试文件基本操作")
//    testBasicFileOperations(testDir)
//
//    // 测试2: 文件读写操作
//    println("\n2. 测试文件读写操作")
//    testFileReadWrite(testDir)
//
//    // 测试3: 文件复制移动操作
//    println("\n3. 测试文件复制移动操作")
//    testFileCopyMove(testDir)
//
//    // 测试4: 目录操作
//    println("\n4. 测试目录操作")
//    testDirectoryOperations(testDir)
//
//    // 测试5: 文件信息获取
//    println("\n5. 测试文件信息获取")
//    testFileInfo(testDir)
//
//    // 测试6: 文件查找和过滤
//    println("\n6. 测试文件查找和过滤")
//    testFileSearch(testDir)
//
//    // 测试7: 文件哈希计算
//    println("\n7. 测试文件哈希计算")
//    testFileHash(testDir)
//
//    // 测试8: ZIP压缩解压
//    println("\n8. 测试ZIP压缩解压")
//    testZipOperations(testDir)
//
//    // 测试9: 文件大小格式化
//    println("\n9. 测试文件大小格式化")
//    testFileSizeFormatting()
//
//    // 清理测试文件
//    println("\n10. 清理测试文件")
//    cleanupTestFiles(testDir)
//
//    println("\n=== FileUtil 测试完成 ===")
//}
//
///**
// * 测试文件基本操作
// */
//private fun testBasicFileOperations(testDir: File) {
//    val testFile = File(testDir, "test.txt")
//
//    try {
//        // 创建文件
//        val created = FileUtil.createFile(testFile.absolutePath)
//        println("✓ 创建文件: ${if (created) "成功" else "失败"}")
//
//        // 检查文件存在
//        val exists = FileUtil.exists(testFile.absolutePath)
//        println("✓ 文件存在检查: $exists")
//
//        // 检查是否为文件
//        val isFile = FileUtil.isFile(testFile.absolutePath)
//        println("✓ 是否为文件: $isFile")
//
//        // 检查是否为目录
//        val isDir = FileUtil.isDirectory(testFile.absolutePath)
//        println("✓ 是否为目录: $isDir")
//
//    } catch (e: Exception) {
//        println("✗ 文件基本操作失败: ${e.message}")
//    }
//}
//
///**
// * 测试文件读写操作
// */
//private fun testFileReadWrite(testDir: File) {
//    val testFile = File(testDir, "readwrite_test.txt")
//
//    try {
//        // 写入文本
//        val content = "Hello, FileUtil!\n这是测试内容。\n第三行文本。"
//        FileUtil.writeText(testFile.absolutePath, content)
//        println("✓ 写入文本成功")
//
//        // 读取文本
//        val readContent = FileUtil.readText(testFile.absolutePath)
//        println("✓ 读取文本成功，长度: ${readContent.length}")
//
//        // 读取行
//        val lines = FileUtil.readLines(testFile.absolutePath)
//        println("✓ 读取行数: ${lines.size}")
//        lines.forEachIndexed { index, line ->
//            println("  第${index + 1}行: $line")
//        }
//
//        // 追加文本
//        FileUtil.writeText(testFile.absolutePath, "\n追加的内容", append = true)
//        println("✓ 追加文本成功")
//
//        // 再次读取验证
//        val finalLines = FileUtil.readLines(testFile.absolutePath)
//        println("✓ 追加后行数: ${finalLines.size}")
//
//    } catch (e: Exception) {
//        println("✗ 文件读写操作失败: ${e.message}")
//    }
//}
//
///**
// * 测试文件复制移动操作
// */
//private fun testFileCopyMove(testDir: File) {
//    val sourceFile = File(testDir, "readwrite_test.txt")
//    val copyFile = File(testDir, "copy_test.txt")
//    val moveFile = File(testDir, "moved_test.txt")
//
//    try {
//        // 复制文件
//        val copied = FileUtil.copyFile(sourceFile.absolutePath, copyFile.absolutePath)
//        println("✓ 复制文件: ${if (copied) "成功" else "失败"}")
//
//        if (copied) {
//            // 验证复制的文件内容
//            val originalContent = FileUtil.readText(sourceFile.absolutePath)
//            val copiedContent = FileUtil.readText(copyFile.absolutePath)
//            val contentMatch = originalContent == copiedContent
//            println("✓ 复制内容验证: ${if (contentMatch) "一致" else "不一致"}")
//        }
//
//        // 移动文件
//        val moved = FileUtil.move(copyFile.absolutePath, moveFile.absolutePath)
//        println("✓ 移动文件: ${if (moved) "成功" else "失败"}")
//
//        if (moved) {
//            println("✓ 原文件是否存在: ${FileUtil.exists(copyFile.absolutePath)}")
//            println("✓ 新文件是否存在: ${FileUtil.exists(moveFile.absolutePath)}")
//        }
//
//    } catch (e: Exception) {
//        println("✗ 文件复制移动操作失败: ${e.message}")
//    }
//}
//
///**
// * 测试目录操作
// */
//private fun testDirectoryOperations(testDir: File) {
//    val subDir = File(testDir, "subdir")
//    val subDir2 = File(testDir, "subdir2")
//
//    try {
//        // 创建目录
//        val dirCreated = FileUtil.createDirectories(subDir.absolutePath)
//        println("✓ 创建目录: ${if (dirCreated) "成功" else "失败"}")
//
//        // 在子目录中创建文件
//        repeat(3) { i ->
//            val file = File(subDir, "file$i.txt")
//            FileUtil.writeText(file.absolutePath, "这是文件${i}的内容")
//        }
//        println("✓ 在子目录中创建了3个文件")
//
//        // 复制目录
//        val dirCopied = FileUtil.copyDirectory(subDir.absolutePath, subDir2.absolutePath)
//        println("✓ 复制目录: ${if (dirCopied) "成功" else "失败"}")
//
//        // 获取目录大小
//        val dirSize = FileUtil.getDirectorySize(subDir.absolutePath)
//        println("✓ 目录大小: ${FileUtil.formatFileSize(dirSize)}")
//
//        // 列出目录文件
//        val files = FileUtil.listFiles(subDir.absolutePath, recursive = true)
//        println("✓ 目录文件数量: ${files.size}")
//        files.forEach { file ->
//            println("  ${if (file.isDirectory) "[DIR]" else "[FILE]"} ${file.name}")
//        }
//
//    } catch (e: Exception) {
//        println("✗ 目录操作失败: ${e.message}")
//    }
//}
//
///**
// * 测试文件信息获取
// */
//private fun testFileInfo(testDir: File) {
//    val testFile = File(testDir, "readwrite_test.txt")
//
//    try {
//        val fileInfo = FileUtil.getFileInfo(testFile.absolutePath)
//        println("✓ 文件信息:")
//        println("  名称: ${fileInfo.name}")
//        println("  大小: ${FileUtil.formatFileSize(fileInfo.size)}")
//        println("  扩展名: ${fileInfo.extension}")
//        println("  MIME类型: ${fileInfo.mimeType}")
//        println("  是否为文件: ${fileInfo.isFile}")
//        println("  可读: ${fileInfo.canRead}")
//        println("  可写: ${fileInfo.canWrite}")
//        println("  最后修改: ${Date(fileInfo.lastModified)}")
//
//        // 测试扩展名和文件名处理
//        val extension = FileUtil.getExtension(testFile.absolutePath)
//        val nameWithoutExt = FileUtil.getNameWithoutExtension(testFile.absolutePath)
//        println("✓ 扩展名: $extension")
//        println("✓ 无扩展名文件名: $nameWithoutExt")
//
//    } catch (e: Exception) {
//        println("✗ 获取文件信息失败: ${e.message}")
//    }
//}
//
///**
// * 测试文件查找和过滤
// */
//private fun testFileSearch(testDir: File) {
//    try {
//        // 根据扩展名查找文件
//        val txtFiles = FileUtil.listFilesByExtension(testDir.absolutePath, listOf("txt"), recursive = true)
//        println("✓ 找到 ${txtFiles.size} 个 .txt 文件:")
//        txtFiles.take(5).forEach { file ->
//            println("  ${file.relativeTo(testDir)}")
//        }
//
//        // 使用模式查找文件
//        val foundFiles = FileUtil.findFiles(testDir.absolutePath, "*test*", recursive = true)
//        println("✓ 找到 ${foundFiles.size} 个包含'test'的文件:")
//        foundFiles.forEach { file ->
//            println("  ${file.name}")
//        }
//
//    } catch (e: Exception) {
//        println("✗ 文件查找失败: ${e.message}")
//    }
//}
//
///**
// * 测试文件哈希计算
// */
//private fun testFileHash(testDir: File) {
//    val testFile = File(testDir, "readwrite_test.txt")
//
//    try {
//        val md5 = FileUtil.calculateMD5(testFile.absolutePath)
//        val sha256 = FileUtil.calculateSHA256(testFile.absolutePath)
//
//        println("✓ 文件哈希值:")
//        println("  MD5: $md5")
//        println("  SHA256: $sha256")
//
//        // 测试文件比较
//        val copyFile = File(testDir, "moved_test.txt")
//        if (copyFile.exists()) {
//            val isSame = FileUtil.compareFiles(testFile.absolutePath, copyFile.absolutePath)
//            println("✓ 文件比较结果: ${if (isSame) "相同" else "不同"}")
//        }
//
//    } catch (e: Exception) {
//        println("✗ 文件哈希计算失败: ${e.message}")
//    }
//}
//
///**
// * 测试ZIP压缩解压
// */
//private fun testZipOperations(testDir: File) {
//    val zipFile = File(testDir, "test.zip")
//    val extractDir = File(testDir, "extracted")
//
//    try {
//        // 准备要压缩的文件
//        val filesToZip = listOf(
//            File(testDir, "readwrite_test.txt").absolutePath,
//            File(testDir, "subdir").absolutePath
//        ).filter { FileUtil.exists(it) }
//
//        if (filesToZip.isNotEmpty()) {
//            // 压缩文件
//            val zipped = FileUtil.zipFiles(filesToZip, zipFile.absolutePath)
//            println("✓ 压缩文件: ${if (zipped) "成功" else "失败"}")
//
//            if (zipped) {
//                val zipSize = FileUtil.formatFileSize(FileUtil.getFileSize(zipFile.absolutePath))
//                println("✓ 压缩包大小: $zipSize")
//
//                // 解压文件
//                val extracted = FileUtil.unzipFile(zipFile.absolutePath, extractDir.absolutePath)
//                println("✓ 解压文件: ${if (extracted) "成功" else "失败"}")
//
//                if (extracted) {
//                    val extractedFiles = FileUtil.listFiles(extractDir.absolutePath, recursive = true)
//                    println("✓ 解压后文件数量: ${extractedFiles.size}")
//                }
//            }
//        } else {
//            println("✓ 没有找到可压缩的文件")
//        }
//
//    } catch (e: Exception) {
//        println("✗ ZIP操作失败: ${e.message}")
//    }
//}
//
///**
// * 测试文件大小格式化
// */
//private fun testFileSizeFormatting() {
//    val sizes = listOf(0L, 512L, 1024L, 1048576L, 1073741824L, 1099511627776L)
//
//    println("✓ 文件大小格式化测试:")
//    sizes.forEach { size ->
//        val formatted = FileUtil.formatFileSize(size)
//        println("  $size 字节 = $formatted")
//    }
//}
//
///**
// * 清理测试文件
// */
//private fun cleanupTestFiles(testDir: File) {
//    try {
//        if (testDir.exists()) {
//            val deleted = FileUtil.delete(testDir.absolutePath)
//            println("✓ 测试文件清理: ${if (deleted) "成功" else "失败"}")
//        }
//    } catch (e: Exception) {
//        println("✗ 清理测试文件失败: ${e.message}")
//    }
//}

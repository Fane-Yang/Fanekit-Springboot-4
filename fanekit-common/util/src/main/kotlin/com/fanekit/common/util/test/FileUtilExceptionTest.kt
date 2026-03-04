package com.fanekit.common.util.test

import com.fanekit.common.core.exception.UtilException
import com.fanekit.common.util.io.FileUtil

/**
 * FileUtil异常测试类
 * 专门用于测试工具类异常的堆栈跟踪效果
 */
object FileUtilExceptionTest {

    @JvmStatic
    fun main(args: Array<String>) {
        println("=== FileUtil 异常堆栈测试 ===\n")

        // 测试1：文件不存在异常
        testFileNotFound()

//        // 测试2：路径不是文件异常
//        testPathNotFile()
//
//        // 测试3：删除不存在文件异常
//        testDeleteNonexistentFile()
//
//        // 测试4：创建无效文件异常
//        testCreateInvalidFile()
//
//        // 测试5：测试调用链异常
//        testCallChainException()

        println("\n=== 测试完成 ===")
    }

    private fun testFileNotFound() {
        println("1. 测试文件不存在异常:")
        try {
            FileUtil.readText("不存在的文件.txt")
        } catch (ex: UtilException) {
            printExceptionDetails("文件不存在", ex)
        } catch (ex: Exception) {
            println("  意外异常: ${ex::class.simpleName}: ${ex.message}")
        }
        println()
    }

    private fun testPathNotFile() {
        println("2. 测试路径不是文件异常:")
        try {
            // 创建临时目录用于测试
            val tempDir = FileUtil.createTempDirectory("test_dir")
            println("  创建临时目录: ${tempDir.absolutePath}")

            // 尝试读取目录（应该抛出异常）
            FileUtil.readText(tempDir.absolutePath)
        } catch (ex: UtilException) {
            printExceptionDetails("路径不是文件", ex)
        } catch (ex: Exception) {
            println("  意外异常: ${ex::class.simpleName}: ${ex.message}")
        }
        println()
    }

    private fun testDeleteNonexistentFile() {
        println("3. 测试删除不存在文件异常:")
        try {
            FileUtil.delete("完全不存在的文件.txt")
        } catch (ex: UtilException) {
            printExceptionDetails("删除不存在文件", ex)
        } catch (ex: Exception) {
            println("  意外异常: ${ex::class.simpleName}: ${ex.message}")
        }
        println()
    }

    private fun testCreateInvalidFile() {
        println("4. 测试创建无效文件异常:")
        try {
            // 使用Windows保留文件名或无效路径
            val invalidPath = if (System.getProperty("os.name").lowercase().contains("windows")) {
                "CON"  // Windows保留文件名
            } else {
                "/dev/null/impossible.txt"  // 无效路径
            }
            println("  尝试创建无效文件: $invalidPath")
            FileUtil.createFile(invalidPath)
        } catch (ex: UtilException) {
            printExceptionDetails("创建无效文件", ex)
        } catch (ex: Exception) {
            println("  意外异常: ${ex::class.simpleName}: ${ex.message}")
        }
        println()
    }

    private fun testCallChainException() {
        println("5. 测试调用链异常:")
        try {
            methodA()
        } catch (ex: UtilException) {
            printExceptionDetails("调用链", ex)
        } catch (ex: Exception) {
            println("  意外异常: ${ex::class.simpleName}: ${ex.message}")
        }
    }

    // 模拟调用链：methodA -> methodB -> methodC -> FileUtil
    private fun methodA() {
        println("  进入 methodA")
        methodB()
    }

    private fun methodB() {
        println("  进入 methodB")
        methodC()
    }

    private fun methodC() {
        println("  进入 methodC，调用 FileUtil.readText")
        FileUtil.readText("不存在的调用链测试文件.txt")
    }

    private fun printExceptionDetails(testName: String, ex: UtilException) {
        println("  ✓ 成功捕获 $testName 异常")
        println("  异常类型: ${ex::class.simpleName}")
        println("  错误码: ${ex.code}")
        println("  错误信息: ${ex.message}")

        if (ex.cause != null) {
            println("  原始异常: ${ex.cause!!.message}")
            println("  原始异常类型: ${ex.cause!!::class.simpleName}")

            println("  完整堆栈跟踪:" )
            ex.printStackTrace()
            println("  ----------------------------------------")
            println("  关键堆栈信息:")
            val importantFrames = ex.stackTrace.filter { frame ->
                frame.className.contains("FileUtil") ||
                frame.className.contains("Test") ||
                frame.methodName == "main"
            }
            importantFrames.forEach { frame ->
                println("    at ${frame.className}.${frame.methodName}(${frame.fileName}:${frame.lineNumber})")
            }

            if (ex.cause != null) {
                println("  原始异常堆栈:")
                ex.cause!!.stackTrace.take(3).forEach { frame ->
                    println("    at ${frame.className}.${frame.methodName}(${frame.fileName}:${frame.lineNumber})")
                }
            }
        }
        println("  ========================================")
    }
}

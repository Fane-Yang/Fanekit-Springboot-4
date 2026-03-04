package com.fanekit.common.util.encryption

import org.bouncycastle.util.encoders.Hex

/**
 * 十六进制编码解码工具类
 * 封装BouncyCastle的Hex功能
 */
object HexUtils {

    /**
     * 十六进制编码 - 字节数组转十六进制字符串
     * @param data 要编码的字节数组
     * @return 十六进制字符串（小写）
     */
    fun encode(data: ByteArray): String {
        return Hex.toHexString(data)
    }

    /**
     * 十六进制编码 - 字符串转十六进制字符串
     * @param data 要编码的字符串
     * @return 十六进制字符串（小写）
     */
    fun encode(data: String): String {
        return Hex.toHexString(data.toByteArray())
    }

    /**
     * 十六进制解码 - 十六进制字符串转字节数组
     * @param hexString 十六进制字符串
     * @return 解码后的字节数组
     */
    fun decode(hexString: String): ByteArray {
        return Hex.decode(hexString)
    }

    /**
     * 十六进制解码 - 十六进制字符串转字符串
     * @param hexString 十六进制字符串
     * @return 解码后的字符串
     */
    fun decodeToString(hexString: String): String {
        return String(Hex.decode(hexString))
    }

    /**
     * 十六进制编码 - 字节数组转大写十六进制字符串
     * @param data 要编码的字节数组
     * @return 大写十六进制字符串
     */
    fun encodeUpperCase(data: ByteArray): String {
        return Hex.toHexString(data).uppercase()
    }

    /**
     * 十六进制编码 - 字符串转大写十六进制字符串
     * @param data 要编码的字符串
     * @return 大写十六进制字符串
     */
    fun encodeUpperCase(data: String): String {
        return Hex.toHexString(data.toByteArray()).uppercase()
    }

    /**
     * 检查字符串是否为有效的十六进制格式
     * @param hexString 要检查的字符串
     * @return 如果是有效的十六进制格式返回true，否则返回false
     */
    fun isValidHex(hexString: String): Boolean {
        if (hexString.isEmpty() || hexString.length % 2 != 0) {
            return false
        }
        return try {
            Hex.decode(hexString)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 格式化十六进制字符串 - 添加空格分隔符
     * @param hexString 十六进制字符串
     * @param separator 分隔符，默认为空格
     * @param groupSize 分组大小，默认为2（即每两个字符一组）
     * @return 格式化后的十六进制字符串
     */
    fun format(hexString: String, separator: String = " ", groupSize: Int = 2): String {
        if (hexString.isEmpty() || groupSize <= 0) {
            return hexString
        }
        return hexString.chunked(groupSize).joinToString(separator)
    }

    /**
     * 移除十六进制字符串中的分隔符和空格
     * @param hexString 带有分隔符的十六进制字符串
     * @return 清理后的十六进制字符串
     */
    fun clean(hexString: String): String {
        return hexString.replace("\\s".toRegex(), "")
            .replace(":", "")
            .replace("-", "")
            .replace(",", "")
    }
}

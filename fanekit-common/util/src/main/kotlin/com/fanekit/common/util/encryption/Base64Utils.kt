package com.fanekit.common.util.encryption

import java.util.*

/**
 * Base64编码解码工具类
 * 封装Java标准库的Base64功能
 */
object Base64Utils {

    /**
     * Base64编码 - 字节数组转Base64字符串
     * @param data 要编码的字节数组
     * @return Base64编码后的字符串
     */
    fun encode(data: ByteArray): String {
        return Base64.getEncoder().encodeToString(data)
    }

    /**
     * Base64编码 - 字符串转Base64字符串
     * @param data 要编码的字符串
     * @return Base64编码后的字符串
     */
    fun encode(data: String): String {
        return Base64.getEncoder().encodeToString(data.toByteArray())
    }

    /**
     * Base64解码 - Base64字符串转字节数组
     * @param data Base64编码的字符串
     * @return 解码后的字节数组
     */
    fun decode(data: String): ByteArray {
        return Base64.getDecoder().decode(data)
    }

    /**
     * Base64解码 - Base64字符串转字符串
     * @param data Base64编码的字符串
     * @return 解码后的字符串
     */
    fun decodeToString(data: String): String {
        return String(Base64.getDecoder().decode(data))
    }

    /**
     * URL安全的Base64编码 - 字节数组转Base64字符串
     * @param data 要编码的字节数组
     * @return URL安全的Base64编码后的字符串
     */
    fun encodeUrlSafe(data: ByteArray): String {
        return Base64.getUrlEncoder().encodeToString(data)
    }

    /**
     * URL安全的Base64编码 - 字符串转Base64字符串
     * @param data 要编码的字符串
     * @return URL安全的Base64编码后的字符串
     */
    fun encodeUrlSafe(data: String): String {
        return Base64.getUrlEncoder().encodeToString(data.toByteArray())
    }

    /**
     * URL安全的Base64解码 - Base64字符串转字节数组
     * @param data URL安全的Base64编码的字符串
     * @return 解码后的字节数组
     */
    fun decodeUrlSafe(data: String): ByteArray {
        return Base64.getUrlDecoder().decode(data)
    }

    /**
     * URL安全的Base64解码 - Base64字符串转字符串
     * @param data URL安全的Base64编码的字符串
     * @return 解码后的字符串
     */
    fun decodeUrlSafeToString(data: String): String {
        return String(Base64.getUrlDecoder().decode(data))
    }

    /**
     * 检查字符串是否为有效的Base64格式
     * @param data 要检查的字符串
     * @return 如果是有效的Base64格式返回true，否则返回false
     */
    fun isValidBase64(data: String): Boolean {
        return try {
            Base64.getDecoder().decode(data)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}

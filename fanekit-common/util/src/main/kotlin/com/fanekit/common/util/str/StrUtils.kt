package com.fanekit.common.util.str

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import java.util.regex.Pattern

/**
 * 字符串工具类
 * 提供常用的字符串处理方法
 */
object StrUtils {

    private const val EMPTY = ""
    private val EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    )
    private val PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$")
    private val ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{5}(18|19|([23]\\d))\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$")

    /**
     * 判断字符串是否为空
     */
    @JvmStatic
    fun isEmpty(str: String?): Boolean = str.isNullOrEmpty()

    /**
     * 判断字符串是否不为空
     */
    @JvmStatic
    fun isNotEmpty(str: String?): Boolean = !isEmpty(str)

    /**
     * 判断字符串是否为空白（包括空格、制表符等）
     */
    @JvmStatic
    fun isBlank(str: String?): Boolean = str.isNullOrBlank()

    /**
     * 判断字符串是否不为空白
     */
    @JvmStatic
    fun isNotBlank(str: String?): Boolean = !isBlank(str)

    /**
     * 如果字符串为空则返回默认值
     */
    @JvmStatic
    fun defaultIfEmpty(str: String?, defaultValue: String): String {
        return if (isEmpty(str)) defaultValue else str!!
    }

    /**
     * 如果字符串为空白则返回默认值
     */
    @JvmStatic
    fun defaultIfBlank(str: String?, defaultValue: String): String {
        return if (isBlank(str)) defaultValue else str!!
    }

    /**
     * 去除字符串前后空格，如果为null则返回空字符串
     */
    @JvmStatic
    fun trim(str: String?): String = str?.trim() ?: EMPTY

    /**
     * 去除字符串前后空格，如果为null或空字符串则返回null
     */
    @JvmStatic
    fun trimToNull(str: String?): String? {
        val trimmed = trim(str)
        return if (trimmed.isEmpty()) null else trimmed
    }

    /**
     * 驼峰转下划线
     */
    @JvmStatic
    fun camelToSnake(str: String?): String {
        if (isEmpty(str)) return EMPTY
        return str!!.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    }

    /**
     * 下划线转驼峰
     */
    @JvmStatic
    fun snakeToCamel(str: String?): String {
        if (isEmpty(str)) return EMPTY
        return str!!.split("_").mapIndexed { index, s ->
            if (index == 0) s.lowercase() else s.lowercase().replaceFirstChar { it.uppercaseChar() }
        }.joinToString("")
    }

    /**
     * 首字母大写
     */
    @JvmStatic
    fun capitalize(str: String?): String {
        if (isEmpty(str)) return EMPTY
        return str!!.replaceFirstChar { it.uppercaseChar() }
    }

    /**
     * 首字母小写
     */
    @JvmStatic
    fun uncapitalize(str: String?): String {
        if (isEmpty(str)) return EMPTY
        return str!!.replaceFirstChar { it.lowercaseChar() }
    }

    /**
     * 反转字符串
     */
    @JvmStatic
    fun reverse(str: String?): String {
        if (isEmpty(str)) return EMPTY
        return str!!.reversed()
    }

    /**
     * 重复字符串指定次数
     */
    @JvmStatic
    fun repeat(str: String?, times: Int): String {
        if (isEmpty(str) || times <= 0) return EMPTY
        return str!!.repeat(times)
    }

    /**
     * 截取字符串（如果超长则加省略号）
     */
    @JvmStatic
    fun truncate(str: String?, maxLength: Int, ellipsis: String = "..."): String {
        if (isEmpty(str) || maxLength <= 0) return EMPTY
        if (str!!.length <= maxLength) return str
        return str.substring(0, maxLength - ellipsis.length) + ellipsis
    }

    /**
     * 左补齐字符串
     */
    @JvmStatic
    fun padLeft(str: String?, length: Int, padChar: Char = ' '): String {
        if (isEmpty(str)) return padChar.toString().repeat(length)
        if (str!!.length >= length) return str
        return padChar.toString().repeat(length - str.length) + str
    }

    /**
     * 右补齐字符串
     */
    @JvmStatic
    fun padRight(str: String?, length: Int, padChar: Char = ' '): String {
        if (isEmpty(str)) return padChar.toString().repeat(length)
        if (str!!.length >= length) return str
        return str + padChar.toString().repeat(length - str.length)
    }

    /**
     * 移除字符串中的所有空白字符
     */
    @JvmStatic
    fun removeWhitespace(str: String?): String {
        if (isEmpty(str)) return EMPTY
        return str!!.replace(Regex("\\s"), "")
    }

    /**
     * 移除字符串开头的指定字符串
     */
    @JvmStatic
    fun removeStart(str: String?, remove: String?): String {
        if (isEmpty(str) || isEmpty(remove)) return str ?: EMPTY
        if (str!!.startsWith(remove!!)) {
            return str.substring(remove.length)
        }
        return str
    }

    /**
     * 移除字符串结尾的指定字符串
     */
    @JvmStatic
    fun removeEnd(str: String?, remove: String?): String {
        if (isEmpty(str) || isEmpty(remove)) return str ?: EMPTY
        if (str!!.endsWith(remove!!)) {
            return str.substring(0, str.length - remove.length)
        }
        return str
    }

    /**
     * 计算字符串的MD5值
     */
    @JvmStatic
    fun md5(str: String?): String {
        if (isEmpty(str)) return EMPTY
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(str!!.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * 生成随机字符串
     */
    @JvmStatic
    fun randomString(length: Int, includeNumbers: Boolean = true, includeLetters: Boolean = true): String {
        if (length <= 0) return EMPTY

        val chars = buildString {
            if (includeLetters) append("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
            if (includeNumbers) append("0123456789")
        }

        if (chars.isEmpty()) return EMPTY

        val random = Random()
        return (1..length).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }

    /**
     * 验证邮箱格式
     */
    @JvmStatic
    fun isValidEmail(email: String?): Boolean {
        if (isEmpty(email)) return false
        return EMAIL_PATTERN.matcher(email!!).matches()
    }

    /**
     * 验证手机号格式（中国大陆）
     */
    @JvmStatic
    fun isValidPhone(phone: String?): Boolean {
        if (isEmpty(phone)) return false
        return PHONE_PATTERN.matcher(phone!!).matches()
    }

    /**
     * 验证身份证号格式（中国大陆）
     */
    @JvmStatic
    fun isValidIdCard(idCard: String?): Boolean {
        if (isEmpty(idCard)) return false
        return ID_CARD_PATTERN.matcher(idCard!!).matches()
    }

    /**
     * 手机号脱敏（显示前3位和后4位）
     */
    @JvmStatic
    fun maskPhone(phone: String?): String {
        if (isEmpty(phone) || phone!!.length != 11) return phone ?: EMPTY
        return phone.substring(0, 3) + "****" + phone.substring(7)
    }

    /**
     * 邮箱脱敏
     */
    @JvmStatic
    fun maskEmail(email: String?): String {
        if (isEmpty(email) || !email!!.contains("@")) return email ?: EMPTY
        val parts = email.split("@")
        val username = parts[0]
        val domain = parts[1]

        return when {
            username.length <= 2 -> "${username.first()}***@$domain"
            username.length <= 4 -> "${username.substring(0, 2)}***@$domain"
            else -> "${username.substring(0, 3)}***${username.last()}@$domain"
        }
    }

    /**
     * 身份证号脱敏
     */
    @JvmStatic
    fun maskIdCard(idCard: String?): String {
        if (isEmpty(idCard) || idCard!!.length != 18) return idCard ?: EMPTY
        return idCard.substring(0, 6) + "********" + idCard.substring(14)
    }

    /**
     * 银行卡号脱敏
     */
    @JvmStatic
    fun maskBankCard(bankCard: String?): String {
        if (isEmpty(bankCard) || bankCard!!.length < 8) return bankCard ?: EMPTY
        return bankCard.substring(0, 4) + "****" + bankCard.substring(bankCard.length - 4)
    }

    /**
     * 统计字符串中指定字符的个数
     */
    @JvmStatic
    fun countMatches(str: String?, ch: Char): Int {
        if (isEmpty(str)) return 0
        return str!!.count { it == ch }
    }

    /**
     * 统计字符串中指定子字符串的个数
     */
    @JvmStatic
    fun countMatches(str: String?, sub: String?): Int {
        if (isEmpty(str) || isEmpty(sub)) return 0
        var count = 0
        var index = 0
        while (str!!.indexOf(sub!!, index).also { index = it } != -1) {
            count++
            index += sub.length
        }
        return count
    }

    /**
     * 判断字符串是否只包含数字
     */
    @JvmStatic
    fun isNumeric(str: String?): Boolean {
        if (isEmpty(str)) return false
        return str!!.all { it.isDigit() }
    }

    /**
     * 判断字符串是否只包含字母
     */
    @JvmStatic
    fun isAlpha(str: String?): Boolean {
        if (isEmpty(str)) return false
        return str!!.all { it.isLetter() }
    }

    /**
     * 判断字符串是否只包含字母和数字
     */
    @JvmStatic
    fun isAlphanumeric(str: String?): Boolean {
        if (isEmpty(str)) return false
        return str!!.all { it.isLetterOrDigit() }
    }

    /**
     * 将字符串转换为URL安全的格式
     */
    @JvmStatic
    fun toUrlSafe(str: String?): String {
        if (isEmpty(str)) return EMPTY
        return str!!.replace(Regex("[^a-zA-Z0-9-_.]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
    }

    /**
     * 格式化文件大小
     */
    @JvmStatic
    fun formatFileSize(size: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var fileSize = size.toDouble()
        var unitIndex = 0

        while (fileSize >= 1024 && unitIndex < units.size - 1) {
            fileSize /= 1024
            unitIndex++
        }

        return if (unitIndex == 0) {
            "${size}${units[unitIndex]}"
        } else {
            "%.2f${units[unitIndex]}".format(fileSize)
        }
    }

    /**
     * 检查字符串是否以任意一个指定的前缀开头
     */
    @JvmStatic
    fun startsWithAny(str: String?, vararg prefixes: String?): Boolean {
        if (isEmpty(str) || prefixes.isEmpty()) return false
        return prefixes.any { prefix ->
            !isEmpty(prefix) && str!!.startsWith(prefix!!)
        }
    }

    /**
     * 检查字符串是否以任意一个指定的前缀开头（忽略大小写）
     */
    @JvmStatic
    fun startsWithAnyIgnoreCase(str: String?, vararg prefixes: String?): Boolean {
        if (isEmpty(str) || prefixes.isEmpty()) return false
        return prefixes.any { prefix ->
            !isEmpty(prefix) && str!!.startsWith(prefix!!, ignoreCase = true)
        }
    }

    /**
     * 检查字符串是否以任意一个指定的后缀结尾
     */
    @JvmStatic
    fun endsWithAny(str: String?, vararg suffixes: String?): Boolean {
        if (isEmpty(str) || suffixes.isEmpty()) return false
        return suffixes.any { suffix ->
            !isEmpty(suffix) && str!!.endsWith(suffix!!)
        }
    }

    /**
     * 检查字符串是否以任意一个指定的后缀结尾（忽略大小写）
     */
    @JvmStatic
    fun endsWithAnyIgnoreCase(str: String?, vararg suffixes: String?): Boolean {
        if (isEmpty(str) || suffixes.isEmpty()) return false
        return suffixes.any { suffix ->
            !isEmpty(suffix) && str!!.endsWith(suffix!!, ignoreCase = true)
        }
    }

    /**
     * 检查字符串是否包含任意一个指定的子字符串
     */
    @JvmStatic
    fun containsAny(str: String?, vararg subStrings: String?): Boolean {
        if (isEmpty(str) || subStrings.isEmpty()) return false
        return subStrings.any { subString ->
            !isEmpty(subString) && str!!.contains(subString!!)
        }
    }

    /**
     * 检查字符串是否包含任意一个指定的子字符串（忽略大小写）
     */
    @JvmStatic
    fun containsAnyIgnoreCase(str: String?, vararg subStrings: String?): Boolean {
        if (isEmpty(str) || subStrings.isEmpty()) return false
        return subStrings.any { subString ->
            !isEmpty(subString) && str!!.contains(subString!!, ignoreCase = true)
        }
    }

    /**
     * 检查字符串是否等于任意一个指定的字符串
     */
    @JvmStatic
    fun equalsAny(str: String?, vararg values: String?): Boolean {
        if (str == null && values.isEmpty()) return false
        return values.any { value -> str == value }
    }

    /**
     * 检查字符串是否等于任意一个指定的字符串（忽略大小写）
     */
    @JvmStatic
    fun equalsAnyIgnoreCase(str: String?, vararg values: String?): Boolean {
        if (isEmpty(str) || values.isEmpty()) return false
        return values.any { value ->
            !isEmpty(value) && str!!.equals(value!!, ignoreCase = true)
        }
    }

    /**
     * 检查字符串是否不等于所有指定的字符串
     */
    @JvmStatic
    fun equalsNone(str: String?, vararg values: String?): Boolean {
        return !equalsAny(str, *values)
    }

    /**
     * 检查字符串是否不等于所有指定的字符串（忽略大小写）
     */
    @JvmStatic
    fun equalsNoneIgnoreCase(str: String?, vararg values: String?): Boolean {
        return !equalsAnyIgnoreCase(str, *values)
    }

    /**
     * 检查字符串是否包含所有指定的子字符串
     */
    @JvmStatic
    fun containsAll(str: String?, vararg subStrings: String?): Boolean {
        if (isEmpty(str) || subStrings.isEmpty()) return false
        return subStrings.all { subString ->
            !isEmpty(subString) && str!!.contains(subString!!)
        }
    }

    /**
     * 检查字符串是否包含所有指定的子字符串（忽略大小写）
     */
    @JvmStatic
    fun containsAllIgnoreCase(str: String?, vararg subStrings: String?): Boolean {
        if (isEmpty(str) || subStrings.isEmpty()) return false
        return subStrings.all { subString ->
            !isEmpty(subString) && str!!.contains(subString!!, ignoreCase = true)
        }
    }

    /**
     * 检查字符串是否不包含任何指定的子字符串
     */
    @JvmStatic
    fun containsNone(str: String?, vararg subStrings: String?): Boolean {
        return !containsAny(str, *subStrings)
    }

    /**
     * 检查字符串是否不包含任何指定的子字符串（忽略大小写）
     */
    @JvmStatic
    fun containsNoneIgnoreCase(str: String?, vararg subStrings: String?): Boolean {
        return !containsAnyIgnoreCase(str, *subStrings)
    }

    /**
     * 在字符串数组中查找第一个匹配的前缀
     */
    @JvmStatic
    fun findFirstStartsWith(str: String?, vararg prefixes: String?): String? {
        if (isEmpty(str) || prefixes.isEmpty()) return null
        return prefixes.firstOrNull { prefix ->
            !isEmpty(prefix) && str!!.startsWith(prefix!!)
        }
    }

    /**
     * 在字符串数组中查找第一个匹配的后缀
     */
    @JvmStatic
    fun findFirstEndsWith(str: String?, vararg suffixes: String?): String? {
        if (isEmpty(str) || suffixes.isEmpty()) return null
        return suffixes.firstOrNull { suffix ->
            !isEmpty(suffix) && str!!.endsWith(suffix!!)
        }
    }

    /**
     * 在字符串数组中查找第一个包含的子字符串
     */
    @JvmStatic
    fun findFirstContains(str: String?, vararg subStrings: String?): String? {
        if (isEmpty(str) || subStrings.isEmpty()) return null
        return subStrings.firstOrNull { subString ->
            !isEmpty(subString) && str!!.contains(subString!!)
        }
    }

    /**
     * 使用分隔符连接字符串数组（过滤空值）
     */
    @JvmStatic
    fun joinNonEmpty(separator: String, vararg strings: String?): String {
        return strings.filterNotNull().filter { it.isNotEmpty() }.joinToString(separator)
    }

    /**
     * 使用分隔符连接字符串数组（过滤空白值）
     */
    @JvmStatic
    fun joinNonBlank(separator: String, vararg strings: String?): String {
        return strings.filterNotNull().filter { it.isNotBlank() }.joinToString(separator)
    }

    /**
     * 安全的子字符串截取（不会抛出越界异常）
     */
    @JvmStatic
    fun safeSubstring(str: String?, start: Int, end: Int? = null): String {
        if (isEmpty(str)) return EMPTY
        val s = str!!
        val safeStart = maxOf(0, minOf(start, s.length))
        val safeEnd = if (end != null) maxOf(safeStart, minOf(end, s.length)) else s.length
        return s.substring(safeStart, safeEnd)
    }

    /**
     * 替换字符串中的所有匹配项
     */
    @JvmStatic
    fun replaceAll(str: String?, vararg replacements: Pair<String, String>): String {
        if (isEmpty(str) || replacements.isEmpty()) return str ?: EMPTY
        var result = str!!
        replacements.forEach { (old, new) ->
            result = result.replace(old, new)
        }
        return result
    }
}
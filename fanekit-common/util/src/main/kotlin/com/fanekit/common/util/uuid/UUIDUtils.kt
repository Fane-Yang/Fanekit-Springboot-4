package com.fanekit.common.util.uuid

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.regex.Pattern

/**
 * UUID工具类
 * 提供各种UUID生成、验证、转换功能
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2025/04/18
 * @since 1.0.0
 * @version 1.0.0
 */
object UUIDUtils {

    /**
     * UUID格式枚举
     */
    enum class UUIDFormat {
        /** 标准格式：xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx */
        STANDARD,
        /** 简单格式：xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx */
        SIMPLE,
        /** 大写格式：XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX */
        UPPERCASE,
        /** 大写简单格式：XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX */
        UPPERCASE_SIMPLE
    }

    /**
     * UUID版本枚举
     */
    enum class UUIDVersion(val version: Int) {
        /** 基于时间的UUID */
        TIME_BASED(1),
        /** DCE安全UUID */
        DCE_SECURITY(2),
        /** 基于名称的UUID (MD5) */
        NAME_BASED_MD5(3),
        /** 随机生成的UUID */
        RANDOM(4),
        /** 基于名称的UUID (SHA-1) */
        NAME_BASED_SHA1(5)
    }

    // UUID格式正则表达式
    private val UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    )
    private val SIMPLE_UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{32}$")

    // ================== 基本UUID生成方法 ==================

    /**
     * 生成标准格式的随机UUID
     * @return 格式: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
     */
    fun randomUUID(): String = java.util.UUID.randomUUID().toString()

    /**
     * 生成简单格式的随机UUID（无连字符）
     * @return 格式: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     */
    fun randomSimpleUUID(): String = java.util.UUID.randomUUID().toString().replace("-", "")

    /**
     * 生成指定格式的随机UUID
     * @param format UUID格式
     * @return 指定格式的UUID字符串
     */
    fun randomUUID(format: UUIDFormat): String {
        val uuid = java.util.UUID.randomUUID().toString()
        return formatUUID(uuid, format)
    }

    /**
     * 批量生成UUID
     * @param count 生成数量
     * @param format UUID格式，默认标准格式
     * @return UUID列表
     */
    fun batchRandomUUID(count: Int, format: UUIDFormat = UUIDFormat.STANDARD): List<String> {
        require(count > 0) { "生成数量必须大于0" }
        return (1..count).map { randomUUID(format) }
    }

    // ================== 基于名称的UUID生成 ==================

    /**
     * 基于名称生成UUID（MD5版本3）
     * @param name 名称字符串
     * @param format UUID格式，默认标准格式
     * @return 基于名称的UUID
     */
    fun nameBasedUUID(name: String, format: UUIDFormat = UUIDFormat.STANDARD): String {
        val uuid = nameUUIDFromBytes(name.toByteArray(Charsets.UTF_8))
        return formatUUID(uuid, format)
    }

    /**
     * 基于名称和命名空间生成UUID
     * @param namespace 命名空间UUID
     * @param name 名称字符串
     * @param format UUID格式，默认标准格式
     * @return 基于名称的UUID
     */
    fun nameBasedUUID(namespace: String, name: String, format: UUIDFormat = UUIDFormat.STANDARD): String {
        val namespaceBytes = parseUUID(namespace).toByteArray()
        val nameBytes = name.toByteArray(Charsets.UTF_8)
        val combined = namespaceBytes + nameBytes
        val uuid = nameUUIDFromBytes(combined)
        return formatUUID(uuid, format)
    }

    /**
     * 根据字节数组生成基于名称的UUID（MD5版本3）
     */
    private fun nameUUIDFromBytes(bytes: ByteArray): String {
        val md = try {
            MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("MD5算法不可用", e)
        }

        val hash = md.digest(bytes)
        hash[6] = (hash[6].toInt() and 0x0f or 0x30).toByte() // 设置版本为3
        hash[8] = (hash[8].toInt() and 0x3f or 0x80).toByte() // 设置变体

        return formatBytes(hash)
    }

    // ================== UUID格式转换方法 ==================

    /**
     * 格式化UUID字符串
     * @param uuid 原始UUID字符串
     * @param format 目标格式
     * @return 格式化后的UUID字符串
     */
    fun formatUUID(uuid: String, format: UUIDFormat): String {
        val cleanUuid = uuid.replace("-", "")
        require(cleanUuid.length == 32) { "无效的UUID格式: $uuid" }

        return when (format) {
            UUIDFormat.STANDARD -> "${cleanUuid.substring(0, 8)}-${cleanUuid.substring(8, 12)}-${cleanUuid.substring(12, 16)}-${cleanUuid.substring(16, 20)}-${cleanUuid.substring(20)}"
            UUIDFormat.SIMPLE -> cleanUuid
            UUIDFormat.UPPERCASE -> "${cleanUuid.substring(0, 8)}-${cleanUuid.substring(8, 12)}-${cleanUuid.substring(12, 16)}-${cleanUuid.substring(16, 20)}-${cleanUuid.substring(20)}".uppercase()
            UUIDFormat.UPPERCASE_SIMPLE -> cleanUuid.uppercase()
        }
    }

    /**
     * 标准格式转简单格式
     */
    fun toSimple(uuid: String): String = uuid.replace("-", "")

    /**
     * 简单格式转标准格式
     */
    fun toStandard(simpleUuid: String): String {
        require(simpleUuid.length == 32) { "无效的简单UUID格式: $simpleUuid" }
        return "${simpleUuid.substring(0, 8)}-${simpleUuid.substring(8, 12)}-${simpleUuid.substring(12, 16)}-${simpleUuid.substring(16, 20)}-${simpleUuid.substring(20)}"
    }

    /**
     * UUID转大写
     */
    fun toUpperCase(uuid: String): String = uuid.uppercase()

    /**
     * UUID转小写
     */
    fun toLowerCase(uuid: String): String = uuid.lowercase()

    // ================== UUID验证方法 ==================

    /**
     * 验证UUID格式是否有效
     * @param uuid UUID字符串
     * @return 是否有效
     */
    fun isValid(uuid: String?): Boolean {
        if (uuid.isNullOrBlank()) return false
        return UUID_PATTERN.matcher(uuid).matches() || SIMPLE_UUID_PATTERN.matcher(uuid).matches()
    }

    /**
     * 验证标准格式UUID
     */
    fun isValidStandard(uuid: String?): Boolean {
        if (uuid.isNullOrBlank()) return false
        return UUID_PATTERN.matcher(uuid).matches()
    }

    /**
     * 验证简单格式UUID
     */
    fun isValidSimple(uuid: String?): Boolean {
        if (uuid.isNullOrBlank()) return false
        return SIMPLE_UUID_PATTERN.matcher(uuid).matches()
    }

    /**
     * 获取UUID版本
     * @param uuid UUID字符串
     * @return UUID版本，如果无效则返回null
     */
    fun getVersion(uuid: String): UUIDVersion? {
        if (!isValid(uuid)) return null

        val cleanUuid = uuid.replace("-", "")
        val versionChar = cleanUuid[12]
        val version = versionChar.toString().toIntOrNull() ?: return null

        return UUIDVersion.entries.find { it.version == version }
    }

    /**
     * 获取UUID变体
     * @param uuid UUID字符串
     * @return 变体值，如果无效则返回null
     */
    fun getVariant(uuid: String): String? {
        if (!isValid(uuid)) return null

        val cleanUuid = uuid.replace("-", "")
        val variantChar = cleanUuid[16]
        val variantInt = variantChar.toString().toInt(16)

        return when {
            (variantInt and 0x8) == 0 -> "NCS向后兼容"
            (variantInt and 0xC) == 0x8 -> "RFC 4122标准"
            (variantInt and 0xE) == 0xC -> "Microsoft向后兼容"
            else -> "保留供未来定义"
        }
    }

    // ================== UUID信息提取方法 ==================

    /**
     * 获取UUID详细信息
     * @param uuid UUID字符串
     * @return UUID信息对象
     */
    fun getUUIDInfo(uuid: String): UUIDInfo? {
        if (!isValid(uuid)) return null

        val standardUuid = if (uuid.contains("-")) uuid else toStandard(uuid)
        val javaUuid = parseUUID(standardUuid)

        return UUIDInfo(
            uuid = standardUuid,
            simpleUuid = toSimple(standardUuid),
            version = getVersion(standardUuid),
            variant = getVariant(standardUuid),
            mostSignificantBits = javaUuid.mostSignificantBits,
            leastSignificantBits = javaUuid.leastSignificantBits
        )
    }

    // ================== 工具方法 ==================

    /**
     * 比较两个UUID是否相等（忽略格式）
     */
    fun equals(uuid1: String?, uuid2: String?): Boolean {
        if (uuid1 == null || uuid2 == null) return false
        if (!isValid(uuid1) || !isValid(uuid2)) return false

        val clean1 = uuid1.replace("-", "").lowercase()
        val clean2 = uuid2.replace("-", "").lowercase()
        return clean1 == clean2
    }

    /**
     * 生成空UUID（全0）
     */
    fun nilUUID(format: UUIDFormat = UUIDFormat.STANDARD): String {
        val nilUuid = "00000000-0000-0000-0000-000000000000"
        return formatUUID(nilUuid, format)
    }

    /**
     * 判断是否为空UUID
     */
    fun isNil(uuid: String): Boolean {
        if (!isValid(uuid)) return false
        val cleanUuid = uuid.replace("-", "")
        return cleanUuid.all { it == '0' }
    }

    /**
     * 生成UUID数组
     */
    fun generateArray(size: Int, format: UUIDFormat = UUIDFormat.STANDARD): Array<String> {
        require(size > 0) { "数组大小必须大于0" }
        return Array(size) { randomUUID(format) }
    }

    /**
     * 从字符串解析UUID
     */
    private fun parseUUID(uuid: String): java.util.UUID {
        return java.util.UUID.fromString(uuid)
    }

    /**
     * 将字节数组格式化为UUID字符串
     */
    private fun formatBytes(bytes: ByteArray): String {
        require(bytes.size == 16) { "字节数组长度必须为16" }

        val sb = StringBuilder(36)
        for (i in bytes.indices) {
            if (i == 4 || i == 6 || i == 8 || i == 10) {
                sb.append('-')
            }
            sb.append(String.format("%02x", bytes[i]))
        }
        return sb.toString()
    }

    /**
     * 扩展java.util.UUID为字节数组
     */
    private fun java.util.UUID.toByteArray(): ByteArray {
        val bytes = ByteArray(16)
        val msb = this.mostSignificantBits
        val lsb = this.leastSignificantBits

        for (i in 0..7) {
            bytes[i] = (msb shr (8 * (7 - i))).toByte()
        }
        for (i in 8..15) {
            bytes[i] = (lsb shr (8 * (7 - i))).toByte()
        }
        return bytes
    }

    // ================== 业务场景方法 ==================

    /**
     * 生成会话ID
     */
    fun generateSessionId(): String = randomUUID(UUIDFormat.SIMPLE)

    /**
     * 生成追踪ID
     */
    fun generateTraceId(): String = randomUUID(UUIDFormat.SIMPLE)

    /**
     * 生成请求ID
     */
    fun generateRequestId(): String = randomUUID(UUIDFormat.STANDARD)

    /**
     * 生成文件ID
     */
    fun generateFileId(): String = randomUUID(UUIDFormat.SIMPLE)

    /**
     * 生成事务ID
     */
    fun generateTransactionId(): String = randomUUID(UUIDFormat.SIMPLE)

    /**
     * 生成关联ID
     */
    fun generateCorrelationId(): String = randomUUID(UUIDFormat.STANDARD)

    /**
     * UUID信息数据类
     */
    data class UUIDInfo(
        val uuid: String,                           // 标准格式UUID
        val simpleUuid: String,                     // 简单格式UUID
        val version: UUIDVersion?,                  // UUID版本
        val variant: String?,                       // UUID变体
        val mostSignificantBits: Long,              // 最高有效位
        val leastSignificantBits: Long              // 最低有效位
    ) {
        fun toReadableString(): String {
            return """
                标准格式: $uuid
                简单格式: $simpleUuid
                版本: ${version?.name ?: "未知"}
                变体: ${variant ?: "未知"}
                最高有效位: $mostSignificantBits
                最低有效位: $leastSignificantBits
            """.trimIndent()
        }
    }
}

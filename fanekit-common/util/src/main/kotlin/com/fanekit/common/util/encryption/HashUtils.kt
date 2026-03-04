import java.security.MessageDigest

/**
 * 哈希值计算工具类
 */
object HashUtils {

    /**
     * 计算指定算法的哈希值
     * @param algorithm 哈希算法名称，例如 "SHA-256", "MD5" 等
     * @param data 要计算哈希值的数据
     * @return 计算出的哈希值的十六进制字符串
     */
    fun calculateHash(algorithm: String, data: ByteArray): String {
        val digest = MessageDigest.getInstance(algorithm)
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * 计算 MD5 哈希值
     * @param data 要计算哈希值的数据
     * @return MD5 哈希值的十六进制字符串
     */
    fun md5(data: ByteArray): String {
        return calculateHash("MD5", data)
    }

    /**
     * 计算 SHA-1 哈希值
     * @param data 要计算哈希值的数据
     * @return SHA-1 哈希值的十六进制字符串
     */
    fun sha1(data: ByteArray): String {
        return calculateHash("SHA-1", data)
    }

    /**
     * 计算 SHA-256 哈希值
     * @param data 要计算哈希值的数据
     * @return SHA-256 哈希值的十六进制字符串
     */
    fun sha256(data: ByteArray): String {
        return calculateHash("SHA-256", data)
    }

    /**
     * 计算 SHA-512 哈希值
     * @param data 要计算哈希值的数据
     * @return SHA-512 哈希值的十六进制字符串
     */
    fun sha512(data: ByteArray): String {
        return calculateHash("SHA-512", data)
    }
}

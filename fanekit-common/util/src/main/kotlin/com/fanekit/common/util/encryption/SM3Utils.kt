package com.fanekit.common.util.encryption

import HashUtils
import com.fanekit.common.util.io.Zip4jUtil
import com.fanekit.common.util.io.ZipUtil
import net.lingala.zip4j.model.enums.CompressionLevel
import org.bouncycastle.crypto.digests.SM3Digest
import org.bouncycastle.util.encoders.Hex
import java.io.File
import java.security.MessageDigest
import kotlin.div
import kotlin.random.Random

/**
 * 摘要签名算法：SM3
 * 使用bouncycastle加解密
 */
object SM3Utils {
    /**
     * 使用SM3算法生成哈希
     * @param data 需要哈希的数据
     * @return 数据的哈希值
     */
    fun generateSM3Hash(data: String): String {
        val sm3 = SM3Digest()
        val hash = ByteArray(sm3.digestSize)

        val bytes = data.toByteArray()
        sm3.update(bytes, 0, bytes.size)
        sm3.doFinal(hash, 0)

        return String(Hex.encode(hash))
    }

    /**
     * 使用SM3算法验证哈希
     * @param data 原始数据
     * @param hash 需要验证的哈希
     * @return 如果哈希匹配数据则返回true，否则返回false
     */
    fun verifySM3Hash(data: String, hash: String): Boolean {
        val newHash = generateSM3Hash(data)
        return newHash == hash
    }

    /**
     * 使用SM3算法生成哈希
     * @param data 需要哈希的字节数组
     * @return 数据的哈希值
     */
    fun generateSM3Hash(data: ByteArray): String {
        val sm3 = SM3Digest()
        val hash = ByteArray(sm3.digestSize)
        sm3.update(data, 0, data.size)
        sm3.doFinal(hash, 0)

        return String(Hex.encode(hash))
    }

    /**
     * 使用SM3算法验证哈希
     * @param data 原始字节数组
     * @param hash 需要验证的哈希
     * @return 如果哈希匹配数据则返回true，否则返回false
     */
    fun verifySM3Hash(data: ByteArray, hash: String): Boolean {
        val newHash = generateSM3Hash(data)
        return newHash == hash
    }


}

fun hashWithAlgorithm(algorithm: String, data: ByteArray): String {
    val digest = MessageDigest.getInstance(algorithm)
    val hash = digest.digest(data)
    return hash.joinToString("") { "%02x".format(it) }
}

/**
 * 使用方法
 */
fun main() {
    val randomData = Random.nextBytes(74) // 生成32字节随机数据

    val sha1Hash = HashUtils.sha1(randomData)
    val sha256Hash = HashUtils.sha256(randomData)
    val sm3Hash = SM3Utils.generateSM3Hash(randomData)
    val md5Hash = HashUtils.md5(randomData)
    System.out.println("sha1Hash: ${sha1Hash.length}")
    System.out.println("sha256Hash: ${sha256Hash.length}")
    System.out.println("sm3Hash: ${sm3Hash.length}")
    System.out.println("md5Hash: ${md5Hash.length}")
    // 测试
//    val hash = SM3Utils.generateSM3Hash("你好呀")
//    val result = SM3Utils.verifySM3Hash("你好呀", hash)
//    println("hash: $hash")
//    println("result: $result")
//
//    val hash1 = SM3Utils.generateSM3Hash("你好呀".toByteArray())
//    val result1 = SM3Utils.verifySM3Hash("你好呀".toByteArray(), hash1)
//    println("hash1: $hash1")
//    println("result1: $result1")
//
//    val file = File("test_sm3.txt")
//    file.bufferedWriter().use { writer ->
//        repeat(2000_000) {
//            val randomData = Random.nextBytes(74) // 生成32字节随机数据
//            val sm3Hash = SM3Utils.generateSM3Hash(randomData)
//            val sha1Hash = HashUtils.sha1(randomData)
//            val sha256Hash = HashUtils.sha256(randomData)
//            val md5Hash = HashUtils.md5(randomData)
//            val base64 = java.util.Base64.getEncoder().encodeToString(randomData)
//            writer.write("$sm3Hash,$sha1Hash,$sha256Hash,$md5Hash,$base64")
//            writer.newLine()
//        }
//    }
//    val fileSizeInBytes = file.length()
//    val fileSizeInMB = fileSizeInBytes / (1024.0 * 1024.0)
//    println("文件大小: %.2f MB".format(fileSizeInMB))
//    Zip4jUtil.zipFile(file.path, "test_sm3.zip", config = Zip4jUtil.CompressionConfig(
//        compressionLevel = CompressionLevel.NORMAL
//    ))
//    val zipFileSizeInBytes = File("test_sm3.zip").length()
//    val zipFileSizeInMB = zipFileSizeInBytes / (1024.0 * 1024.0)
//    println("压缩后文件大小: %.2f MB".format(zipFileSizeInMB))
//    // 压缩前后百分比
//    val compressionRatio = (fileSizeInBytes - zipFileSizeInBytes).toDouble() / fileSizeInBytes * 100
//    println("压缩率: %.2f%%".format(compressionRatio))
}
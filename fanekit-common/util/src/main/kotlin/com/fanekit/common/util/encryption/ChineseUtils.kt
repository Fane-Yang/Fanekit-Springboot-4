package com.fanekit.common.util.encryption

import java.nio.charset.StandardCharsets
import java.security.Key
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * 自定义的中文字符加解密方法
 * 可以将文本加解密为中文字符串和从中文字符串去解密
 */
object ChineseUtils {
    private val CHINESE_CHARS = CharArray(256)
    private val CHINESE_TO_BYTE: MutableMap<Char, Byte> = HashMap()

    init {
        for (i in 0..255) {
            CHINESE_CHARS[i] = (0x4e00 + i).toChar() // 0x4e00 is the start of the Chinese unicode block
            CHINESE_TO_BYTE[CHINESE_CHARS[i]] = i.toByte()
        }
    }

    fun bytesToChinese(bytes: ByteArray): String {
        val chinese = StringBuilder()
        for (b in bytes) {
            chinese.append(CHINESE_CHARS[b.toInt() and 0xFF])
        }
        return chinese.toString()
    }

    fun chineseToBytes(chinese: String): ByteArray {
        val bytes = ByteArray(chinese.length)
        for (i in 0..<chinese.length) {
            bytes[i] = CHINESE_TO_BYTE[chinese[i]]!!
        }
        return bytes
    }

    fun encrypt(plainText: String, key: String, offset: String): String {
        val bytes = plainText.toByteArray()
        val keyBytes = key.toByteArray()
        val offsetBytes = offset.toByteArray()
        for (i in bytes.indices) {
            bytes[i] = (bytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
            bytes[i] = (bytes[i] + offsetBytes[i % offsetBytes.size]).toByte()
        }
        return bytesToChinese(bytes)
    }

    fun decrypt(chinese: String, key: String, offset: String): String {
        val bytes = chineseToBytes(chinese)
        val keyBytes = key.toByteArray()
        val offsetBytes = offset.toByteArray()
        for (i in bytes.indices) {
            bytes[i] = (bytes[i] - offsetBytes[i % offsetBytes.size]).toByte()
            bytes[i] = (bytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        return String(bytes)
    }

    private const val AES = "AES"
    private const val RSA = "RSA"
    private const val CHARSET_GBK = "GBK"

    //    private static final String CHARSET_UTF8 = "UTF-8";
    private const val SECRET_KEY = "1234567890123456" // 16 characters for AES-128

    @Throws(Exception::class)
    fun encrypt(plainText: String, publicKey: PublicKey?): String {
        // AES encryption
        val key: Key = SecretKeySpec(SECRET_KEY.toByteArray(charset(CHARSET_GBK)), AES)
        var cipher = Cipher.getInstance(AES)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        var encryptedBytes = cipher.doFinal(plainText.toByteArray(charset(CHARSET_GBK)))

        // Convert to Base64 GBK string
        val base64GBK = Base64.getEncoder().encodeToString(encryptedBytes)

        // RSA encryption
        cipher = Cipher.getInstance(RSA)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        encryptedBytes = cipher.doFinal(base64GBK.toByteArray(StandardCharsets.UTF_8))

        // Convert to Base64 UTF-8 string
        val base64UTF8 = Base64.getEncoder().encodeToString(encryptedBytes)

        // Convert to Chinese
        return bytesToChinese(base64UTF8.toByteArray(StandardCharsets.UTF_8))
    }

    @Throws(Exception::class)
    fun decrypt(chinese: String, privateKey: PrivateKey?): String {
        // Convert from Chinese
        val base64UTF8 = String(chineseToBytes(chinese), StandardCharsets.UTF_8)

        // RSA decryption
        var cipher = Cipher.getInstance(RSA)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        var decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(base64UTF8))

        // Convert from Base64 UTF-8 string
        val base64GBK = String(decryptedBytes!!, StandardCharsets.UTF_8)

        // AES decryption
        val key: Key = SecretKeySpec(SECRET_KEY.toByteArray(charset(CHARSET_GBK)), AES)
        cipher = Cipher.getInstance(AES)
        cipher.init(Cipher.DECRYPT_MODE, key)
        decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(base64GBK))

        // Convert from Base64 GBK string
        return String(decryptedBytes, charset(CHARSET_GBK))
    }
}

fun main(){
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048)
    val keyPair = keyPairGenerator.generateKeyPair()
    val publicKey = keyPair.public
    val privateKey = keyPair.private

    val originalStr = "012345678910\n你好"
    val encrypted = ChineseUtils.encrypt(originalStr, publicKey)
    println("加密结果:$encrypted")
    val decrypted = ChineseUtils.decrypt(encrypted, privateKey)
    println("解密结果:$decrypted")
}
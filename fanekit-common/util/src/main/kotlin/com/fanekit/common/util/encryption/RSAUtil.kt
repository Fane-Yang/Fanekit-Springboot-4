package com.fanekit.common.util.encryption

import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.*
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher

/**
 * RSA加密工具类
 * 提供生成RSA密钥对、加载私钥和公钥、加密解密、数字签名验签的功能
 */
class RSAUtil {
    /**
     * RSA密钥长度枚举
     */
    enum class KeySize(val bits: Int) {
        RSA_1024(1024),
        RSA_2048(2048),
        RSA_3072(3072),
        RSA_4096(4096)
    }
    /**
     * 签名算法枚举
     */
    enum class SignatureAlgorithm(val algorithm: String) {
        SHA1_WITH_RSA("SHA1withRSA"),
        SHA256_WITH_RSA("SHA256withRSA"),
        SHA384_WITH_RSA("SHA384withRSA"),
        SHA512_WITH_RSA("SHA512withRSA")
    }
    companion object {
        // RSA加密算法
        private const val RSA_ALGORITHM = "RSA"
        // RSA/ECB/PKCS1Padding 用于加密解密
        private const val RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding"

        /**
         * 生成RSA密钥对并返回Base64编码的字符串
         * @param keySize 密钥长度，默认2048位
         * @return Pair<PublicKey, PrivateKey> Base64编码的公钥和私钥
         */
        fun generateRsaKeyPairBase64(keySize: KeySize = KeySize.RSA_2048): Pair<String, String> {
            val keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM)
            keyGen.initialize(keySize.bits)
            val keyPair = keyGen.generateKeyPair()

            val publicKey = keyPair.public.encoded
            val privateKey = keyPair.private.encoded

            val pubBase64 = Base64.getEncoder().encodeToString(publicKey)
            val priBase64 = Base64.getEncoder().encodeToString(privateKey)

            return pubBase64 to priBase64
        }

        /**
         * 生成RSA密钥对
         * @param keySize 密钥长度，默认2048位
         * @return KeyPair 密钥对对象
         */
        fun generateRsaKeyPair(keySize: KeySize = KeySize.RSA_2048): KeyPair {
            val keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM)
            keyGen.initialize(keySize.bits)
            return keyGen.generateKeyPair()
        }

        /**
         * 从Base64字符串加载RSA私钥
         */
        fun loadRsaPrivateKeyFromBase64(base64: String): RSAPrivateKey {
            val keyBytes = Base64.getDecoder().decode(base64)
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            return KeyFactory.getInstance(RSA_ALGORITHM).generatePrivate(keySpec) as RSAPrivateKey
        }

        /**
         * 从Base64字符串加载RSA公钥
         */
        fun loadRsaPublicKeyFromBase64(base64: String): RSAPublicKey {
            val keyBytes = Base64.getDecoder().decode(base64)
            val keySpec = X509EncodedKeySpec(keyBytes)
            return KeyFactory.getInstance(RSA_ALGORITHM).generatePublic(keySpec) as RSAPublicKey
        }

        /**
         * 使用公钥加密数据
         * @param data 要加密的数据
         * @param publicKey 公钥
         * @return 加密后的数据（Base64编码）
         */
        fun encrypt(data: String, publicKey: RSAPublicKey): String {
            val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(encryptedBytes)
        }

        /**
         * 使用公钥加密数据（使用Base64编码的公钥）
         * @param data 要加密的数据
         * @param publicKeyBase64 Base64编码的公钥
         * @return 加密后的数据（Base64编码）
         */
        fun encrypt(data: String, publicKeyBase64: String): String {
            val publicKey = loadRsaPublicKeyFromBase64(publicKeyBase64)
            return encrypt(data, publicKey)
        }

        /**
         * 使用私钥解密数据
         * @param encryptedData Base64编码的加密数据
         * @param privateKey 私钥
         * @return 解密后的原始数据
         */
        fun decrypt(encryptedData: String, privateKey: RSAPrivateKey): String {
            val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            val encryptedBytes = Base64.getDecoder().decode(encryptedData)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        }

        /**
         * 使用私钥解密数据（使用Base64编码的私钥）
         * @param encryptedData Base64编码的加密数据
         * @param privateKeyBase64 Base64编码的私钥
         * @return 解密后的原始数据
         */
        fun decrypt(encryptedData: String, privateKeyBase64: String): String {
            val privateKey = loadRsaPrivateKeyFromBase64(privateKeyBase64)
            return decrypt(encryptedData, privateKey)
        }

        /**
         * 使用私钥对数据进行数字签名
         * @param data 要签名的数据
         * @param privateKey 私钥
         * @param algorithm 签名算法，默认SHA256withRSA
         * @return Base64编码的签名
         */
        fun sign(data: String, privateKey: RSAPrivateKey, algorithm: SignatureAlgorithm = SignatureAlgorithm.SHA256_WITH_RSA): String {
            val signature = Signature.getInstance(algorithm.algorithm)
            signature.initSign(privateKey)
            signature.update(data.toByteArray(Charsets.UTF_8))
            val signBytes = signature.sign()
            return Base64.getEncoder().encodeToString(signBytes)
        }

        /**
         * 使用私钥对数据进行数字签名（使用Base64编码的私钥）
         * @param data 要签名的数据
         * @param privateKeyBase64 Base64编码的私钥
         * @param algorithm 签名算法，默认SHA256withRSA
         * @return Base64编码的签名
         */
        fun sign(data: String, privateKeyBase64: String, algorithm: SignatureAlgorithm = SignatureAlgorithm.SHA256_WITH_RSA): String {
            val privateKey = loadRsaPrivateKeyFromBase64(privateKeyBase64)
            return sign(data, privateKey, algorithm)
        }

        /**
         * 使用公钥验证数字签名
         * @param data 原始数据
         * @param signatureBase64 Base64编码的签名
         * @param publicKey 公钥
         * @param algorithm 签名算法，默认SHA256withRSA
         * @return 验证结果
         */
        fun verify(data: String, signatureBase64: String, publicKey: RSAPublicKey, algorithm: SignatureAlgorithm = SignatureAlgorithm.SHA256_WITH_RSA): Boolean {
            return try {
                val signature = Signature.getInstance(algorithm.algorithm)
                signature.initVerify(publicKey)
                signature.update(data.toByteArray(Charsets.UTF_8))
                val signBytes = Base64.getDecoder().decode(signatureBase64)
                signature.verify(signBytes)
            } catch (e: Exception) {
                false
            }
        }

        /**
         * 使用公钥验证数字签名（使用Base64编码的公钥）
         * @param data 原始数据
         * @param signatureBase64 Base64编码的签名
         * @param publicKeyBase64 Base64编码的公钥
         * @param algorithm 签名算法，默认SHA256withRSA
         * @return 验证结果
         */
        fun verify(data: String, signatureBase64: String, publicKeyBase64: String, algorithm: SignatureAlgorithm = SignatureAlgorithm.SHA256_WITH_RSA): Boolean {
            return try {
                val publicKey = loadRsaPublicKeyFromBase64(publicKeyBase64)
                verify(data, signatureBase64, publicKey, algorithm)
            } catch (e: Exception) {
                false
            }
        }

        /**
         * 获取密钥的信息
         * @param publicKey 公钥
         * @return 密钥信息字符串
         */
        fun getKeyInfo(publicKey: RSAPublicKey): String {
            return buildString {
                append("RSA Public Key Info:\n")
                append("Algorithm: ${publicKey.algorithm}\n")
                append("Format: ${publicKey.format}\n")
                append("Key Size: ${publicKey.modulus.bitLength()} bits\n")
                append("Modulus: ${publicKey.modulus}\n")
                append("Public Exponent: ${publicKey.publicExponent}")
            }
        }

        /**
         * 获取密钥的信息
         * @param privateKey 私钥
         * @return 密钥信息字符串
         */
        fun getKeyInfo(privateKey: RSAPrivateKey): String {
            return buildString {
                append("RSA Private Key Info:\n")
                append("Algorithm: ${privateKey.algorithm}\n")
                append("Format: ${privateKey.format}\n")
                append("Key Size: ${privateKey.modulus.bitLength()} bits\n")
                append("Modulus: ${privateKey.modulus}\n")
                append("Private Exponent: [PROTECTED]")
            }
        }

        /**
         * 将公钥转换为PEM格式
         * @param publicKey 公钥
         * @return PEM格式的公钥字符串
         */
        fun publicKeyToPem(publicKey: RSAPublicKey): String {
            val encoded = Base64.getEncoder().encodeToString(publicKey.encoded)
            return buildString {
                append("-----BEGIN PUBLIC KEY-----\n")
                // 每64个字符换行
                encoded.chunked(64).forEach { line ->
                    append(line)
                    append("\n")
                }
                append("-----END PUBLIC KEY-----")
            }
        }

        /**
         * 将私钥转换为PEM格式
         * @param privateKey 私钥
         * @return PEM格式的私钥字符串
         */
        fun privateKeyToPem(privateKey: RSAPrivateKey): String {
            val encoded = Base64.getEncoder().encodeToString(privateKey.encoded)
            return buildString {
                append("-----BEGIN PRIVATE KEY-----\n")
                // 每64个字符换行
                encoded.chunked(64).forEach { line ->
                    append(line)
                    append("\n")
                }
                append("-----END PRIVATE KEY-----")
            }
        }

        /**
         * 从PEM格式加载公钥
         * @param pemPublicKey PEM格式的公钥字符串
         * @return RSA公钥
         */
        fun loadPublicKeyFromPem(pemPublicKey: String): RSAPublicKey {
            val publicKeyPEM = pemPublicKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\\s".toRegex(), "")

            return loadRsaPublicKeyFromBase64(publicKeyPEM)
        }

        /**
         * 从PEM格式加载私钥
         * @param pemPrivateKey PEM格式的私钥字符串
         * @return RSA私钥
         */
        fun loadPrivateKeyFromPem(pemPrivateKey: String): RSAPrivateKey {
            val privateKeyPEM = pemPrivateKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\s".toRegex(), "")

            return loadRsaPrivateKeyFromBase64(privateKeyPEM)
        }

        /**
         * 从Base64字符串加载ECDSA公钥
         */
        fun loadEcdsaPublicKeyFromBase64(base64: String): ECPublicKey {
            val keyBytes = Base64.getDecoder().decode(base64)
            val keySpec = X509EncodedKeySpec(keyBytes)
            return KeyFactory.getInstance("EC").generatePublic(keySpec) as ECPublicKey
        }

        /**
         * 从Base64字符串加载ECDSA私钥
         */
        fun loadEcdsaPrivateKeyFromBase64(base64: String): ECPrivateKey {
            val keyBytes = Base64.getDecoder().decode(base64)
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            return KeyFactory.getInstance("EC").generatePrivate(keySpec) as ECPrivateKey
        }
    }
}
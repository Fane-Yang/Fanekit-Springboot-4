package com.fanekit.common.util.encryption

import org.bouncycastle.crypto.engines.SM2Engine
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.params.ParametersWithRandom
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

/**
 * 非对称加密和签名：SM2
 * 实现了bouncycastle方式加解密
 * 默认情况下字符串相关的都会作为UTF-8编码加解密，
 * 如果需要使用其他编码形式，需要使用 ByteToByte方法，自定义编码即可
 */
object SM2Utils {
    // 下面是采用bouncycastle的加解密方法
    @Throws(Exception::class)
    fun generateSM2KeyByBouncyCastle(): Map<String, String> {
        var keyPairGenerator: KeyPairGenerator? = null
        val secureRandom = SecureRandom()
        val sm2Spec = ECGenParameterSpec("sm2p256v1")
        keyPairGenerator = KeyPairGenerator.getInstance("EC", BouncyCastleProvider())
        keyPairGenerator.initialize(sm2Spec)
        keyPairGenerator.initialize(sm2Spec, secureRandom)
        val keyPair = keyPairGenerator.generateKeyPair()
        val privateKey = keyPair.private
        val publicKey = keyPair.public
        val publicKeyStr = Base64Utils.encode(publicKey.encoded)
        val privateKeyStr = Base64Utils.encode(privateKey.encoded)
        println("私钥:$privateKeyStr")
        println("公钥:$publicKeyStr")
        println()
        val map: MutableMap<String, String> = HashMap()
        map["publicKey"] = publicKeyStr
        map["privateKey"] = privateKeyStr
        return map
    }

    @Throws(Exception::class)
    fun getPrivateKeyByBase64(privateKeyStr: String): PrivateKey {
        val keyBytes = Base64Utils.decode(privateKeyStr)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider())
        return keyFactory.generatePrivate(keySpec)
    }

    @Throws(Exception::class)
    fun getPublicKeyByBase64(publicKeyStr: String): PublicKey {
        val keyBytes = Base64Utils.decode(publicKeyStr)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider())
        return keyFactory.generatePublic(keySpec)
    }

    private fun getEcPublicKeyParameters(publicKey: PublicKey): ECPublicKeyParameters? {
        val sm2PubK = publicKey as BCECPublicKey
        var localECPublicKeyParameters: ECPublicKeyParameters? = null
        val localECParameterSpec = sm2PubK.parameters
        val localECDomainParameters = ECDomainParameters(
            localECParameterSpec.curve,
            localECParameterSpec.g, localECParameterSpec.n
        )
        localECPublicKeyParameters = ECPublicKeyParameters(sm2PubK.q, localECDomainParameters)

        return localECPublicKeyParameters
    }

    /**
     * 根据publicKey对原始数据data，使用SM2加密
     */
    fun encryptByteToByteByBouncyCastle(data: ByteArray, publicKey: String): ByteArray? {
        try {
            val pubKey = getPublicKeyByBase64(publicKey)
            val localECPublicKeyParameters =
                getEcPublicKeyParameters(pubKey)
            val localSM2Engine = SM2Engine()
            localSM2Engine.init(true, ParametersWithRandom(localECPublicKeyParameters, SecureRandom()))
            val arrayOfByte2 = localSM2Engine.processBlock(data, 0, data.size)
            return arrayOfByte2
        } catch (e: Exception) {
            println("加密失败:" + e.message)
            return null
        }
    }

    fun encryptByteToHexStrByBouncyCastle(data: ByteArray, publicKey: String): String {
        return HexUtils.encode(
            encryptByteToByteByBouncyCastle(
                data,
                publicKey
            )!!
        )
    }

    fun encryptByteToBase64ByBouncyCastle(data: ByteArray, publicKey: String): String? {
        return Base64Utils.encode(
            encryptByteToByteByBouncyCastle(
                data,
                publicKey
            )!!
        )
    }

    fun encryptStrToByteByBouncyCastle(data: String, publicKey: String): ByteArray? {
        return encryptByteToByteByBouncyCastle(
            data.toByteArray(
                StandardCharsets.UTF_8
            ), publicKey
        )
    }

    fun encryptStrToHexStrByBouncyCastle(data: String, publicKey: String): String {
        return HexUtils.encode(
            encryptStrToByteByBouncyCastle(
                data,
                publicKey
            )!!
        )
    }

    fun encryptStrToBase64ByBouncyCastle(data: String, publicKey: String): String? {
        return Base64Utils.encode(
            encryptStrToByteByBouncyCastle(
                data,
                publicKey
            )!!
        )
    }

    @Throws(IOException::class)
    fun encryptFileToFileByBouncyCastle(sourceFilePath: String, targetFilePath: String, publicKey: String) {
        val fileBytes = Files.readAllBytes(Paths.get(sourceFilePath))
        val encryptedBytes =
            encryptByteToByteByBouncyCastle(fileBytes, publicKey)
        Files.write(Paths.get(targetFilePath), encryptedBytes)
    }

    /**
     * 根据privateKey对加密数据encode data，使用SM2解密
     */
    fun decryptByteToByteByBouncyCastle(encodeData: ByteArray, privateKey: String): ByteArray? {
        try {
            val priKey = getPrivateKeyByBase64(privateKey)
            val sm2PriK = priKey as BCECPrivateKey
            val localSM2Engine = SM2Engine()
            val localECParameterSpec = sm2PriK.parameters
            val localECDomainParameters = ECDomainParameters(
                localECParameterSpec.curve,
                localECParameterSpec.g, localECParameterSpec.n
            )
            val localECPrivateKeyParameters = ECPrivateKeyParameters(
                sm2PriK.d,
                localECDomainParameters
            )
            localSM2Engine.init(false, localECPrivateKeyParameters)
            return localSM2Engine.processBlock(encodeData, 0, encodeData.size)
        } catch (e: Exception) {
            println("Decryption error: $e")
            return null
        }
    }

    fun decryptByteToStrByBouncyCastle(encodeData: ByteArray, privateKey: String): String {
        return String(
            decryptByteToByteByBouncyCastle(
                encodeData,
                privateKey
            )!!, StandardCharsets.UTF_8)
    }

    fun decryptHexStrToByteByBouncyCastle(encodeData: String?, privateKey: String): ByteArray? {
        return decryptByteToByteByBouncyCastle(
            HexUtils.decode(
                encodeData!!
            ), privateKey
        )
    }

    fun decryptHexStrToStrByBouncyCastle(encodeData: String?, privateKey: String): String {
        return String(
            decryptHexStrToByteByBouncyCastle(
                encodeData,
                privateKey
            )!!, StandardCharsets.UTF_8)
    }

    fun decryptBase64ToByteByBouncyCastle(encodeData: String?, privateKey: String): ByteArray? {
        return decryptByteToByteByBouncyCastle(
            Base64Utils.decode(
                encodeData!!
            ), privateKey
        )
    }

    fun decryptBase64ToStrByBouncyCastle(encodeData: String?, privateKey: String): String {
        return String(
            decryptBase64ToByteByBouncyCastle(
                encodeData,
                privateKey
            )!!, StandardCharsets.UTF_8)
    }

    @Throws(IOException::class)
    fun decryptFileToFileByBouncyCastle(sourceFilePath: String, targetFilePath: String, privateKey: String) {
        val fileBytes = Files.readAllBytes(Paths.get(sourceFilePath))
        val decryptedBytes =
            decryptByteToByteByBouncyCastle(fileBytes, privateKey)
        Files.write(Paths.get(targetFilePath), decryptedBytes)
    }

}
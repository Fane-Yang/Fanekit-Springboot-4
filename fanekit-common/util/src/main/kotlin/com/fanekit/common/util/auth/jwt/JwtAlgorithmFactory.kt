package com.fanekit.common.util.auth.jwt

import com.auth0.jwt.algorithms.Algorithm
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

/**
 * JWT相关算法辅助工具类
 * TODO 修改为工厂类，不返回 Algorithm 实例，而是返回一个封装了算法的对象直接传递给配置类
 */
class JwtAlgorithmFactory {
    companion object {
        fun hmac256(secret: String): Algorithm = Algorithm.HMAC256(secret)
        fun hmac384(secret: String): Algorithm = Algorithm.HMAC384(secret)
        fun hmac512(secret: String): Algorithm = Algorithm.HMAC512(secret)

        fun rsa256(publicKey: RSAPublicKey, privateKey: RSAPrivateKey): Algorithm = Algorithm.RSA256(publicKey, privateKey)
        fun rsa512(publicKey: RSAPublicKey, privateKey: RSAPrivateKey): Algorithm = Algorithm.RSA512(publicKey, privateKey)

        fun ecdsa256(publicKey: ECPublicKey, privateKey: ECPrivateKey): Algorithm = Algorithm.ECDSA256(publicKey, privateKey)

    }
}
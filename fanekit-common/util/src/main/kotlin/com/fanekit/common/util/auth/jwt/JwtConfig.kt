package com.fanekit.common.util.auth.jwt

import com.auth0.jwt.algorithms.Algorithm
import com.fanekit.common.util.encryption.RSAUtil
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

/**
 * JWT 配置类
 * 用于存储 JWT 相关的配置信息，如发行者、受众、过期时间和算法等。
 *
 * @property issuer JWT 的发行者
 * @property audience JWT 的受众
 * @property expiresInMs JWT 的过期时间，单位为毫秒，默认为 1 天
 * @property algorithm JWT 使用的加密算法
 */
data class JwtConfig(
    val issuer: String,
    val audience: String,
    val expiresInMs: Long = 24 * 60 * 60 * 1000, // 1 day
    internal val algorithm: Algorithm
) {
    companion object {
        /**
         * 使用 HMAC256 算法创建 JWT 配置
         */
        fun withHmac256(
            issuer: String,
            audience: String,
            secret: String,
            expiresInMs: Long = 24 * 60 * 60 * 1000
        ): JwtConfig = JwtConfig(issuer, audience, expiresInMs, Algorithm.HMAC256(secret))

        /**
         * 使用 HMAC384 算法创建 JWT 配置
         */
        fun withHmac384(
            issuer: String,
            audience: String,
            secret: String,
            expiresInMs: Long = 24 * 60 * 60 * 1000
        ): JwtConfig = JwtConfig(issuer, audience, expiresInMs, Algorithm.HMAC384(secret))

        /**
         * 使用 HMAC512 算法创建 JWT 配置
         */
        fun withHmac512(
            issuer: String,
            audience: String,
            secret: String,
            expiresInMs: Long = 24 * 60 * 60 * 1000
        ): JwtConfig = JwtConfig(issuer, audience, expiresInMs, Algorithm.HMAC512(secret))

        /**
         * 使用 RSA256 算法创建 JWT 配置
         */
        fun withRsa256(
            issuer: String,
            audience: String,
            publicKey: RSAPublicKey,
            privateKey: RSAPrivateKey,
            expiresInMs: Long = 24 * 60 * 60 * 1000
        ): JwtConfig = JwtConfig(issuer, audience, expiresInMs, Algorithm.RSA256(publicKey, privateKey))

        /**
         * 使用 Base64 编码的 RSA256 公私钥创建 JWT 配置
         */
        fun withRsa256(
            issuer: String,
            audience: String,
            publicKeyBase64: String,
            privateKeyBase64: String,
            expiresInMs: Long = 24 * 60 * 60 * 1000
        ): JwtConfig {
            val publicKey = RSAUtil.loadRsaPublicKeyFromBase64(publicKeyBase64)
            val privateKey = RSAUtil.loadRsaPrivateKeyFromBase64(privateKeyBase64)
            return JwtConfig(issuer, audience, expiresInMs, Algorithm.RSA256(publicKey, privateKey))
        }

        /**
         * 使用 RSA512 算法创建 JWT 配置
         */
        fun withRsa512(
            issuer: String,
            audience: String,
            publicKey: RSAPublicKey,
            privateKey: RSAPrivateKey,
            expiresInMs: Long = 24 * 60 * 60 * 1000
        ): JwtConfig = JwtConfig(issuer, audience, expiresInMs, Algorithm.RSA512(publicKey, privateKey))

        /**
         * 使用 Base64 编码的 RSA512 公私钥创建 JWT 配置
         */
        fun withRsa512(
            issuer: String,
            audience: String,
            publicKeyBase64: String,
            privateKeyBase64: String,
            expiresInMs: Long = 24 * 60 * 60 * 1000
        ): JwtConfig {
            val publicKey = RSAUtil.loadRsaPublicKeyFromBase64(publicKeyBase64)
            val privateKey =  RSAUtil.loadRsaPrivateKeyFromBase64(privateKeyBase64)
            return JwtConfig(issuer, audience, expiresInMs, Algorithm.RSA512(publicKey, privateKey))
        }

        /**
         * 使用 ECDSA256 算法创建 JWT 配置
         */
        fun withEcdsa256(
            issuer: String,
            audience: String,
            publicKey: ECPublicKey,
            privateKey: ECPrivateKey,
            expiresInMs: Long = 24 * 60 * 60 * 1000
        ): JwtConfig = JwtConfig(issuer, audience, expiresInMs, Algorithm.ECDSA256(publicKey, privateKey))

        /**
         * 使用 Base64 编码的 ECDSA256 公私钥创建 JWT 配置
         */
        fun withEcdsa256(
            issuer: String,
            audience: String,
            publicKeyBase64: String,
            privateKeyBase64: String,
            expiresInMs: Long = 24 * 60 * 60 * 1000
        ): JwtConfig {
            val publicKey = RSAUtil.loadEcdsaPublicKeyFromBase64(publicKeyBase64)
            val privateKey = RSAUtil.loadEcdsaPrivateKeyFromBase64(privateKeyBase64)
            return JwtConfig(issuer, audience, expiresInMs, Algorithm.ECDSA256(publicKey, privateKey))
        }
    }
}

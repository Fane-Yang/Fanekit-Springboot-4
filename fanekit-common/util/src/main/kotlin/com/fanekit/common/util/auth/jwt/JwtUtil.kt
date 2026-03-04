package com.fanekit.common.util.auth.jwt


import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import java.util.*

class JwtUtil(private val config: JwtConfig) {

    private val verifier: JWTVerifier by lazy {
        JWT.require(config.algorithm)
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .build()
    }
            /**
     * 业务侧常用的封装：直接传 uid / roles / permissions，
     * 内部转换为 payload 后复用下方 generateToken(payload)。
     *
     * @param uid      必填，写入 claim "uid"
     * @param roles       可选，非空时写入 claim "roles"（字符串数组）
     * @param permissions 可选，非空时写入 claim "permissions"（字符串数组）
     * @param extra       额外自定义字段，可覆盖同名默认键
     */
    fun generateToken(
        uid: String = "",
        roles: List<String> = emptyList(),
        permissions: List<String> = emptyList(),
        extra: Map<String, Any> = emptyMap()
    ): String {
        // 1️⃣ 构造 payload
        val payload = mutableMapOf<String, Any>(
            "uid" to uid
        ).apply {
            if (roles.isNotEmpty()) put("roles", roles)
            if (permissions.isNotEmpty()) put("perms", permissions)
            putAll(extra)                    // 允许传入额外 Claim
        }

        // 2️⃣ 复用已有实现
        return generateToken(payload)
    }

    fun generateToken(payload: Map<String, Any>): String {
        val now = System.currentTimeMillis()
        val builder = JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + config.expiresInMs))

        payload.forEach { (k, v) ->
            when (v) {
                is String -> builder.withClaim(k, v)
                is Int -> builder.withClaim(k, v)
                is Long -> builder.withClaim(k, v)
                is Boolean -> builder.withClaim(k, v)
                is List<*> -> if (v.all { it is String }) {
                    builder.withArrayClaim(k, v.map { it as String }.toTypedArray())
                }
                else -> {} // 不支持的忽略
            }
        }

        return builder.sign(config.algorithm)
    }

    fun verifyToken(token: String): DecodedJWT {
        return verifier.verify(token)
    }

    fun decodeToken(token: String): DecodedJWT {
        return JWT.decode(token)
    }
    /** 供 Ktor Authentication 直接使用 */
    fun verifier(): JWTVerifier = verifier

}


//fun main() {
//    // 初始化配置
//    val secret = "test-secret"
//    val algorithm: Algorithm = JwtAlgorithmFactory.hmac256(secret)
//
//    JwtUtil.init(
//        JwtUtil.Config(
//            issuer = "test-issuer",
//            audience = "test-audience",
//            expiresInMs = 3600_000,
//            algorithm = algorithm
//        )
//    )
//
//    // 构造 payload
//    val payload = mapOf(
//        "userId" to "1001",
//        "username" to "testUser",
//        "roles" to listOf("admin", "user")
//    )
//
//    // 生成 token
//    val token1 = JwtUtil.generateToken(payload)
//    println("✅ 生成的 JWT Token：\n$token1\n")
//
//    // 验证 token
//    try {
//        val jwt: DecodedJWT = JwtUtil.verifyToken(token1)
//        println("✅ 解析成功：")
//        println("userId: ${jwt.getClaim("userId").asString()}")
//        println("username: ${jwt.getClaim("username").asString()}")
//        println("roles: ${jwt.getClaim("roles").asList(String::class.java)}")
//        println("issuer: ${jwt.issuer}")
//        println("audience: ${jwt.audience}")
//        println("expiresAt: ${jwt.expiresAt}")
//    } catch (e: Exception) {
//        println("❌ 验证失败: ${e.message}")
//    }
//
//    // 1. 生成 Base64 编码的 RSA 密钥对
//    val (publicBase64, privateBase64) = RSAUtil.generateRsaKeyPairBase64()
//
//    println("🔐 PublicKey (Base64):\n$publicBase64\n")
//    println("🔐 PrivateKey (Base64):\n$privateBase64\n")
//
//    // 2. 从 Base64 解码为 Key 对象
//    val publicKey = RSAUtil.loadRsaPublicKeyFromBase64(publicBase64)
//    val privateKey = RSAUtil.loadRsaPrivateKeyFromBase64(privateBase64)
//
//    // 3. 初始化 JwtUtil（RSA 模式）
//    JwtUtil.init(
//        JwtConfig(
//            issuer = "base64-issuer",
//            audience = "base64-audience",
//            algorithm = JwtAlgorithmFactory.rsa256(publicKey, privateKey)
//        )
//    )
//
//    // 4. 生成 token
//    val token = JwtUtil.generateToken(
//        mapOf("userId" to "5005", "roles" to listOf("manager", "guest"))
//    )
//    println("🧾 生成的 JWT Token:\n$token\n")
//
//    // 5. 验证 token
//    try {
//        val decoded: DecodedJWT = JwtUtil.verifyToken(token)
//        println("✅ 验证成功 userId = ${decoded.getClaim("userId").asString()}")
//        println("roles: ${decoded.getClaim("roles").asList(String::class.java)}")
//    } catch (e: Exception) {
//        println("❌ 验证失败: ${e.message}")
//    }
//}
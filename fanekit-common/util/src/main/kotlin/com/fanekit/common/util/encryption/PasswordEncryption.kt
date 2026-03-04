package com.fanekit.common.util.encryption

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.nio.charset.StandardCharsets


class PasswordEncryption {
    companion object {
        /**
         * Argon2id密码加密算法
         */
        fun argon2idHash(
            passStr: String,
            saltStr: String,
            memorySize: Int = 4096, // 内存使用量：4MB
            parallelism: Int = 2, // 并行度：2
            iterations: Int = 3// 迭代次数：3
        ): String {
            val password = passStr.toByteArray(StandardCharsets.UTF_8)
            val salt = saltStr.toByteArray(StandardCharsets.UTF_8)

            val builder = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withMemoryAsKB(memorySize)
                .withParallelism(parallelism)
                .withIterations(iterations)

            val generator = Argon2BytesGenerator()
            generator.init(builder.build())

            val hash = ByteArray(32) // 输出哈希：32字节
            generator.generateBytes(password, hash)
//            val result = HexUtil.encodeHexStr(hash)
            val result = HexUtils.encode(hash)
//            println(result)
            return result
        }
    }
}
fun main() {
    // 每个用户创建时生成一个salt值用于对密码进行hash，此后先从数据库查出用户名，和salt，然后根据这两个值进行hash，最后比较hash结果
    // 安全起见前端也不能传明文密码过来，前端应该使用公钥加密文本后传入加密的数据，后端先使用私钥解密。
    PasswordEncryption.argon2idHash(
        "46fd0cfe7f6bab71ccb2",
        "3d4ba0b0a40d2fd00"
    )
}
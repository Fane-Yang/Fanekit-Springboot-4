package com.fanekit.common.util.encryption

/**
 * RSAUtil使用示例
 * 演示各种RSA加密、解密、签名、验签的使用场景
 */
fun main() {
    println("=== RSA工具类使用示例 ===\n")

    // 1. 生成不同长度的密钥对
    println("1. 生成不同长度的密钥对")
    println("=" * 50)

    // 生成2048位密钥对（推荐用于生产环境）
    val (publicKey2048, privateKey2048) = RSAUtil.generateRsaKeyPairBase64(RSAUtil.KeySize.RSA_2048)
    println("✓ 生成2048位密钥对成功")

    // 生成4096位密钥对（高安全性要求）
    val (publicKey4096, privateKey4096) = RSAUtil.generateRsaKeyPairBase64(RSAUtil.KeySize.RSA_4096)
    println("✓ 生成4096位密钥对成功")

    // 2. 基本加密解密示例
    println("\n2. 基本加密解密示例")
    println("=" * 50)

    val originalText = "这是需要加密的敏感信息：用户密码123456"
    println("原始文本: $originalText")

    // 使用公钥加密
    val encryptedText = RSAUtil.encrypt(originalText, publicKey2048)
    println("加密后: ${encryptedText.substring(0, 50)}...")

    // 使用私钥解密
    val decryptedText = RSAUtil.decrypt(encryptedText, privateKey2048)
    println("解密后: $decryptedText")
    println("✓ 加密解密验证成功: ${originalText == decryptedText}")

    // 3. 数字签名和验签示例
    println("\n3. 数字签名和验签示例")
    println("=" * 50)

    val documentContent = "这是一份重要的合同文档内容"
    println("文档内容: $documentContent")

    // 使用私钥签名（不同算法）
    val signatureSHA256 = RSAUtil.sign(documentContent, privateKey2048, RSAUtil.SignatureAlgorithm.SHA256_WITH_RSA)
    val signatureSHA512 = RSAUtil.sign(documentContent, privateKey2048, RSAUtil.SignatureAlgorithm.SHA512_WITH_RSA)

    println("SHA256签名: ${signatureSHA256.substring(0, 50)}...")
    println("SHA512签名: ${signatureSHA512.substring(0, 50)}...")

    // 使用公钥验签
    val isValidSHA256 =
        RSAUtil.verify(documentContent, signatureSHA256, publicKey2048, RSAUtil.SignatureAlgorithm.SHA256_WITH_RSA)
    val isValidSHA512 =
        RSAUtil.verify(documentContent, signatureSHA512, publicKey2048, RSAUtil.SignatureAlgorithm.SHA512_WITH_RSA)

    println("✓ SHA256签名验证: $isValidSHA256")
    println("✓ SHA512签名验证: $isValidSHA512")

    // 验证篡改检测
    val tamperedContent = "这是一份被篡改的合同文档内容"
    val isTamperedValid = RSAUtil.verify(tamperedContent, signatureSHA256, publicKey2048)
    println("✓ 篡改内容验证: $isTamperedValid (应该为false)")

    // 4. PEM格式转换示例
    println("\n4. PEM格式转换示例")
    println("=" * 50)

    val publicKeyObj = RSAUtil.loadRsaPublicKeyFromBase64(publicKey2048)
    val privateKeyObj = RSAUtil.loadRsaPrivateKeyFromBase64(privateKey2048)

    val publicKeyPem = RSAUtil.publicKeyToPem(publicKeyObj)
    val privateKeyPem = RSAUtil.privateKeyToPem(privateKeyObj)

    println("公钥PEM格式:")
    println(publicKeyPem.substring(0, 200) + "...")

    println("\n私钥PEM格式:")
    println(privateKeyPem.substring(0, 200) + "...")

    // 从PEM格式加载回来验证
    val loadedPublicKey = RSAUtil.loadPublicKeyFromPem(publicKeyPem)
    val testEncryption = RSAUtil.encrypt("PEM测试", loadedPublicKey)
    val testDecryption = RSAUtil.decrypt(testEncryption, privateKeyObj)
    println("✓ PEM格式加载验证: ${testDecryption == "PEM测试"}")

    // 5. 密钥信息查看
    println("\n5. 密钥信息查看")
    println("=" * 50)

    val keyInfo = RSAUtil.getKeyInfo(publicKeyObj)
    println(keyInfo)

    // 6. 实际应用场景示例
    println("\n6. 实际应用场景示例")
    println("=" * 50)

    // 场景1: API接口数据加密传输
    println("场景1: API接口数据加密传输")
    val apiData = """{"userId": 12345, "password": "secret123", "email": "user@example.com"}"""
    val encryptedApiData = RSAUtil.encrypt(apiData, publicKey2048)
    println("API数据加密成功，长度: ${encryptedApiData.length}")

    // 场景2: 数字合同签名
    println("\n场景2: 数字合同签名")
    val contract = "甲方：XXX公司\n乙方：YYY公司\n合同金额：1000000元\n签署日期：2025-01-19"
    val contractSignature = RSAUtil.sign(contract, privateKey2048)
    val isContractValid = RSAUtil.verify(contract, contractSignature, publicKey2048)
    println("合同签名验证: $isContractValid")

    // 场景3: 许可证验证
    println("\n场景3: 软件许可证验证")
    val licenseInfo = "产品：RSA工具\n用户：测试用户\n过期时间：2025-12-31\n功能：完整版"
    val licenseSignature = RSAUtil.sign(licenseInfo, privateKey2048)
    val isLicenseValid = RSAUtil.verify(licenseInfo, licenseSignature, publicKey2048)
    println("许可证验证: $isLicenseValid")

    // 7. 性能测试示例
    println("\n7. 性能测试示例")
    println("=" * 50)

    val performanceText = "性能测试数据"
    RSAUtil.KeySize.values().forEach { keySize ->
        val startTime = System.currentTimeMillis()

        // 生成密钥
        val keyPair = RSAUtil.generateRsaKeyPairBase64(keySize)
        val keyGenTime = System.currentTimeMillis() - startTime

        // 加密解密
        val cryptoStart = System.currentTimeMillis()
        val encrypted = RSAUtil.encrypt(performanceText, keyPair.first)
        val decrypted = RSAUtil.decrypt(encrypted, keyPair.second)
        val cryptoTime = System.currentTimeMillis() - cryptoStart

        println("${keySize.name}: 密钥生成=${keyGenTime}ms, 加密解密=${cryptoTime}ms")
    }

    println("\n=== RSA工具类示例演示完成 ===")
}

/**
 * 扩展函数：重复字符
 */
private operator fun String.times(n: Int): String = this.repeat(n)

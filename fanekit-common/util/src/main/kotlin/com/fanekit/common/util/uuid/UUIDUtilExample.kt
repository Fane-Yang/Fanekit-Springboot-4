package com.fanekit.common.util.uuid

/**
 * UUIDUtil使用示例
 * 展示各种UUID生成、验证、转换的使用场景
 */
fun main() {
    println("=== UUIDUtil 使用示例 ===\n")

    // 1. 基本UUID生成
    println("1. 基本UUID生成")
    println("=" * 50)

    val standardUuid = UUIDUtils.randomUUID()
    val simpleUuid = UUIDUtils.randomSimpleUUID()

    println("标准格式UUID: $standardUuid")
    println("简单格式UUID: $simpleUuid")

    // 2. 不同格式的UUID生成
    println("\n2. 不同格式的UUID生成")
    println("=" * 50)

    UUIDUtils.UUIDFormat.values().forEach { format ->
        val uuid = UUIDUtils.randomUUID(format)
        println("${format.name.padEnd(20)}: $uuid")
    }

    // 3. 批量UUID生成
    println("\n3. 批量UUID生成")
    println("=" * 50)

    val batchUuids = UUIDUtils.batchRandomUUID(5, UUIDUtils.UUIDFormat.SIMPLE)
    println("批量生成5个简单格式UUID:")
    batchUuids.forEachIndexed { index, uuid ->
        println("  ${index + 1}. $uuid")
    }

    // 4. 基于名称的UUID生成
    println("\n4. 基于名称的UUID生成")
    println("=" * 50)

    val nameBasedUuid1 = UUIDUtils.nameBasedUUID("my-application")
    val nameBasedUuid2 = UUIDUtils.nameBasedUUID("my-application")
    val nameBasedUuid3 = UUIDUtils.nameBasedUUID("other-application")

    println("基于名称'my-application'的UUID (第1次): $nameBasedUuid1")
    println("基于名称'my-application'的UUID (第2次): $nameBasedUuid2")
    println("基于名称'other-application'的UUID:     $nameBasedUuid3")
    println("相同名称生成相同UUID: ${nameBasedUuid1 == nameBasedUuid2}")

    // 使用命名空间的基于名称UUID
    val namespace = UUIDUtils.randomUUID()
    val nsUuid = UUIDUtils.nameBasedUUID(namespace, "my-application")
    println("使用命名空间的UUID: $nsUuid")

    // 5. UUID格式转换
    println("\n5. UUID格式转换")
    println("=" * 50)

    val originalUuid = UUIDUtils.randomUUID()
    println("原始UUID: $originalUuid")
    println("转为简单格式: ${UUIDUtils.toSimple(originalUuid)}")
    println("转为大写: ${UUIDUtils.toUpperCase(originalUuid)}")
    println("转为小写: ${UUIDUtils.toLowerCase(originalUuid.uppercase())}")

    val simpleTest = UUIDUtils.toSimple(originalUuid)
    println("简单格式转标准: ${UUIDUtils.toStandard(simpleTest)}")

    // 6. UUID验证
    println("\n6. UUID验证")
    println("=" * 50)

    val validUuids = listOf(
        UUIDUtils.randomUUID(),
        UUIDUtils.randomSimpleUUID(),
        "550e8400-e29b-41d4-a716-446655440000",
        "550e8400e29b41d4a716446655440000"
    )

    val invalidUuids = listOf(
        "invalid-uuid",
        "123456",
        "",
        "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
    )

    println("有效UUID验证:")
    validUuids.forEach { uuid ->
        println("  $uuid -> ${UUIDUtils.isValid(uuid)}")
    }

    println("\n无效UUID验证:")
    invalidUuids.forEach { uuid ->
        println("  '$uuid' -> ${UUIDUtils.isValid(uuid)}")
    }

    // 7. UUID信息提取
    println("\n7. UUID信息提取")
    println("=" * 50)

    val testUuid = UUIDUtils.randomUUID()
    println("测试UUID: $testUuid")

    val version = UUIDUtils.getVersion(testUuid)
    val variant = UUIDUtils.getVariant(testUuid)
    val info = UUIDUtils.getUUIDInfo(testUuid)

    println("版本: ${version?.name}")
    println("变体: $variant")

    if (info != null) {
        println("\n详细信息:")
        println(info.toReadableString())
    }

    // 8. UUID比较
    println("\n8. UUID比较")
    println("=" * 50)

    val uuid1 = UUIDUtils.randomUUID()
    val uuid2 = UUIDUtils.toSimple(uuid1)  // 同一个UUID的不同格式
    val uuid3 = UUIDUtils.toUpperCase(uuid1)  // 同一个UUID的大写格式
    val uuid4 = UUIDUtils.randomUUID()  // 不同的UUID

    println("UUID1 (标准): $uuid1")
    println("UUID2 (简单): $uuid2")
    println("UUID3 (大写): $uuid3")
    println("UUID4 (其他): $uuid4")

    println("\n比较结果:")
    println("UUID1 == UUID2: ${UUIDUtils.equals(uuid1, uuid2)}")
    println("UUID1 == UUID3: ${UUIDUtils.equals(uuid1, uuid3)}")
    println("UUID1 == UUID4: ${UUIDUtils.equals(uuid1, uuid4)}")

    // 9. 空UUID处理
    println("\n9. 空UUID处理")
    println("=" * 50)

    val nilUuid = UUIDUtils.nilUUID()
    val nilSimple = UUIDUtils.nilUUID(UUIDUtils.UUIDFormat.SIMPLE)

    println("标准格式空UUID: $nilUuid")
    println("简单格式空UUID: $nilSimple")
    println("是否为空UUID: ${UUIDUtils.isNil(nilUuid)}")
    println("普通UUID是否为空: ${UUIDUtils.isNil(UUIDUtils.randomUUID())}")

    // 10. 业务场景UUID生成
    println("\n10. 业务场景UUID生成")
    println("=" * 50)

    println("会话ID: ${UUIDUtils.generateSessionId()}")
    println("追踪ID: ${UUIDUtils.generateTraceId()}")
    println("请求ID: ${UUIDUtils.generateRequestId()}")
    println("文件ID: ${UUIDUtils.generateFileId()}")
    println("事务ID: ${UUIDUtils.generateTransactionId()}")
    println("关联ID: ${UUIDUtils.generateCorrelationId()}")

    // 11. 实际应用场景示例
    println("\n11. 实际应用场景示例")
    println("=" * 50)

    // 场景1: Web请求处理
    println("场景1: Web请求处理")
    val requestId = UUIDUtils.generateRequestId()
    val traceId = UUIDUtils.generateTraceId()
    println("请求ID: $requestId")
    println("追踪ID: $traceId")

    // 场景2: 文件上传
    println("\n场景2: 文件上传")
    val fileId = UUIDUtils.generateFileId()
    val uploadSessionId = UUIDUtils.generateSessionId()
    println("文件ID: $fileId")
    println("上传会话ID: $uploadSessionId")

    // 场景3: 数据库操作
    println("\n场景3: 数据库操作")
    val transactionId = UUIDUtils.generateTransactionId()
    val correlationId = UUIDUtils.generateCorrelationId()
    println("事务ID: $transactionId")
    println("关联ID: $correlationId")

    // 场景4: 缓存键生成
    println("\n场景4: 缓存键生成")
    val cacheKey = "user:profile:${UUIDUtils.randomSimpleUUID()}"
    println("缓存键: $cacheKey")

    // 场景5: 消息队列
    println("\n场景5: 消息队列")
    val messageId = UUIDUtils.randomUUID(UUIDUtils.UUIDFormat.SIMPLE)
    val consumerGroupId = UUIDUtils.nameBasedUUID("my-service-consumer")
    println("消息ID: $messageId")
    println("消费者组ID: $consumerGroupId")

    // 12. 性能测试示例
    println("\n12. 性能测试示例")
    println("=" * 50)

    val count = 10000
    println("生成${count}个UUID的性能测试:")

    val startTime = System.currentTimeMillis()
    repeat(count) {
        UUIDUtils.randomUUID()
    }
    val endTime = System.currentTimeMillis()
    val duration = endTime - startTime
    val qps = count * 1000.0 / duration

    println("耗时: ${duration}ms")
    println("QPS: ${String.format("%.0f", qps)}")

    // 13. UUID工具方法示例
    println("\n13. UUID工具方法示例")
    println("=" * 50)

    // 生成UUID数组
    val uuidArray = UUIDUtils.generateArray(3, UUIDUtils.UUIDFormat.SIMPLE)
    println("UUID数组:")
    uuidArray.forEachIndexed { index, uuid ->
        println("  [$index]: $uuid")
    }

    // 格式化现有UUID
    val existingUuid = "550e8400e29b41d4a716446655440000"
    val formattedUuids = UUIDUtils.UUIDFormat.values().map { format ->
        format.name to UUIDUtils.formatUUID(existingUuid, format)
    }

    println("\n同一UUID的不同格式:")
    formattedUuids.forEach { (format, uuid) ->
        println("  $format: $uuid")
    }

    println("\n=== UUIDUtil 使用示例完成 ===")
}

/**
 * 扩展函数：重复字符
 */
private operator fun String.times(n: Int): String = this.repeat(n)

/**
 * 业务服务示例类
 * 展示在实际业务代码中如何使用UUIDUtil
 */
class BusinessService {

    /**
     * 用户注册服务
     */
    fun registerUser(username: String, email: String): UserRegistrationResult {
        val userId = UUIDUtils.generateSessionId()  // 用户ID使用简单格式
        val sessionId = UUIDUtils.generateSessionId()
        val traceId = UUIDUtils.generateTraceId()

        println("用户注册 - 用户ID: $userId, 会话ID: $sessionId, 追踪ID: $traceId")

        return UserRegistrationResult(
            userId = userId,
            sessionId = sessionId,
            traceId = traceId,
            success = true
        )
    }

    /**
     * 文档上传服务
     */
    fun uploadDocument(fileName: String, content: ByteArray): DocumentUploadResult {
        val documentId = UUIDUtils.generateFileId()
        val uploadId = UUIDUtils.generateRequestId()
        val storageKey = "docs/${UUIDUtils.randomUUID(UUIDUtils.UUIDFormat.SIMPLE)}"

        println("文档上传 - 文档ID: $documentId, 上传ID: $uploadId, 存储键: $storageKey")

        return DocumentUploadResult(
            documentId = documentId,
            uploadId = uploadId,
            storageKey = storageKey,
            success = true
        )
    }

    /**
     * 订单处理服务
     */
    fun processOrder(customerId: String, items: List<String>): OrderProcessResult {
        val orderId = UUIDUtils.randomUUID(UUIDUtils.UUIDFormat.SIMPLE)
        val transactionId = UUIDUtils.generateTransactionId()
        val correlationId = UUIDUtils.generateCorrelationId()

        println("订单处理 - 订单ID: $orderId, 事务ID: $transactionId, 关联ID: $correlationId")

        return OrderProcessResult(
            orderId = orderId,
            transactionId = transactionId,
            correlationId = correlationId,
            success = true
        )
    }

    data class UserRegistrationResult(
        val userId: String,
        val sessionId: String,
        val traceId: String,
        val success: Boolean
    )

    data class DocumentUploadResult(
        val documentId: String,
        val uploadId: String,
        val storageKey: String,
        val success: Boolean
    )

    data class OrderProcessResult(
        val orderId: String,
        val transactionId: String,
        val correlationId: String,
        val success: Boolean
    )
}

package com.fanekit.app.router

import com.fanekit.common.core.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.web.bind.annotation.*

@RestController
@Order(1)
@RequestMapping("/test")
@Tag(name = "测试", description = "测试相关")
class TestController {

    @Value("\${app.version}")
    private val version: String? = null

    @GetMapping("/test")
    @Operation(summary = "测试接口", description = "测试接口")
    fun test(): ApiResponse {
        return ApiResponse.success("测试接口调用成功", mapOf(
            "timestamp" to System.currentTimeMillis(),
            "message" to "Hello from Kotlin!",
            "test" to "$version"
        ))
    }

    @GetMapping("/hello/{name}")
    fun hello(@PathVariable name: String): ApiResponse {
        return ApiResponse.success("Hello, $name!", mapOf(
            "name" to name,
            "timestamp" to System.currentTimeMillis()
        ))
    }

    @PostMapping("/echo")
    fun echo(@RequestBody body: Map<String, Any>): ApiResponse {
        return ApiResponse.success("Echo响应", mapOf(
            "echo" to body,
            "receivedAt" to System.currentTimeMillis()
        ))
    }
}
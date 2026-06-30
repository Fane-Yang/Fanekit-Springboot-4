package com.fanekit.common.log

import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference

@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestResponseLoggingFilter : WebFilter {

    private val log = LoggerFactory.getLogger(RequestResponseLoggingFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val startTime = System.currentTimeMillis()
        val startTimeObj = LocalDateTime.now()

        val requestBodyRef = AtomicReference<String>("")
        val responseBodyRef = AtomicReference<String>("")

        // 1. 记录请求
        val decoratedRequest: ServerHttpRequest = object : ServerHttpRequestDecorator(exchange.request) {
            override fun getBody(): Flux<DataBuffer> {
                return super.getBody().map { buffer: DataBuffer ->
                    val length = buffer.readableByteCount()
                    if (length > 0) {
                        val content = ByteArray(length)
                        buffer.read(content)
                        DataBufferUtils.release(buffer)
                        val body = String(content, StandardCharsets.UTF_8)
                        requestBodyRef.updateAndGet { it + body }
                        // 重新包装以便后续使用
                        exchange.response.bufferFactory().wrap(content)
                    } else {
                        buffer
                    }
                }
            }
        }

        // 2. 记录响应
        val decoratedResponse: ServerHttpResponse = object : ServerHttpResponseDecorator(exchange.getResponse()) {
            override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
                return super.writeWith(Flux.from(body).map { dataBuffer: DataBuffer ->
                    val length = dataBuffer.readableByteCount()
                    if (length > 0) {
                        val content = ByteArray(length)
                        dataBuffer.read(content)
                        DataBufferUtils.release(dataBuffer)
                        val responseBody = String(content, StandardCharsets.UTF_8)
                        responseBodyRef.updateAndGet { it + responseBody }
                        exchange.getResponse().bufferFactory().wrap(content)
                    } else {
                        dataBuffer
                    }
                })
            }
        }

        return chain.filter(
            exchange.mutate()
                .request(decoratedRequest)
                .response(decoratedResponse)
                .build()
        ).doFinally {
            val endTime = System.currentTimeMillis()
            val endTimeObj = LocalDateTime.now()
            val duration = endTime - startTime

            printLog(decoratedRequest, requestBodyRef.get(), responseBodyRef.get(), startTimeObj, endTimeObj, duration)
        }
    }

    private fun printLog(
        request: ServerHttpRequest,
        requestBody: String,
        responseBody: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        duration: Long
    ) {
        val sb = StringBuilder()
        sb.append("\n=== HTTP请求响应日志 ===\n")
        sb.append("【请求信息】\n")
        sb.append("请求方法: ").append(request.method).append("\n")
        sb.append("请求URI: ").append(request.uri.path).append("\n")

        val queryParams = request.queryParams
        sb.append("查询参数: ").append(if (queryParams.isEmpty()) "null" else queryParams).append("\n")
        sb.append("客户端IP: ").append(getClientIp(request)).append("\n")

        try {
            val headersMap = request.headers.toSingleValueMap()
            sb.append("请求头: ").append(formatHeaders(headersMap)).append("\n")
        } catch (_: Exception) {
            sb.append("请求头: ").append(request.headers).append("\n")
        }

        sb.append("请求参数: ").append(if (requestBody.isEmpty()) "{}" else requestBody).append("\n")
        sb.append("【响应信息】\n")
        sb.append("响应体: ").append(responseBody).append("\n")
        sb.append("请求开始时间: ").append(startTime).append("\n")
        sb.append("响应结束时间: ").append(endTime).append("\n")
        sb.append("请求总耗时: ").append(duration).append("ms\n")
        sb.append("=== 请求响应处理完成 ===")

        log.info(sb.toString())
    }

    private fun getClientIp(request: ServerHttpRequest): String {
        val headers = request.headers
        var ip = headers.getFirst("X-Forwarded-For")
        if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = headers.getFirst("Proxy-Client-IP")
        }
        if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            ip = headers.getFirst("WL-Proxy-Client-IP")
        }
        if (ip.isNullOrEmpty() || "unknown".equals(ip, ignoreCase = true)) {
            val remoteAddress = request.remoteAddress
            if (remoteAddress != null) {
                ip = remoteAddress.address.hostAddress
            }
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim()
        }
        return ip ?: "0:0:0:0:0:0:0:1"
    }

    private fun formatHeaders(headers: Map<String, String>): String {
        return headers.entries.joinToString(prefix = "{", postfix = "}", separator = ",") {
            val key = it.key
            val value = it.value.replace("\"", "\\\"")
            "\"$key\":\"$value\""
        }
    }
}
